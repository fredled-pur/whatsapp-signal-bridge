package com.bridge.whatsapptosignal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bridge.whatsapptosignal.BridgeApplication
import com.bridge.whatsapptosignal.data.entity.MessageLog
import com.bridge.whatsapptosignal.data.entity.MessageStatus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class BridgeUiState(
    val isSetupComplete: Boolean = false,
    val setupStep: Int = 1,
    
    // Bridge state
    val bridgeEnabled: Boolean = false,
    val listenerConnected: Boolean = false,
    
    // Signal state
    val signalRegistered: Boolean = false,
    val signalPhoneNumber: String = "",
    val destinationNumber: String = "",
    val isRegistering: Boolean = false,
    val registrationError: String? = null,
    
    // Stats
    val todayCount: Int = 0,
    val totalCount: Int = 0,
    val forwardedCount: Int = 0,
    
    // Recent logs
    val recentLogs: List<MessageLog> = emptyList(),
    
    // Settings
    val forwardMedia: Boolean = true,
    val showPreview: Boolean = true,
    val filterSpam: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: Int = 23,
    val quietHoursEnd: Int = 7,
    val filterMode: String = "none",
    val filteredContacts: Set<String> = emptySet()
)

class BridgeViewModel(private val app: BridgeApplication) : ViewModel() {
    
    private val _uiState = MutableStateFlow(BridgeUiState())
    val uiState: StateFlow<BridgeUiState> = _uiState.asStateFlow()
    
    init {
        loadState()
    }
    
    private fun loadState() {
        viewModelScope.launch {
            // Combine all preference flows
            combine(
                app.preferencesManager.bridgeEnabled,
                app.preferencesManager.listenerConnected,
                app.preferencesManager.signalRegistered,
                app.preferencesManager.signalPhoneNumber,
                app.preferencesManager.destinationNumber
            ) { bridgeEnabled, listenerConnected, signalRegistered, signalPhone, destNumber ->
                _uiState.update { current ->
                    current.copy(
                        bridgeEnabled = bridgeEnabled,
                        listenerConnected = listenerConnected,
                        signalRegistered = signalRegistered,
                        signalPhoneNumber = signalPhone ?: "",
                        destinationNumber = destNumber ?: "",
                        isSetupComplete = signalRegistered && listenerConnected
                    )
                }
            }.collect()
        }
        
        // Load settings
        viewModelScope.launch {
            combine(
                app.preferencesManager.forwardMedia,
                app.preferencesManager.showPreview,
                app.preferencesManager.filterSpam,
                app.preferencesManager.quietHoursEnabled,
                app.preferencesManager.filterMode
            ) { forwardMedia, showPreview, filterSpam, quietHours, filterMode ->
                _uiState.update { current ->
                    current.copy(
                        forwardMedia = forwardMedia,
                        showPreview = showPreview,
                        filterSpam = filterSpam,
                        quietHoursEnabled = quietHours,
                        filterMode = filterMode
                    )
                }
            }.collect()
        }
        
        // Load quiet hours times
        viewModelScope.launch {
            combine(
                app.preferencesManager.quietHoursStart,
                app.preferencesManager.quietHoursEnd
            ) { start, end ->
                _uiState.update { current ->
                    current.copy(quietHoursStart = start, quietHoursEnd = end)
                }
            }.collect()
        }
        
        // Load filtered contacts
        viewModelScope.launch {
            app.preferencesManager.filteredContacts.collect { contacts ->
                _uiState.update { current ->
                    current.copy(filteredContacts = contacts)
                }
            }
        }
        
        // Load stats
        viewModelScope.launch {
            combine(
                app.database.messageLogDao().getTodayCount(),
                app.database.messageLogDao().getTotalCount(),
                app.database.messageLogDao().getCountByStatus(MessageStatus.FORWARDED)
            ) { today, total, forwarded ->
                _uiState.update { current ->
                    current.copy(
                        todayCount = today,
                        totalCount = total,
                        forwardedCount = forwarded
                    )
                }
            }.collect()
        }
        
        // Load recent logs
        viewModelScope.launch {
            app.database.messageLogDao().getRecentLogs(20).collect { logs ->
                _uiState.update { current ->
                    current.copy(recentLogs = logs)
                }
            }
        }
    }
    
    // Setup actions
    fun setSetupStep(step: Int) {
        _uiState.update { it.copy(setupStep = step) }
    }
    
    fun notificationAccessGranted() {
        viewModelScope.launch {
            app.preferencesManager.setListenerConnected(true)
            _uiState.update { it.copy(listenerConnected = true, setupStep = 2) }
        }
    }
    
    // Signal registration
    fun requestVerificationCode(phoneNumber: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRegistering = true, registrationError = null) }
            
            val success = app.signalClient.requestVerificationCode(phoneNumber)
            
            if (success) {
                app.preferencesManager.setSignalPhoneNumber(phoneNumber)
                _uiState.update { it.copy(
                    isRegistering = false,
                    signalPhoneNumber = phoneNumber,
                    setupStep = 3
                )}
            } else {
                _uiState.update { it.copy(
                    isRegistering = false,
                    registrationError = "Kon verificatiecode niet verzenden"
                )}
            }
        }
    }
    
    fun verifyCode(code: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRegistering = true, registrationError = null) }
            
            val phoneNumber = _uiState.value.signalPhoneNumber
            val success = app.signalClient.register(phoneNumber, code)
            
            if (success) {
                app.preferencesManager.setSignalRegistered(true)
                _uiState.update { it.copy(
                    isRegistering = false,
                    signalRegistered = true,
                    setupStep = 4
                )}
            } else {
                _uiState.update { it.copy(
                    isRegistering = false,
                    registrationError = "Verificatie mislukt. Controleer de code."
                )}
            }
        }
    }
    
    fun setDestinationNumber(number: String) {
        viewModelScope.launch {
            app.preferencesManager.setDestinationNumber(number)
            _uiState.update { it.copy(destinationNumber = number) }
        }
    }
    
    fun completeSetup() {
        viewModelScope.launch {
            app.preferencesManager.setBridgeEnabled(true)
            _uiState.update { it.copy(isSetupComplete = true, bridgeEnabled = true) }
        }
    }
    
    // Bridge control
    fun setBridgeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            app.preferencesManager.setBridgeEnabled(enabled)
        }
    }
    
    // Settings
    fun setForwardMedia(enabled: Boolean) {
        viewModelScope.launch {
            app.preferencesManager.setForwardMedia(enabled)
        }
    }
    
    fun setShowPreview(enabled: Boolean) {
        viewModelScope.launch {
            app.preferencesManager.setShowPreview(enabled)
        }
    }
    
    fun setFilterSpam(enabled: Boolean) {
        viewModelScope.launch {
            app.preferencesManager.setFilterSpam(enabled)
        }
    }
    
    fun setQuietHoursEnabled(enabled: Boolean) {
        viewModelScope.launch {
            app.preferencesManager.setQuietHoursEnabled(enabled)
        }
    }
    
    fun setQuietHours(start: Int, end: Int) {
        viewModelScope.launch {
            app.preferencesManager.setQuietHoursStart(start)
            app.preferencesManager.setQuietHoursEnd(end)
        }
    }
    
    fun setFilterMode(mode: String) {
        viewModelScope.launch {
            app.preferencesManager.setFilterMode(mode)
        }
    }
    
    fun addFilteredContact(contact: String) {
        viewModelScope.launch {
            app.preferencesManager.addFilteredContact(contact)
        }
    }
    
    fun removeFilteredContact(contact: String) {
        viewModelScope.launch {
            app.preferencesManager.removeFilteredContact(contact)
        }
    }
    
    // Logs
    fun clearLogs() {
        viewModelScope.launch {
            app.database.messageLogDao().deleteAll()
        }
    }
}

class BridgeViewModelFactory(private val app: BridgeApplication) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BridgeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BridgeViewModel(app) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
