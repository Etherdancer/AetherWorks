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

    fun loadContent(query: String = "") {
        viewModelScope.launch {
            try {
                val db = AetherDatabase.getPrivateDatabase()
                
                val results = if (query.isBlank()) {
                    val privateContent = db.contentDao().getByVisibility(Visibility.PRIVATE)
                    val trustedContent = db.contentDao().getByVisibility(Visibility.TRUSTED)
                    privateContent + trustedContent
                } else {
                    // Smart Scan: query formatting (e.g. adding * for prefix matching)
                    val ftsQuery = query.trim().split(" ").joinToString(" ") { "$it*" }
                    db.contentDao().searchSmartScan(ftsQuery).filter { 
                        it.visibility == Visibility.PRIVATE || it.visibility == Visibility.TRUSTED 
                    }
                }
                
                _libraryContent.value = results.sortedByDescending { it.timestamp }
            } catch (e: Exception) {
                // Not initialized
            }
        }
    }
}
