package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class SoundAndHapticsManager(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    @Volatile
    var soundEnabled: Boolean = true

    @Volatile
    var hapticsEnabled: Boolean = true

    private val pickupPcm: ShortArray by lazy {
        synthesizeTone(freqHz = 660.0, durationMs = 45, decayRate = 18.0, volume = 0.25)
    }

    private val placePcm: ShortArray by lazy {
        synthesizeChord(freqs = doubleArrayOf(330.0, 495.0), durationMs = 80, decayRate = 12.0, volume = 0.35)
    }

    private val invalidPcm: ShortArray by lazy {
        synthesizeTone(freqHz = 155.0, durationMs = 110, decayRate = 10.0, volume = 0.30)
    }

    private val gameOverPcm: ShortArray by lazy {
        synthesizeArpeggio(freqs = doubleArrayOf(440.0, 349.23, 293.66, 220.0), noteMs = 110, volume = 0.32)
    }

    private val dailyWinPcm: ShortArray by lazy {
        synthesizeArpeggio(freqs = doubleArrayOf(523.25, 659.25, 783.99, 1046.5), noteMs = 95, volume = 0.36)
    }

    fun onPiecePickedUp() {
        if (soundEnabled) playPcm(pickupPcm)
        vibratePulse(durationMs = 14L, amplitude = 70)
    }

    fun onPiecePlaced(linesCleared: Int, combo: Int) {
        if (soundEnabled) {
            if (linesCleared <= 0) {
                playPcm(placePcm)
            } else {
                val semitoneShift = ((combo - 1).coerceIn(0, 7))
                val multiplier = Math.pow(2.0, semitoneShift / 12.0)
                val baseFreqs = if (linesCleared >= 2) {
                    doubleArrayOf(523.25 * multiplier, 659.25 * multiplier, 783.99 * multiplier, 1046.5 * multiplier)
                } else {
                    doubleArrayOf(523.25 * multiplier, 659.25 * multiplier, 783.99 * multiplier)
                }
                scope.launch {
                    val pcm = synthesizeArpeggio(freqs = baseFreqs, noteMs = 68, volume = 0.38)
                    playPcmInternal(pcm)
                }
            }
        }
        if (linesCleared > 0) {
            vibratePulse(durationMs = (30L + linesCleared * 12L).coerceAtMost(75L), amplitude = 180)
        } else {
            vibratePulse(durationMs = 20L, amplitude = 110)
        }
    }

    fun onInvalidPlacement() {
        if (soundEnabled) playPcm(invalidPcm)
        vibratePulse(durationMs = 35L, amplitude = 90)
    }

    fun onGameOver() {
        if (soundEnabled) playPcm(gameOverPcm)
        vibratePulse(durationMs = 80L, amplitude = 160)
    }

    fun onDailyWin() {
        if (soundEnabled) playPcm(dailyWinPcm)
        vibratePulse(durationMs = 90L, amplitude = 210)
    }

    private fun vibratePulse(durationMs: Long, amplitude: Int) {
        if (!hapticsEnabled) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255)))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(durationMs)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Haptic feedback unavailable: ${e.message}")
        }
    }

    private fun playPcm(samples: ShortArray) {
        scope.launch {
            playPcmInternal(samples)
        }
    }

    private fun playPcmInternal(samples: ShortArray) {
        var track: AudioTrack? = null
        try {
            val bufferBytes = samples.size * 2
            track = AudioTrack(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
                bufferBytes,
                AudioTrack.MODE_STATIC,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )
            track.write(samples, 0, samples.size)
            track.play()
            val durationMs = (samples.size * 1000L) / SAMPLE_RATE
            Thread.sleep(durationMs + 40L)
        } catch (e: Exception) {
            Log.w(TAG, "Audio playback skipped: ${e.message}")
        } finally {
            try {
                track?.stop()
                track?.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun synthesizeTone(
        freqHz: Double,
        durationMs: Int,
        decayRate: Double,
        volume: Double
    ): ShortArray {
        val count = (SAMPLE_RATE * durationMs) / 1000
        return ShortArray(count) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val env = exp(-decayRate * t) * (1.0 - exp(-220.0 * t))
            val wave = sin(2.0 * PI * freqHz * t) + 0.25 * sin(4.0 * PI * freqHz * t)
            (wave * env * volume * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    private fun synthesizeChord(
        freqs: DoubleArray,
        durationMs: Int,
        decayRate: Double,
        volume: Double
    ): ShortArray {
        val count = (SAMPLE_RATE * durationMs) / 1000
        return ShortArray(count) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val env = exp(-decayRate * t) * (1.0 - exp(-240.0 * t))
            var sum = 0.0
            for (f in freqs) {
                sum += sin(2.0 * PI * f * t)
            }
            val normalized = sum / freqs.size
            (normalized * env * volume * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    private fun synthesizeArpeggio(
        freqs: DoubleArray,
        noteMs: Int,
        volume: Double
    ): ShortArray {
        val noteSamples = (SAMPLE_RATE * noteMs) / 1000
        val totalSamples = noteSamples * freqs.size
        val out = ShortArray(totalSamples)
        freqs.forEachIndexed { idx, freq ->
            val offset = idx * noteSamples
            for (i in 0 until noteSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val env = exp(-10.0 * t) * (1.0 - exp(-240.0 * t))
                val wave = sin(2.0 * PI * freq * t) + 0.3 * sin(4.0 * PI * freq * t)
                out[offset + i] = (wave * env * volume * Short.MAX_VALUE)
                    .toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    .toShort()
            }
        }
        return out
    }

    private companion object {
        const val TAG = "SoundAndHaptics"
        const val SAMPLE_RATE = 22050
    }
}
