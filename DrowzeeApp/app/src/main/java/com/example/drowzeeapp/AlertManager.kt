package com.example.drowzeeapp

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.TimeUnit

/**
 * Sound alert manager for drowsiness detection
 * Adapted from the Python camera.py script's alert logic
 */
class AlertManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var lastAlertTime: Long = 0
    private val alertCooldown = 30 // 30 seconds cooldown between alerts, matching camera.py

    // Counters for continuous detection
    private var eyeCounter = 0
    private var yawnCounter = 0

    // Thresholds for alert triggering, matching camera.py
    private val EAR_CONSEC_FRAMES = 15 // Number of continuous drowsy frames to trigger alert
    private val MAR_CONSEC_FRAMES = 10 // Number of continuous yawn frames to trigger alert

    // Awake tracking for stopping alert
    private var awakeTimer: Long = 0
    private val awakeThreshold = 5 // 5 seconds of being awake to stop alert, matching camera.py

    private val handler = Handler(Looper.getMainLooper())

    // Flag to track if alert is currently playing
    private var isAlertPlaying = false

    companion object {
        private const val TAG = "AlertManager"
    }

    /**
     * Process detection result and manage alert state
     * @param isDrowsy Whether the driver is currently drowsy
     * @param isYawning Whether the driver is currently yawning
     * @return True if alert state changed, false otherwise
     */
    fun processDetectionResult(isDrowsy: Boolean, isYawning: Boolean): Boolean {
        Log.d(TAG, "Processing: isDrowsy=$isDrowsy, isYawning=$isYawning, eyeCounter=$eyeCounter, yawnCounter=$yawnCounter, isAlertPlaying=$isAlertPlaying")

        // Update counters based on detection result, exactly matching camera.py logic
        if (isDrowsy) {
            eyeCounter++
        } else {
            eyeCounter = 0
        }

        if (isYawning) {
            yawnCounter++
        } else {
            yawnCounter = 0
        }

        // Track awake state for stopping alert, matching camera.py logic
        val now = System.currentTimeMillis() / 1000 // Convert to seconds to match camera.py
        if (!isDrowsy && !isYawning) {
            if (awakeTimer == 0L) {
                awakeTimer = now
            } else if (now - awakeTimer >= awakeThreshold) {
                if (isAlertPlaying) {
                    Log.d(TAG, "Driver awake for $awakeThreshold consecutive seconds, stopping alert")
                    stopAlertSound()
                    return true
                }
            }
        } else {
            awakeTimer = 0
        }

        // Check if we should trigger an alert, matching camera.py logic
        var stateChanged = false

        if (eyeCounter >= EAR_CONSEC_FRAMES) {
            stateChanged = triggerAlert("Drowsy - Eyes closed")
        } else if (yawnCounter >= MAR_CONSEC_FRAMES) {
            stateChanged = triggerAlert("Yawning")
        }

        return stateChanged
    }

    /**
     * Trigger an alert with the specified reason
     * @param reason The reason for the alert (e.g., "Drowsy - Eyes closed", "Yawning")
     * @return True if alert was triggered, false otherwise
     */
    private fun triggerAlert(reason: String): Boolean {
        val currentTime = System.currentTimeMillis() / 1000 // Convert to seconds to match camera.py

        // Check if we're still in cooldown period, matching camera.py logic
        if (currentTime - lastAlertTime <= alertCooldown) {
            return false
        }

        Log.d(TAG, "[ALERT] $reason")
        playAlertSound()
        lastAlertTime = currentTime
        isAlertPlaying = true

        return true
    }

    /**
     * Play the alert sound
     */
    private fun playAlertSound() {
        try {
            // Release any existing media player
            stopAlertSound()

            // Create and prepare a new media player
            mediaPlayer = MediaPlayer.create(context, R.raw.alert)
            if (mediaPlayer == null) {
                Log.e(TAG, "Failed to create MediaPlayer for alert sound")
                return
            }

            mediaPlayer?.setOnErrorListener { mp, what, extra ->
                Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                false
            }

            mediaPlayer?.setOnCompletionListener {
                // If looping is not working, restart manually
                if (isAlertPlaying) {
                    it.start()
                }
            }

            mediaPlayer?.isLooping = true
            mediaPlayer?.start()

            Log.d(TAG, "Alert sound started playing")
        } catch (e: Exception) {
            Log.e(TAG, "Error playing alert sound", e)
        }
    }

    /**
     * Stop the alert sound
     */
    fun stopAlertSound() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
            isAlertPlaying = false
            Log.d(TAG, "Alert sound stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping alert sound", e)
        }
    }

    /**
     * Reset all counters
     */
    fun resetCounters() {
        eyeCounter = 0
        yawnCounter = 0
        awakeTimer = 0
        isAlertPlaying = false
        Log.d(TAG, "All counters reset")
    }

    /**
     * Release resources when no longer needed
     */
    fun release() {
        stopAlertSound()
        handler.removeCallbacksAndMessages(null)
        Log.d(TAG, "Resources released")
    }
}
