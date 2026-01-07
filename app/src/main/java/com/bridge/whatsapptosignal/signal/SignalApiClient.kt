package com.bridge.whatsapptosignal.signal

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.ecc.ECPublicKey
import org.signal.libsignal.protocol.state.PreKeyBundle
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Client for communicating with Signal servers
 * 
 * Note: This is a simplified implementation. Production use would require:
 * - Proper certificate pinning
 * - WebSocket for receiving messages
 * - Proper error handling and retry logic
 */
class SignalApiClient {
    
    private val TAG = "SignalApiClient"
    
    // Signal server endpoints
    private val SIGNAL_SERVICE_URL = "https://chat.signal.org"
    private val SIGNAL_CDN_URL = "https://cdn.signal.org"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    
    // Authentication
    private var authUsername: String? = null
    private var authPassword: String? = null
    
    /**
     * Request SMS verification code
     */
    suspend fun requestSmsVerification(phoneNumber: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val normalizedNumber = normalizePhoneNumber(phoneNumber)
            
            val request = Request.Builder()
                .url("$SIGNAL_SERVICE_URL/v1/accounts/sms/code/$normalizedNumber")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                Log.d(TAG, "SMS verification requested successfully")
                true
            } else {
                Log.e(TAG, "Failed to request SMS verification: ${response.code}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting SMS verification", e)
            false
        }
    }
    
    /**
     * Register device with Signal servers
     */
    suspend fun register(
        phoneNumber: String,
        verificationCode: String,
        registrationId: Int,
        identityKey: IdentityKey,
        signedPreKey: SignedPreKeyRecord,
        preKeys: List<PreKeyRecord>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val normalizedNumber = normalizePhoneNumber(phoneNumber)
            val cleanCode = verificationCode.replace("-", "").replace(" ", "")
            
            // Generate device password
            val password = generatePassword()
            authUsername = normalizedNumber
            authPassword = password
            
            // Build registration JSON
            val registrationJson = JSONObject().apply {
                put("signalingKey", generateSignalingKey())
                put("fetchesMessages", true)
                put("registrationId", registrationId)
                put("voice", false)
                put("video", false)
                put("pin", JSONObject.NULL)
            }
            
            // Verify code and register
            val request = Request.Builder()
                .url("$SIGNAL_SERVICE_URL/v1/accounts/code/$cleanCode")
                .put(registrationJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                .addHeader("Authorization", basicAuth(normalizedNumber, password))
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.e(TAG, "Registration failed: ${response.code} - ${response.body?.string()}")
                return@withContext false
            }
            
            // Upload pre-keys
            val preKeysUploaded = uploadPreKeys(identityKey, signedPreKey, preKeys)
            if (!preKeysUploaded) {
                Log.e(TAG, "Failed to upload pre-keys")
                return@withContext false
            }
            
            Log.d(TAG, "Registration completed successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Registration error", e)
            false
        }
    }
    
    /**
     * Upload pre-keys to Signal servers
     */
    private suspend fun uploadPreKeys(
        identityKey: IdentityKey,
        signedPreKey: SignedPreKeyRecord,
        preKeys: List<PreKeyRecord>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val preKeysJson = JSONArray().apply {
                preKeys.forEach { preKey ->
                    put(JSONObject().apply {
                        put("keyId", preKey.id)
                        put("publicKey", Base64.encodeToString(preKey.keyPair.publicKey.serialize(), Base64.NO_WRAP))
                    })
                }
            }
            
            val json = JSONObject().apply {
                put("identityKey", Base64.encodeToString(identityKey.serialize(), Base64.NO_WRAP))
                put("preKeys", preKeysJson)
                put("signedPreKey", JSONObject().apply {
                    put("keyId", signedPreKey.id)
                    put("publicKey", Base64.encodeToString(signedPreKey.keyPair.publicKey.serialize(), Base64.NO_WRAP))
                    put("signature", Base64.encodeToString(signedPreKey.signature, Base64.NO_WRAP))
                })
            }
            
            val request = Request.Builder()
                .url("$SIGNAL_SERVICE_URL/v2/keys")
                .put(json.toString().toRequestBody(JSON_MEDIA_TYPE))
                .addHeader("Authorization", getAuthHeader())
                .build()
            
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading pre-keys", e)
            false
        }
    }
    
    /**
     * Get pre-key bundle for a recipient
     */
    suspend fun getPreKeyBundle(recipientNumber: String): PreKeyBundle? = withContext(Dispatchers.IO) {
        try {
            val normalizedNumber = normalizePhoneNumber(recipientNumber)
            
            val request = Request.Builder()
                .url("$SIGNAL_SERVICE_URL/v2/keys/$normalizedNumber/1")
                .get()
                .addHeader("Authorization", getAuthHeader())
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.e(TAG, "Failed to get pre-key bundle: ${response.code}")
                return@withContext null
            }
            
            val json = JSONObject(response.body?.string() ?: return@withContext null)
            
            // Parse the response and create PreKeyBundle
            val deviceJson = json.getJSONArray("devices").getJSONObject(0)
            val preKeyJson = deviceJson.getJSONObject("preKey")
            val signedPreKeyJson = deviceJson.getJSONObject("signedPreKey")
            
            val identityKeyBytes = Base64.decode(json.getString("identityKey"), Base64.NO_WRAP)
            val preKeyPublicBytes = Base64.decode(preKeyJson.getString("publicKey"), Base64.NO_WRAP)
            val signedPreKeyPublicBytes = Base64.decode(signedPreKeyJson.getString("publicKey"), Base64.NO_WRAP)
            val signedPreKeySignature = Base64.decode(signedPreKeyJson.getString("signature"), Base64.NO_WRAP)
            
            PreKeyBundle(
                deviceJson.getInt("registrationId"),
                1, // device ID
                preKeyJson.getInt("keyId"),
                ECPublicKey(preKeyPublicBytes),
                signedPreKeyJson.getInt("keyId"),
                ECPublicKey(signedPreKeyPublicBytes),
                signedPreKeySignature,
                IdentityKey(identityKeyBytes)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting pre-key bundle", e)
            null
        }
    }
    
    /**
     * Send an encrypted message
     */
    suspend fun sendMessage(
        recipientNumber: String,
        ciphertext: ByteArray,
        messageType: Int
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val normalizedNumber = normalizePhoneNumber(recipientNumber)
            
            val messageJson = JSONObject().apply {
                put("type", messageType)
                put("destinationDeviceId", 1)
                put("destinationRegistrationId", 0) // Will be filled by server
                put("content", Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            }
            
            val messagesJson = JSONObject().apply {
                put("messages", JSONArray().put(messageJson))
                put("timestamp", System.currentTimeMillis())
                put("online", false)
            }
            
            val request = Request.Builder()
                .url("$SIGNAL_SERVICE_URL/v1/messages/$normalizedNumber")
                .put(messagesJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                .addHeader("Authorization", getAuthHeader())
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                Log.d(TAG, "Message sent successfully")
                true
            } else {
                Log.e(TAG, "Failed to send message: ${response.code}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
            false
        }
    }
    
    // Helper methods
    
    private fun normalizePhoneNumber(number: String): String {
        return number.replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")
            .let { if (it.startsWith("+")) it else "+$it" }
    }
    
    private fun generatePassword(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..24).map { chars.random() }.joinToString("")
    }
    
    private fun generateSignalingKey(): String {
        val bytes = ByteArray(52)
        java.security.SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
    
    private fun basicAuth(username: String, password: String): String {
        val credentials = "$username:$password"
        return "Basic " + Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
    }
    
    private fun getAuthHeader(): String {
        return basicAuth(authUsername ?: "", authPassword ?: "")
    }
}
