package org.example.aetherworks.storage.db.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.example.aetherworks.storage.db.entity.VaultNote
import org.example.aetherworks.storage.db.entity.VaultPassword

@Dao
interface VaultDao {
    // Passwords
    @Query("SELECT * FROM vault_passwords ORDER BY title ASC")
    fun getAllPasswords(): Flow<List<VaultPassword>>

    @Query("SELECT * FROM vault_passwords WHERE category = :category ORDER BY title ASC")
    fun getPasswordsByCategory(category: String): Flow<List<VaultPassword>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPassword(password: VaultPassword)

    @Delete
    suspend fun deletePassword(password: VaultPassword)

    @Query("SELECT * FROM vault_passwords WHERE id = :id LIMIT 1")
    suspend fun getPasswordById(id: String): VaultPassword?

    // Notes
    @Query("SELECT * FROM vault_notes ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotes(): Flow<List<VaultNote>>

    @Query("SELECT * FROM vault_notes WHERE folder = :folder ORDER BY isPinned DESC, updatedAt DESC")
    fun getNotesByFolder(folder: String): Flow<List<VaultNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: VaultNote)

    @Delete
    suspend fun deleteNote(note: VaultNote)

    @Query("SELECT * FROM vault_notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: String): VaultNote?
}
