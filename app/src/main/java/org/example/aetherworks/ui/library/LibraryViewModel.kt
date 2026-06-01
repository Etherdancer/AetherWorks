package org.example.aetherworks.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.aetherworks.storage.db.AetherDatabase
import org.example.aetherworks.storage.db.entity.ContentUnit
import org.example.aetherworks.storage.db.entity.Visibility

class LibraryViewModel : ViewModel() {
    private val _libraryContent = MutableStateFlow<List<ContentUnit>>(emptyList())
    val libraryContent: StateFlow<List<ContentUnit>> = _libraryContent.asStateFlow()

    init {
        loadContent()
    }

    fun loadContent() {
        viewModelScope.launch {
            try {
                val db = AetherDatabase.getPrivateDatabase()
                val privateContent = db.contentDao().getByVisibility(Visibility.PRIVATE)
                val trustedContent = db.contentDao().getByVisibility(Visibility.TRUSTED)
                
                val combined = (privateContent + trustedContent).sortedByDescending { it.timestamp }
                _libraryContent.value = combined
            } catch (e: Exception) {
                // Not initialized
            }
        }
    }
}
