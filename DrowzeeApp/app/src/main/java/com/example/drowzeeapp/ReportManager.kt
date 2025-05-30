package com.example.drowzeeapp

import android.content.Context
import android.telephony.SmsManager
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Report manager for drowsiness detection
 * Handles logging events and sending reports to supervisor
 */
class ReportManager(
    private val context: Context,
    private val driverId: String
) {
    private var drowsyEventCount = 0
    private var yawnEventCount = 0
    private var sessionStartTime = System.currentTimeMillis()
    private val events = mutableListOf<DetectionEvent>()

    companion object {
        private const val TAG = "ReportManager"
    }

    /**
     * Log a detection event
     */
    fun logEvent(status: String, event: String) {
        val timestamp = System.currentTimeMillis()
        events.add(DetectionEvent(timestamp, status, event))

        when (status) {
            "Drowsy - Eyes closed" -> drowsyEventCount++
            "Yawning" -> yawnEventCount++
        }

        // Also save to log file
        saveToLogFile(timestamp, status, event)
    }

    /**
     * Save event to log file
     */
    private fun saveToLogFile(timestamp: Long, status: String, event: String) {
        try {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val logDir = File(context.filesDir, "logs/$driverId")
            logDir.mkdirs()

            val logFile = File(logDir, "${today}_log.csv")
            val isNewFile = !logFile.exists()

            if (isNewFile) {
                logFile.writeText("Timestamp,Status,Event\n")
            }

            val formattedTimestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(Date(timestamp))

            logFile.appendText("$formattedTimestamp,$status,$event\n")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to log file", e)
        }
    }

    /**
     * Generate and send a report to the supervisor
     * @param supervisorMobile The supervisor's mobile number
     * @param forceSend Whether to force sending the report even if there are no events
     * @return True if report was sent, false otherwise
     */
    fun generateAndSendReport(supervisorMobile: String, forceSend: Boolean = false): Boolean {
        if (events.isEmpty() && !forceSend) {
            Log.d(TAG, "No events to report")
            return false
        }

        val sessionDurationMinutes = (System.currentTimeMillis() - sessionStartTime) / 60000

        val reportBuilder = StringBuilder()
        reportBuilder.append("🚨 DRIVER REPORT: $driverId\n\n")
        reportBuilder.append("Session Duration: $sessionDurationMinutes minutes\n")
        reportBuilder.append("Drowsy Events: $drowsyEventCount\n")
        reportBuilder.append("Yawning Events: $yawnEventCount\n\n")

        // Add risk assessment
        val riskLevel = when {
            drowsyEventCount > 5 -> "HIGH RISK"
            drowsyEventCount > 2 -> "MODERATE RISK"
            drowsyEventCount > 0 -> "LOW RISK"
            else -> "NO RISK DETECTED"
        }
        reportBuilder.append("Risk Assessment: $riskLevel\n\n")

        // Add recommendation
        val recommendation = when {
            drowsyEventCount > 5 -> "Driver should take an immediate break of at least 30 minutes."
            drowsyEventCount > 2 -> "Driver should consider taking a short break soon."
            drowsyEventCount > 0 -> "Driver should remain vigilant and consider a break if feeling tired."
            else -> "No action needed at this time."
        }
        reportBuilder.append("Recommendation: $recommendation\n")

        // Add timestamp
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        reportBuilder.append("\nReport generated at $currentTime")

        return sendSmsReport(supervisorMobile, reportBuilder.toString())
    }

    /**
     * Send SMS report to supervisor
     */
    private fun sendSmsReport(phoneNumber: String, message: String): Boolean {
        // Check if we have SMS permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "SMS permission not granted")
            // Save report to file instead
            saveReportToFile(message)
            return false
        }

        return try {
            val smsManager = SmsManager.getDefault()

            // Split message if it's too long
            val messageParts = smsManager.divideMessage(message)
            smsManager.sendMultipartTextMessage(phoneNumber, null, messageParts, null, null)

            Log.d(TAG, "SMS report sent to $phoneNumber")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS report: ${e.message}", e)
            // Save report to file as fallback
            saveReportToFile(message)
            false
        }
    }

    /**
     * Save report to file as fallback when SMS fails
     */
    private fun saveReportToFile(message: String) {
        try {
            val today = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
            val reportsDir = File(context.filesDir, "reports/$driverId")
            reportsDir.mkdirs()

            val reportFile = File(reportsDir, "report_${today}.txt")
            reportFile.writeText(message)

            Log.d(TAG, "Report saved to file: ${reportFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving report to file", e)
        }
    }

    /**
     * Reset the session statistics
     */
    fun resetSession() {
        sessionStartTime = System.currentTimeMillis()
        drowsyEventCount = 0
        yawnEventCount = 0
        events.clear()
    }

    /**
     * Data class for detection events
     */
    data class DetectionEvent(
        val timestamp: Long,
        val status: String,
        val event: String
    )
}
