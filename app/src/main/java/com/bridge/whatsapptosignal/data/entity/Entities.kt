package com.bridge.whatsapptosignal.data.entity

import androidx.room.*
import java.util.Date

/**
 * Stores Signal identity keys
 */
@Entity(tableName = "signal_identities")
data class SignalIdentity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "recipient_id")
    val recipientId: String? = null,
    
    @ColumnInfo(name = "registration_id")
    val registrationId: Int,
    
    @ColumnInfo(name = "identity_key_pair", typeAffinity = ColumnInfo.BLOB)
    val identityKeyPair: ByteArray,
    
    @ColumnInfo(name = "is_local")
    val isLocal: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SignalIdentity
        return id == other.id
    }
    
    override fun hashCode(): Int = id.hashCode()
}

/**
 * Stores Signal sessions with other users
 */
@Entity(
    tableName = "signal_sessions",
    primaryKeys = ["recipient_id", "device_id"]
)
data class SignalSession(
    @ColumnInfo(name = "recipient_id")
    val recipientId: String,
    
    @ColumnInfo(name = "device_id")
    val deviceId: Int,
    
    @ColumnInfo(name = "session_record", typeAffinity = ColumnInfo.BLOB)
    val sessionRecord: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SignalSession
        return recipientId == other.recipientId && deviceId == other.deviceId
    }
    
    override fun hashCode(): Int = 31 * recipientId.hashCode() + deviceId
}

/**
 * Stores Signal pre-keys
 */
@Entity(tableName = "signal_pre_keys")
data class SignalPreKey(
    @PrimaryKey
    @ColumnInfo(name = "pre_key_id")
    val preKeyId: Int,
    
    @ColumnInfo(name = "pre_key_record", typeAffinity = ColumnInfo.BLOB)
    val preKeyRecord: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SignalPreKey
        return preKeyId == other.preKeyId
    }
    
    override fun hashCode(): Int = preKeyId
}

/**
 * Stores Signal signed pre-keys
 */
@Entity(tableName = "signal_signed_pre_keys")
data class SignalSignedPreKey(
    @PrimaryKey
    @ColumnInfo(name = "signed_pre_key_id")
    val signedPreKeyId: Int,
    
    @ColumnInfo(name = "signed_pre_key_record", typeAffinity = ColumnInfo.BLOB)
    val signedPreKeyRecord: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SignalSignedPreKey
        return signedPreKeyId == other.signedPreKeyId
    }
    
    override fun hashCode(): Int = signedPreKeyId
}

/**
 * Message forwarding status
 */
enum class MessageStatus {
    FORWARDED,
    FAILED,
    FILTERED,
    PENDING
}

/**
 * Stores message forwarding logs
 */
@Entity(tableName = "message_logs")
data class MessageLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "sender")
    val sender: String,
    
    @ColumnInfo(name = "text_preview")
    val textPreview: String,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Date,
    
    @ColumnInfo(name = "status")
    val status: MessageStatus
)
