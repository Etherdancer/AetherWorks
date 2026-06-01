package org.example.aetherworks.ui.utilities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.example.aetherworks.storage.db.dao.VaultDao
import org.example.aetherworks.storage.db.entity.VaultPassword
import org.example.aetherworks.storage.db.entity.VaultNote
import java.util.UUID

class VaultViewModel(private val vaultDao: VaultDao) : ViewModel() {
    val passwords: StateFlow<List<VaultPassword>> = vaultDao.getAllPasswords()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recipes: StateFlow<List<VaultNote>> = vaultDao.getNotesByTag("#recipe")
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addPassword(title: String, username: String, encryptedPasswordBlob: String) {
        viewModelScope.launch {
            val newEntry = VaultPassword(
                id = UUID.randomUUID().toString(),
                title = title,
                username = username,
                encryptedPasswordBlob = encryptedPasswordBlob
            )
            vaultDao.insertPassword(newEntry)
        }
    }

    fun deletePassword(id: String) {
        viewModelScope.launch {
            vaultDao.deletePasswordById(id)
        }
    }

    fun addRecipe(title: String, body: String) {
        viewModelScope.launch {
            val newNote = VaultNote(
                id = UUID.randomUUID().toString(),
                title = title,
                markdownContent = body,
                tags = "recipe",
                folder = "Cookbook"
            )
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                vaultDao.insertNote(newNote)
            }
        }
    }
}
