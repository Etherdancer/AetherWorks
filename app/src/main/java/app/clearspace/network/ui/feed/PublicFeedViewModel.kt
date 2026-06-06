package app.clearspace.network.ui.feed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import app.clearspace.network.discovery.ContentIndexer
import app.clearspace.network.storage.db.AetherDatabase
import app.clearspace.network.storage.db.entity.ContentUnit
import app.clearspace.network.storage.db.entity.Visibility
import app.clearspace.network.reputation.ReputationAgent
import app.clearspace.network.crypto.KeyManager

class PublicFeedViewModel(application: Application) : AndroidViewModel(application) {
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

    fun reportContent(unit: ContentUnit, reason: String) {
        viewModelScope.launch {
            try {
                // Delete locally so it's not rendered or shared further
                val db = AetherDatabase.getSharedDatabase()
                db.contentDao().delete(unit.contentHash)
                loadContent()
                
                // Upload report to Firestore and the global blacklist
                val context = getApplication<Application>()
                app.clearspace.network.moderation.ContentReporter(context).reportContent(unit.contentHash, reason)
                
                val repAgent = ReputationAgent(context, KeyManager(context))
                repAgent.submitReport(unit.contentHash)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
