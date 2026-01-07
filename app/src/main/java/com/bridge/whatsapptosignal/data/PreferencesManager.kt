package com.bridge.whatsapptosignal.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bridge_settings")

class PreferencesManager(private val context: Context) {
    
    companion object {
        // Bridge settings
        val BRIDGE_ENABLED = booleanPreferencesKey("bridge_enabled")
        val LISTENER_CONNECTED = booleanPreferencesKey("listener_connected")
        
        // Signal settings
        val SIGNAL_REGISTERED = booleanPreferencesKey("signal_registered")
        val SIGNAL_PHONE_NUMBER = stringPreferencesKey("signal_phone_number")
        val DESTINATION_NUMBER = stringPreferencesKey("destination_number")
        
        // Forwarding settings
        val FORWARD_MEDIA = booleanPreferencesKey("forward_media")
        val SHOW_PREVIEW = booleanPreferencesKey("show_preview")
        val FILTER_SPAM = booleanPreferencesKey("filter_spam")
        
        // Quiet hours
        val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        val QUIET_HOURS_START = intPreferencesKey("quiet_hours_start")
        val QUIET_HOURS_END = intPreferencesKey("quiet_hours_end")
        
        // Contact filtering
        val FILTER_MODE = stringPreferencesKey("filter_mode") // "none", "allowlist", "blocklist"
        val FILTERED_CONTACTS = stringSetPreferencesKey("filtered_contacts")
    }
    
    // Bridge enabled state
    val bridgeEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[BRIDGE_ENABLED] ?: false
    }
    
    suspend fun setBridgeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BRIDGE_ENABLED] = enabled
        }
    }
    
    // Listener connection state
    val listenerConnected: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[LISTENER_CONNECTED] ?: false
    }
    
    suspend fun setListenerConnected(connected: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[LISTENER_CONNECTED] = connected
        }
    }
    
    // Signal registration state
    val signalRegistered: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SIGNAL_REGISTERED] ?: false
    }
    
    suspend fun setSignalRegistered(registered: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SIGNAL_REGISTERED] = registered
        }
    }
    
    // Signal phone number (the bridge number)
    val signalPhoneNumber: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[SIGNAL_PHONE_NUMBER]
    }
    
    suspend fun setSignalPhoneNumber(number: String) {
        context.dataStore.edit { preferences ->
            preferences[SIGNAL_PHONE_NUMBER] = number
        }
    }
    
    // Destination number (where messages are forwarded to)
    val destinationNumber: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[DESTINATION_NUMBER]
    }
    
    suspend fun setDestinationNumber(number: String) {
        context.dataStore.edit { preferences ->
            preferences[DESTINATION_NUMBER] = number
        }
    }
    
    // Forward media setting
    val forwardMedia: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[FORWARD_MEDIA] ?: true
    }
    
    suspend fun setForwardMedia(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[FORWARD_MEDIA] = enabled
        }
    }
    
    // Show preview setting
    val showPreview: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_PREVIEW] ?: true
    }
    
    suspend fun setShowPreview(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_PREVIEW] = enabled
        }
    }
    
    // Filter spam setting
    val filterSpam: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[FILTER_SPAM] ?: true
    }
    
    suspend fun setFilterSpam(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[FILTER_SPAM] = enabled
        }
    }
    
    // Quiet hours enabled
    val quietHoursEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[QUIET_HOURS_ENABLED] ?: false
    }
    
    suspend fun setQuietHoursEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[QUIET_HOURS_ENABLED] = enabled
        }
    }
    
    // Quiet hours start (hour of day, 0-23)
    val quietHoursStart: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[QUIET_HOURS_START] ?: 23
    }
    
    suspend fun setQuietHoursStart(hour: Int) {
        context.dataStore.edit { preferences ->
            preferences[QUIET_HOURS_START] = hour
        }
    }
    
    // Quiet hours end (hour of day, 0-23)
    val quietHoursEnd: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[QUIET_HOURS_END] ?: 7
    }
    
    suspend fun setQuietHoursEnd(hour: Int) {
        context.dataStore.edit { preferences ->
            preferences[QUIET_HOURS_END] = hour
        }
    }
    
    // Filter mode
    val filterMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[FILTER_MODE] ?: "none"
    }
    
    suspend fun setFilterMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[FILTER_MODE] = mode
        }
    }
    
    // Filtered contacts
    val filteredContacts: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[FILTERED_CONTACTS] ?: emptySet()
    }
    
    suspend fun setFilteredContacts(contacts: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[FILTERED_CONTACTS] = contacts
        }
    }
    
    suspend fun addFilteredContact(contact: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[FILTERED_CONTACTS] ?: emptySet()
            preferences[FILTERED_CONTACTS] = current + contact
        }
    }
    
    suspend fun removeFilteredContact(contact: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[FILTERED_CONTACTS] ?: emptySet()
            preferences[FILTERED_CONTACTS] = current - contact
        }
    }
}
