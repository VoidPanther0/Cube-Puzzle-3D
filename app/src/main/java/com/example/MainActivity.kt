package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.audio.SoundAndHapticsManager
import com.example.data.ai.AiStrategyRepository
import com.example.data.model.GameMode
import com.example.data.repository.GameRepository
import com.example.firebase.configureAppCheck
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.GameScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CubePuzzleTheme
import com.example.ui.viewmodel.GameViewModel
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private const val ROUTE_HOME = "home"
private const val ROUTE_GAME = "game/{mode}"
private const val ROUTE_LEADERBOARD = "leaderboard"
private const val ROUTE_SETTINGS = "settings"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            CubePuzzleTheme {
                AppNavigation()
            }
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = { })
    } else {
        key(user.uid) {
            AuthenticatedAppRoot(
                currentUserId = user.uid,
                displayName = user.displayName?.takeIf { it.isNotBlank() } ?: "Player"
            )
        }
    }
}

@Composable
private fun AuthenticatedAppRoot(
    currentUserId: String,
    displayName: String
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val soundAndHaptics = remember(context) { SoundAndHapticsManager(context.applicationContext) }

    val mainViewModel: MainViewModel = viewModel(
        key = "main_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                MainViewModel(
                    repository = GameRepository(db, Firebase.auth),
                    soundAndHaptics = soundAndHaptics,
                    currentUserId = currentUserId,
                    initialDisplayName = displayName
                )
            }
        }
    )

    val gameViewModel: GameViewModel = viewModel(
        key = "game_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                GameViewModel(
                    repository = GameRepository(db, Firebase.auth),
                    aiRepository = AiStrategyRepository(),
                    soundAndHaptics = soundAndHaptics,
                    playerDisplayName = displayName
                )
            }
        }
    )

    val profileState by mainViewModel.userProfileState.collectAsStateWithLifecycle()
    val leaderboardState by mainViewModel.leaderboardState.collectAsStateWithLifecycle()
    val gameUiState by gameViewModel.uiState.collectAsStateWithLifecycle()

    val currentProfile = (profileState as? UiState.Success)?.data
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = ROUTE_HOME
    ) {
        composable(ROUTE_HOME) {
            HomeScreen(
                profile = currentProfile,
                displayName = displayName,
                onPlayClassic = {
                    gameViewModel.initializeGame(GameMode.CLASSIC, currentProfile, forceRestart = false)
                    navController.navigate("game/CLASSIC")
                },
                onPlayDailyChallenge = {
                    gameViewModel.initializeGame(GameMode.DAILY, currentProfile, forceRestart = true)
                    navController.navigate("game/DAILY")
                },
                onOpenLeaderboard = {
                    navController.navigate(ROUTE_LEADERBOARD)
                },
                onOpenSettings = {
                    navController.navigate(ROUTE_SETTINGS)
                },
                onQuitApp = {
                    activity?.finishAndRemoveTask() ?: activity?.finish()
                }
            )
        }

        composable(
            route = ROUTE_GAME,
            arguments = listOf(navArgument("mode") { type = NavType.StringType })
        ) { backStackEntry ->
            val modeArg = backStackEntry.arguments?.getString("mode") ?: "CLASSIC"
            val mode = if (modeArg == "DAILY") GameMode.DAILY else GameMode.CLASSIC
            GameScreen(
                uiState = gameUiState,
                onBack = { navController.popBackStack() },
                onRestart = {
                    gameViewModel.initializeGame(mode, currentProfile, forceRestart = true)
                },
                onPiecePickup = { gameViewModel.onPiecePickup() },
                onAttemptPlace = { slot, row, col ->
                    gameViewModel.attemptPlacePiece(slot, row, col)
                },
                onRequestAiHint = { gameViewModel.requestAiHint() },
                onDismissAiHint = { gameViewModel.dismissAiHint() },
                onDismissDailyBanner = { gameViewModel.dismissDailyBanner() }
            )
        }

        composable(ROUTE_LEADERBOARD) {
            LeaderboardScreen(
                currentUserId = currentUserId,
                profile = currentProfile,
                leaderboardState = leaderboardState,
                onBack = { navController.popBackStack() }
            )
        }

        composable(ROUTE_SETTINGS) {
            SettingsScreen(
                profile = currentProfile,
                displayName = displayName,
                onUpdateSettings = { sound, haptics, preview ->
                    mainViewModel.updateSettings(sound, haptics, preview)
                },
                onBack = { navController.popBackStack() },
                onSignedOut = {
                    navController.popBackStack(ROUTE_HOME, inclusive = false)
                }
            )
        }
    }
}
