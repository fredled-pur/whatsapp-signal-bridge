package com.bridge.whatsapptosignal.data.entity

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SignalIdentityDao {
    @Query("SELECT * FROM signal_identities WHERE is_local = 1 LIMIT 1")
    fun getLocalIdentity(): SignalIdentity?
    
    @Query("SELECT * FROM signal_identities WHERE recipient_id = :recipientId LIMIT 1")
    fun getIdentity(recipientId: String): SignalIdentity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(identity: SignalIdentity)
    
    @Query("DELETE FROM signal_identities WHERE recipient_id = :recipientId")
    fun delete(recipientId: String)
}

@Dao
interface SignalSessionDao {
    @Query("SELECT * FROM signal_sessions WHERE recipient_id = :recipientId AND device_id = :deviceId LIMIT 1")
    fun getSession(recipientId: String, deviceId: Int): SignalSession?
    
    @Query("SELECT device_id FROM signal_sessions WHERE recipient_id = :recipientId")
    fun getDeviceIds(recipientId: String): List<Int>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(session: SignalSession)
    
    @Query("DELETE FROM signal_sessions WHERE recipient_id = :recipientId AND device_id = :deviceId")
    fun delete(recipientId: String, deviceId: Int)
    
    @Query("DELETE FROM signal_sessions WHERE recipient_id = :recipientId")
    fun deleteAll(recipientId: String)
}

@Dao
interface SignalPreKeyDao {
    @Query("SELECT * FROM signal_pre_keys WHERE pre_key_id = :preKeyId LIMIT 1")
    fun getPreKey(preKeyId: Int): SignalPreKey?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(preKey: SignalPreKey)
    
    @Query("DELETE FROM signal_pre_keys WHERE pre_key_id = :preKeyId")
    fun delete(preKeyId: Int)
}

@Dao
interface SignalSignedPreKeyDao {
    @Query("SELECT * FROM signal_signed_pre_keys WHERE signed_pre_key_id = :signedPreKeyId LIMIT 1")
    fun getSignedPreKey(signedPreKeyId: Int): SignalSignedPreKey?
    
    @Query("SELECT * FROM signal_signed_pre_keys")
    fun getAllSignedPreKeys(): List<SignalSignedPreKey>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(signedPreKey: SignalSignedPreKey)
    
    @Query("DELETE FROM signal_signed_pre_keys WHERE signed_pre_key_id = :signedPreKeyId")
    fun delete(signedPreKeyId: Int)
}

@Dao
interface MessageLogDao {
    @Query("SELECT * FROM message_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<MessageLog>>
    
    @Query("SELECT * FROM message_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int): Flow<List<MessageLog>>
    
    @Query("SELECT * FROM message_logs WHERE status = :status ORDER BY timestamp DESC")
    fun getLogsByStatus(status: MessageStatus): Flow<List<MessageLog>>
    
    @Query("SELECT COUNT(*) FROM message_logs WHERE status = :status")
    fun getCountByStatus(status: MessageStatus): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM message_logs WHERE date(timestamp / 1000, 'unixepoch') = date('now')")
    fun getTodayCount(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM message_logs")
    fun getTotalCount(): Flow<Int>
    
    @Insert
    fun insert(log: MessageLog)
    
    @Query("DELETE FROM message_logs")
    fun deleteAll()
}
