package org.example.aetherworks.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.example.aetherworks.discovery.ContentIndexer
import org.example.aetherworks.storage.db.AetherDatabase
import org.example.aetherworks.storage.db.entity.ContentUnit
import org.example.aetherworks.storage.db.entity.Visibility

class PublicFeedViewModel : ViewModel() {
    val indexer = ContentIndexer()
    
    private val _rawContent = MutableStateFlow<List<ContentUnit>>(emptyList())
    
    private val _feedContent = MutableStateFlow<List<ContentUnit>>(emptyList())
    val feedContent: StateFlow<List<ContentUnit>> = _feedContent.asStateFlow()

    init {
        loadContent()
        
        viewModelScope.launch {
            combine(
                _rawContent,
                indexer.selectedCategories,
                indexer.selectedEmotions,
                indexer.isAndFilter,
                indexer.sortMode
            ) { raw, _, _, _, _ ->
                indexer.filterAndSort(raw)
            }.collectLatest { filtered ->
                _feedContent.value = filtered
            }
        }
    }

    fun loadContent() {
        viewModelScope.launch {
            try {
                val db = AetherDatabase.getSharedDatabase()
                val content = db.contentDao().getByVisibility(Visibility.PUBLIC)
                _rawContent.value = content
            } catch (e: Exception) {
                // Not initialized
            }
        }
    }
}
