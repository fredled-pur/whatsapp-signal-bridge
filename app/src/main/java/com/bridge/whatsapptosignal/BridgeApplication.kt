package com.bridge.whatsapptosignal

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.bridge.whatsapptosignal.data.AppDatabase
import com.bridge.whatsapptosignal.data.PreferencesManager
import com.bridge.whatsapptosignal.signal.SignalClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class BridgeApplication : Application() {
    
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Lazy initialization of components
    val database by lazy { AppDatabase.getDatabase(this) }
    val preferencesManager by lazy { PreferencesManager(this) }
    val signalClient by lazy { SignalClient(this, database, applicationScope) }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            
            // Channel for forwarded messages
            val messagesChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "Doorgestuurde berichten",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaties voor doorgestuurde WhatsApp berichten"
            }
            
            // Channel for service status
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE,
                "Bridge Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Toont dat de bridge actief is"
            }
            
            notificationManager.createNotificationChannel(messagesChannel)
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }
    
    companion object {
        const val CHANNEL_MESSAGES = "bridge_messages"
        const val CHANNEL_SERVICE = "bridge_service"
        
        lateinit var instance: BridgeApplication
            private set
    }
}
