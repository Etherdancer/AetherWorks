package org.example.aetherworks.discovery

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.example.aetherworks.storage.db.entity.ContentUnit

class ContentIndexer {

    private val _selectedCategories = MutableStateFlow<Set<String>>(emptySet())
    val selectedCategories: StateFlow<Set<String>> = _selectedCategories.asStateFlow()

    private val _selectedEmotions = MutableStateFlow<Set<String>>(emptySet())
    val selectedEmotions: StateFlow<Set<String>> = _selectedEmotions.asStateFlow()

    private val _isAndFilter = MutableStateFlow<Boolean>(true)
    val isAndFilter: StateFlow<Boolean> = _isAndFilter.asStateFlow()

    private val _sortMode = MutableStateFlow<SortMode>(SortMode.CHRONOLOGICAL)
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()

    fun toggleCategory(category: String) {
        val current = _selectedCategories.value.toMutableSet()
        if (current.contains(category)) current.remove(category) else current.add(category)
        _selectedCategories.value = current
    }

    fun toggleEmotion(emotion: String) {
        val current = _selectedEmotions.value.toMutableSet()
        if (current.contains(emotion)) current.remove(emotion) else current.add(emotion)
        _selectedEmotions.value = current
    }

    fun setFilterMode(isAnd: Boolean) {
        _isAndFilter.value = isAnd
    }

    fun setSortMode(mode: SortMode) {
        _sortMode.value = mode
    }

    fun filterAndSort(content: List<ContentUnit>): List<ContentUnit> {
        val cats = _selectedCategories.value
        val emos = _selectedEmotions.value
        val isAnd = _isAndFilter.value

        val filtered = content.filter { unit ->
            val unitCats = unit.categoryFlags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
            val unitEmos = unit.emotionFlags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()

            if (cats.isEmpty() && emos.isEmpty()) return@filter true

            val catMatch = if (cats.isEmpty()) isAnd else {
                if (isAnd) unitCats.containsAll(cats) else cats.any { unitCats.contains(it) }
            }

            val emoMatch = if (emos.isEmpty()) isAnd else {
                if (isAnd) unitEmos.containsAll(emos) else emos.any { unitEmos.contains(it) }
            }

            if (isAnd) {
                (cats.isEmpty() || catMatch) && (emos.isEmpty() || emoMatch)
            } else {
                catMatch || emoMatch
            }
        }

        return when (_sortMode.value) {
            SortMode.CHRONOLOGICAL -> filtered.sortedByDescending { it.timestamp }
            SortMode.REPUTATION -> {
                val now = System.currentTimeMillis()
                filtered.sortedByDescending { unit ->
                    val reputation = unit.likeTokens.size - unit.dislikeTokens.size
                    // HackerNews-style gravity algorithm
                    // Score = (Reputation - 1) / (AgeInHours + 2)^1.5
                    val ageInHours = (now - unit.timestamp) / (1000.0 * 60 * 60)
                    val gravity = 1.5
                    
                    // If reputation is 0 or less, just sort it chronologically below the positive ones
                    if (reputation <= 0) {
                        reputation - ageInHours
                    } else {
                        (reputation - 1) / Math.pow(ageInHours + 2.0, gravity)
                    }
                }
            }
            SortMode.POPULARITY -> filtered.sortedByDescending { it.importCount }
            SortMode.ALPHABETICAL -> filtered.sortedBy { it.title }
        }
    }

    companion object {
        val CATEGORIES = listOf(
            "Politics", "Religion", "Sports", "Music", "News", "Science", "Technology", "Art", 
            "Literature", "History", "Philosophy", "Health", "Food", "Travel", "Nature", 
            "Comedy", "Education", "DIY", "Finance", "Fashion", "Gaming", "Movies", "TV", 
            "Photography", "Podcasts", "Automotive", "Pets", "Parenting", "Relationships", 
            "Career", "Fitness", "Mental Health", "Culture", "Language", "Architecture", 
            "Gardening", "Crafts", "Collecting", "Volunteering", "Local Events", "Other"
        )

        val EMOTIONS = listOf(
            "Happy", "Sad", "Cheerful", "Angry", "Inspired", "Anxious", "Calm", "Excited", 
            "Nostalgic", "Amused", "Hopeful", "Frustrated", "Grateful", "Confused", "Proud", 
            "Disgusted", "Surprised", "Moved", "Bored", "Scared", "Empowered", "Lonely", 
            "Peaceful", "Curious", "Overwhelmed", "Determined", "Melancholic", "Playful", 
            "Tender", "Rebellious"
        )
    }
}

enum class SortMode {
    CHRONOLOGICAL, REPUTATION, POPULARITY, ALPHABETICAL
}
