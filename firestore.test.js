const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTimestamp() {
  return new Date(Date.now() - 2000);
}

function validProfilePayload(uid, overrides = {}) {
  const ts = pastTimestamp();
  return {
    userId: uid,
    displayName: "Player One",
    bestScore: 1200,
    highestCombo: 4,
    gamesPlayed: 3,
    totalLinesCleared: 18,
    dailyStreak: 2,
    dailyWins: 2,
    lastDailyDate: "2026-11-15",
    soundEnabled: true,
    hapticsEnabled: true,
    showLinePreview: true,
    savedClassicBoard: Array(64).fill(0),
    savedClassicScore: 340,
    savedClassicCombo: 1,
    savedClassicTray: ["DOT_1:1", "H_3:2", "SQ_2:4"],
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
  };
}

function validLeaderboardPayload(uid, overrides = {}) {
  return {
    userId: uid,
    displayName: "Player One",
    bestScore: 1200,
    highestCombo: 4,
    dailyWins: 2,
    isPublic: true,
    updatedAt: pastTimestamp(),
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write user profiles or leaderboard", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID)));
  await assertFails(unauthDb.collection("leaderboard").where("isPublic", "==", true).get());
  await assertFails(unauthDb.collection("leaderboard").doc(ALICE_UID).set(validLeaderboardPayload(ALICE_UID)));
});

test("Authenticated owner can create and update their own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(docRef.set(validProfilePayload(ALICE_UID)));
  await assertSucceeds(docRef.get());
  await assertSucceeds(
    docRef.update({
      bestScore: 2500,
      savedClassicScore: 500,
      updatedAt: pastTimestamp(),
    })
  );
  await assertSucceeds(
    docRef.update({
      soundEnabled: false,
      updatedAt: pastTimestamp(),
    })
  );
});

test("Authenticated user cannot read, write, or spoof another user's profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID)));

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ bestScore: 999999, updatedAt: pastTimestamp() }));
  await assertFails(bobDb.collection("users").doc(BOB_UID).set(validProfilePayload(ALICE_UID)));
});

test("Shadow update with ghost field and value poisoning are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection("users").doc(ALICE_UID);

  // Ghost field on create
  await assertFails(docRef.set(validProfilePayload(ALICE_UID, { isAdmin: true })));
  // Negative score poisoning
  await assertFails(docRef.set(validProfilePayload(ALICE_UID, { bestScore: -10 })));
  // Oversized board array
  await assertFails(docRef.set(validProfilePayload(ALICE_UID, { savedClassicBoard: Array(65).fill(0) })));
});

test("Leaderboard allows authenticated query with bestScore filter and owner-only writes", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("leaderboard").doc(ALICE_UID).set(validLeaderboardPayload(ALICE_UID)));
  await assertSucceeds(bobDb.collection("leaderboard").doc(BOB_UID).set(validLeaderboardPayload(BOB_UID, { bestScore: 1800 })));

  // Aligned list query succeeds
  await assertSucceeds(
    aliceDb.collection("leaderboard").where("isPublic", "==", true).orderBy("bestScore", "desc").limit(50).get()
  );

  // Cross-user leaderboard overwrite fails
  await assertFails(
    bobDb.collection("leaderboard").doc(ALICE_UID).update({ bestScore: 0, updatedAt: pastTimestamp() })
  );

  // Ghost field on leaderboard fails
  await assertFails(
    aliceDb.collection("leaderboard").doc(ALICE_UID).set(validLeaderboardPayload(ALICE_UID, { verified: true }))
  );
});
