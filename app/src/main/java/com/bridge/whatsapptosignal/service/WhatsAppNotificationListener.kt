package com.bridge.whatsapptosignal.service

import android.app.Notification
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.bridge.whatsapptosignal.BridgeApplication
import com.bridge.whatsapptosignal.data.entity.MessageLog
import com.bridge.whatsapptosignal.data.entity.MessageStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date

class WhatsAppNotificationListener : NotificationListenerService() {
    
    private val TAG = "WANotificationListener"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val app by lazy { application as BridgeApplication }
    
    // WhatsApp package names
    private val whatsAppPackages = setOf(
        "com.whatsapp",
        "com.whatsapp.w4b"  // WhatsApp Business
    )
    
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Check if it's a WhatsApp notification
        if (sbn.packageName !in whatsAppPackages) return
        
        // Parse the notification
        val notification = sbn.notification
        val extras = notification.extras
        
        val title = extras.getString(Notification.EXTRA_TITLE) ?: return
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return
        
        // Skip group summary notifications
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        
        // Skip empty or system notifications
        if (text.isEmpty() || title.contains("WhatsApp")) return
        
        Log.d(TAG, "WhatsApp message from: $title")
        
        // Process the message
        serviceScope.launch {
            processWhatsAppMessage(title, text, sbn.postTime)
        }
    }
    
    private suspend fun processWhatsAppMessage(sender: String, text: String, timestamp: Long) {
        try {
            // Check if bridge is enabled
            val isEnabled = app.preferencesManager.bridgeEnabled.first()
            if (!isEnabled) {
                Log.d(TAG, "Bridge is disabled, skipping message")
                return
            }
            
            // Check quiet hours
            if (isInQuietHours()) {
                Log.d(TAG, "In quiet hours, skipping message")
                return
            }
            
            // Check if sender is filtered
            if (shouldFilterSender(sender)) {
                logMessage(sender, text, timestamp, MessageStatus.FILTERED)
                return
            }
            
            // Check spam filter
            if (isSpamMessage(text)) {
                logMessage(sender, text, timestamp, MessageStatus.FILTERED)
                return
            }
            
            // Forward via Signal
            val success = forwardViaSignal(sender, text)
            
            // Log the message
            logMessage(
                sender, 
                text, 
                timestamp, 
                if (success) MessageStatus.FORWARDED else MessageStatus.FAILED
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing message", e)
            logMessage(sender, text, timestamp, MessageStatus.FAILED)
        }
    }
    
    private suspend fun forwardViaSignal(sender: String, text: String): Boolean {
        return try {
            val destinationNumber = app.preferencesManager.destinationNumber.first()
            if (destinationNumber.isNullOrEmpty()) {
                Log.w(TAG, "No destination number configured")
                return false
            }
            
            // Format the message
            val formattedMessage = """
                📱 $sender
                
                $text
            """.trimIndent()
            
            // Send via Signal
            app.signalClient.sendMessage(destinationNumber, formattedMessage)
            
            Log.d(TAG, "Message forwarded successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to forward message via Signal", e)
            false
        }
    }
    
    private suspend fun isInQuietHours(): Boolean {
        val quietHoursEnabled = app.preferencesManager.quietHoursEnabled.first()
        if (!quietHoursEnabled) return false
        
        val startHour = app.preferencesManager.quietHoursStart.first()
        val endHour = app.preferencesManager.quietHoursEnd.first()
        
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        
        return if (startHour <= endHour) {
            currentHour in startHour..endHour
        } else {
            currentHour >= startHour || currentHour <= endHour
        }
    }
    
    private suspend fun shouldFilterSender(sender: String): Boolean {
        val filterMode = app.preferencesManager.filterMode.first()
        val filteredContacts = app.preferencesManager.filteredContacts.first()
        
        return when (filterMode) {
            "allowlist" -> sender !in filteredContacts
            "blocklist" -> sender in filteredContacts
            else -> false
        }
    }
    
    private fun isSpamMessage(text: String): Boolean {
        // Common spam patterns
        val spamPatterns = listOf(
            Regex("\\b\\d{4,6}\\b.*(?:code|verify|verificatie)", RegexOption.IGNORE_CASE),
            Regex("(?:OTP|PIN|code).*\\b\\d{4,6}\\b", RegexOption.IGNORE_CASE),
            Regex("Your.*verification.*code", RegexOption.IGNORE_CASE),
            Regex("Je.*verificatiecode", RegexOption.IGNORE_CASE)
        )
        
        return spamPatterns.any { it.containsMatchIn(text) }
    }
    
    private suspend fun logMessage(
        sender: String, 
        text: String, 
        timestamp: Long,
        status: MessageStatus
    ) {
        val messageLog = MessageLog(
            sender = sender,
            textPreview = text.take(100),
            timestamp = Date(timestamp),
            status = status
        )
        app.database.messageLogDao().insert(messageLog)
    }
    
    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Not needed for our use case
    }
    
    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected")
        
        // Notify that we're connected
        serviceScope.launch {
            app.preferencesManager.setListenerConnected(true)
        }
    }
    
    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "Notification listener disconnected")
        
        serviceScope.launch {
            app.preferencesManager.setListenerConnected(false)
        }
    }
}
