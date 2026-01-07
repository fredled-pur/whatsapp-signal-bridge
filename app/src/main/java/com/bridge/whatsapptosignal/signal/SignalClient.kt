package com.bridge.whatsapptosignal.signal

import android.content.Context
import android.util.Log
import com.bridge.whatsapptosignal.data.AppDatabase
import com.bridge.whatsapptosignal.data.entity.SignalIdentity
import com.bridge.whatsapptosignal.data.entity.SignalPreKey
import com.bridge.whatsapptosignal.data.entity.SignalSession
import com.bridge.whatsapptosignal.data.entity.SignalSignedPreKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.signal.libsignal.protocol.*
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.ecc.ECKeyPair
import org.signal.libsignal.protocol.state.*
import org.signal.libsignal.protocol.util.KeyHelper
import java.util.*

/**
 * SignalClient handles all Signal protocol operations including:
 * - Key generation and management
 * - Registration with Signal servers
 * - Sending encrypted messages
 * - Session management
 */
class SignalClient(
    private val context: Context,
    private val database: AppDatabase,
    private val scope: CoroutineScope
) {
    private val TAG = "SignalClient"
    
    // Protocol stores
    private lateinit var signalProtocolStore: SignalProtocolStore
    private lateinit var sessionStore: SessionStore
    private lateinit var preKeyStore: PreKeyStore
    private lateinit var signedPreKeyStore: SignedPreKeyStore
    private lateinit var identityKeyStore: IdentityKeyStore
    
    // Registration state
    private var isInitialized = false
    private var registrationId: Int = 0
    private var identityKeyPair: IdentityKeyPair? = null
    
    // Signal API client
    private val signalApi = SignalApiClient()
    
    /**
     * Initialize the Signal client with stored or new keys
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Initialize stores
            initializeStores()
            
            // Check if we have existing identity
            val existingIdentity = database.signalIdentityDao().getLocalIdentity()
            
            if (existingIdentity != null) {
                // Load existing identity
                registrationId = existingIdentity.registrationId
                identityKeyPair = IdentityKeyPair(existingIdentity.identityKeyPair)
                Log.d(TAG, "Loaded existing identity, registrationId: $registrationId")
            } else {
                // Generate new identity
                generateIdentity()
                Log.d(TAG, "Generated new identity, registrationId: $registrationId")
            }
            
            isInitialized = true
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Signal client", e)
            false
        }
    }
    
    /**
     * Register this device with Signal servers
     */
    suspend fun register(phoneNumber: String, verificationCode: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!isInitialized) {
                initialize()
            }
            
            val identityKey = identityKeyPair ?: throw IllegalStateException("Identity not initialized")
            
            // Generate pre-keys
            val preKeys = generatePreKeys(0, 100)
            val signedPreKey = generateSignedPreKey(identityKey, 1)
            
            // Register with Signal servers
            val success = signalApi.register(
                phoneNumber = phoneNumber,
                verificationCode = verificationCode,
                registrationId = registrationId,
                identityKey = identityKey.publicKey,
                signedPreKey = signedPreKey,
                preKeys = preKeys
            )
            
            if (success) {
                // Store pre-keys
                preKeys.forEach { preKey ->
                    database.signalPreKeyDao().insert(
                        SignalPreKey(
                            preKeyId = preKey.id,
                            preKeyRecord = preKey.serialize()
                        )
                    )
                }
                
                // Store signed pre-key
                database.signalSignedPreKeyDao().insert(
                    SignalSignedPreKey(
                        signedPreKeyId = signedPreKey.id,
                        signedPreKeyRecord = signedPreKey.serialize()
                    )
                )
                
                Log.d(TAG, "Registration successful")
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "Registration failed", e)
            false
        }
    }
    
    /**
     * Request SMS verification code
     */
    suspend fun requestVerificationCode(phoneNumber: String): Boolean = withContext(Dispatchers.IO) {
        try {
            signalApi.requestSmsVerification(phoneNumber)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request verification code", e)
            false
        }
    }
    
    /**
     * Send an encrypted message to a recipient
     */
    suspend fun sendMessage(recipientNumber: String, message: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!isInitialized) {
                throw IllegalStateException("Signal client not initialized")
            }
            
            val recipientAddress = SignalProtocolAddress(recipientNumber, 1)
            
            // Check if we have a session with this recipient
            if (!sessionStore.containsSession(recipientAddress)) {
                // Fetch recipient's pre-key bundle and establish session
                val preKeyBundle = signalApi.getPreKeyBundle(recipientNumber)
                    ?: throw IllegalStateException("Could not fetch pre-key bundle")
                
                val sessionBuilder = SessionBuilder(signalProtocolStore, recipientAddress)
                sessionBuilder.process(preKeyBundle)
                
                Log.d(TAG, "Established new session with $recipientNumber")
            }
            
            // Encrypt the message
            val sessionCipher = SessionCipher(signalProtocolStore, recipientAddress)
            val ciphertext = sessionCipher.encrypt(message.toByteArray(Charsets.UTF_8))
            
            // Send via Signal API
            val success = signalApi.sendMessage(
                recipientNumber = recipientNumber,
                ciphertext = ciphertext.serialize(),
                messageType = ciphertext.type
            )
            
            Log.d(TAG, "Message sent successfully: $success")
            success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message", e)
            false
        }
    }
    
    /**
     * Check if client is registered
     */
    fun isRegistered(): Boolean {
        return isInitialized && identityKeyPair != null
    }
    
    // Private helper methods
    
    private fun initializeStores() {
        sessionStore = DatabaseSessionStore(database)
        preKeyStore = DatabasePreKeyStore(database)
        signedPreKeyStore = DatabaseSignedPreKeyStore(database)
        identityKeyStore = DatabaseIdentityKeyStore(database)
        
        signalProtocolStore = object : SignalProtocolStore,
            SessionStore by sessionStore,
            PreKeyStore by preKeyStore,
            SignedPreKeyStore by signedPreKeyStore,
            IdentityKeyStore by identityKeyStore {
            
            override fun storeSenderKey(sender: SignalProtocolAddress, distributionId: java.util.UUID, record: org.signal.libsignal.protocol.groups.state.SenderKeyRecord) {
                // Not implemented - we don't use sender keys for 1:1 messaging
            }
            
            override fun loadSenderKey(sender: SignalProtocolAddress, distributionId: java.util.UUID): org.signal.libsignal.protocol.groups.state.SenderKeyRecord? {
                return null
            }
        }
    }
    
    private suspend fun generateIdentity() {
        registrationId = KeyHelper.generateRegistrationId(false)
        identityKeyPair = IdentityKeyPair.generate()
        
        // Store identity
        database.signalIdentityDao().insert(
            SignalIdentity(
                id = 1,
                registrationId = registrationId,
                identityKeyPair = identityKeyPair!!.serialize(),
                isLocal = true
            )
        )
    }
    
    private fun generatePreKeys(start: Int, count: Int): List<PreKeyRecord> {
        return (start until start + count).map { id ->
            val keyPair = Curve.generateKeyPair()
            PreKeyRecord(id, keyPair)
        }
    }
    
    private fun generateSignedPreKey(identityKeyPair: IdentityKeyPair, signedPreKeyId: Int): SignedPreKeyRecord {
        val keyPair = Curve.generateKeyPair()
        val signature = Curve.calculateSignature(
            identityKeyPair.privateKey,
            keyPair.publicKey.serialize()
        )
        return SignedPreKeyRecord(
            signedPreKeyId,
            System.currentTimeMillis(),
            keyPair,
            signature
        )
    }
}

/**
 * Database-backed session store
 */
class DatabaseSessionStore(private val database: AppDatabase) : SessionStore {
    
    override fun loadSession(address: SignalProtocolAddress): SessionRecord {
        val session = database.signalSessionDao().getSession(address.name, address.deviceId)
        return if (session != null) {
            SessionRecord(session.sessionRecord)
        } else {
            SessionRecord()
        }
    }
    
    override fun loadExistingSessions(addresses: MutableList<SignalProtocolAddress>): MutableList<SessionRecord> {
        return addresses.map { loadSession(it) }.toMutableList()
    }
    
    override fun getSubDeviceSessions(name: String): MutableList<Int> {
        return database.signalSessionDao().getDeviceIds(name).toMutableList()
    }
    
    override fun storeSession(address: SignalProtocolAddress, record: SessionRecord) {
        database.signalSessionDao().insert(
            SignalSession(
                recipientId = address.name,
                deviceId = address.deviceId,
                sessionRecord = record.serialize()
            )
        )
    }
    
    override fun containsSession(address: SignalProtocolAddress): Boolean {
        return database.signalSessionDao().getSession(address.name, address.deviceId) != null
    }
    
    override fun deleteSession(address: SignalProtocolAddress) {
        database.signalSessionDao().delete(address.name, address.deviceId)
    }
    
    override fun deleteAllSessions(name: String) {
        database.signalSessionDao().deleteAll(name)
    }
}

/**
 * Database-backed pre-key store
 */
class DatabasePreKeyStore(private val database: AppDatabase) : PreKeyStore {
    
    override fun loadPreKey(preKeyId: Int): PreKeyRecord {
        val preKey = database.signalPreKeyDao().getPreKey(preKeyId)
            ?: throw InvalidKeyIdException("No pre-key found with id: $preKeyId")
        return PreKeyRecord(preKey.preKeyRecord)
    }
    
    override fun storePreKey(preKeyId: Int, record: PreKeyRecord) {
        database.signalPreKeyDao().insert(
            SignalPreKey(preKeyId = preKeyId, preKeyRecord = record.serialize())
        )
    }
    
    override fun containsPreKey(preKeyId: Int): Boolean {
        return database.signalPreKeyDao().getPreKey(preKeyId) != null
    }
    
    override fun removePreKey(preKeyId: Int) {
        database.signalPreKeyDao().delete(preKeyId)
    }
}

/**
 * Database-backed signed pre-key store
 */
class DatabaseSignedPreKeyStore(private val database: AppDatabase) : SignedPreKeyStore {
    
    override fun loadSignedPreKey(signedPreKeyId: Int): SignedPreKeyRecord {
        val signedPreKey = database.signalSignedPreKeyDao().getSignedPreKey(signedPreKeyId)
            ?: throw InvalidKeyIdException("No signed pre-key found with id: $signedPreKeyId")
        return SignedPreKeyRecord(signedPreKey.signedPreKeyRecord)
    }
    
    override fun loadSignedPreKeys(): MutableList<SignedPreKeyRecord> {
        return database.signalSignedPreKeyDao().getAllSignedPreKeys()
            .map { SignedPreKeyRecord(it.signedPreKeyRecord) }
            .toMutableList()
    }
    
    override fun storeSignedPreKey(signedPreKeyId: Int, record: SignedPreKeyRecord) {
        database.signalSignedPreKeyDao().insert(
            SignalSignedPreKey(signedPreKeyId = signedPreKeyId, signedPreKeyRecord = record.serialize())
        )
    }
    
    override fun containsSignedPreKey(signedPreKeyId: Int): Boolean {
        return database.signalSignedPreKeyDao().getSignedPreKey(signedPreKeyId) != null
    }
    
    override fun removeSignedPreKey(signedPreKeyId: Int) {
        database.signalSignedPreKeyDao().delete(signedPreKeyId)
    }
}

/**
 * Database-backed identity key store
 */
class DatabaseIdentityKeyStore(private val database: AppDatabase) : IdentityKeyStore {
    
    override fun getIdentityKeyPair(): IdentityKeyPair {
        val identity = database.signalIdentityDao().getLocalIdentity()
            ?: throw IllegalStateException("No local identity found")
        return IdentityKeyPair(identity.identityKeyPair)
    }
    
    override fun getLocalRegistrationId(): Int {
        val identity = database.signalIdentityDao().getLocalIdentity()
            ?: throw IllegalStateException("No local identity found")
        return identity.registrationId
    }
    
    override fun saveIdentity(address: SignalProtocolAddress, identityKey: IdentityKey): Boolean {
        val existing = database.signalIdentityDao().getIdentity(address.name)
        database.signalIdentityDao().insert(
            SignalIdentity(
                recipientId = address.name,
                registrationId = 0,
                identityKeyPair = identityKey.serialize(),
                isLocal = false
            )
        )
        return existing != null && !existing.identityKeyPair.contentEquals(identityKey.serialize())
    }
    
    override fun isTrustedIdentity(
        address: SignalProtocolAddress,
        identityKey: IdentityKey,
        direction: IdentityKeyStore.Direction
    ): Boolean {
        val existing = database.signalIdentityDao().getIdentity(address.name)
        return existing == null || existing.identityKeyPair.contentEquals(identityKey.serialize())
    }
    
    override fun getIdentity(address: SignalProtocolAddress): IdentityKey? {
        val identity = database.signalIdentityDao().getIdentity(address.name)
        return identity?.let { IdentityKey(it.identityKeyPair) }
    }
}
