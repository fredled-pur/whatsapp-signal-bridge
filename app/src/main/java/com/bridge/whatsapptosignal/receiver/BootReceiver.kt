package com.bridge.whatsapptosignal.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.bridge.whatsapptosignal.BridgeApplication
import com.bridge.whatsapptosignal.service.BridgeForegroundService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Receiver that starts the bridge service when device boots
 */
class BootReceiver : BroadcastReceiver() {
    
    private val TAG = "BootReceiver"
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            Log.d(TAG, "Boot completed, checking if bridge should start")
            
            val app = context.applicationContext as BridgeApplication
            
            // Check if bridge was enabled
            val bridgeEnabled = runBlocking {
                app.preferencesManager.bridgeEnabled.first()
            }
            
            if (bridgeEnabled) {
                Log.d(TAG, "Starting bridge service")
                startBridgeService(context)
            } else {
                Log.d(TAG, "Bridge is disabled, not starting service")
            }
        }
    }
    
    private fun startBridgeService(context: Context) {
        val serviceIntent = Intent(context, BridgeForegroundService::class.java)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
