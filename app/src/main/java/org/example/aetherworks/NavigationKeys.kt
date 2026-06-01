package org.example.aetherworks

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey

@Serializable data object FeedTab : NavKey
@Serializable data object SocialTab : NavKey
@Serializable data object LibraryTab : NavKey
@Serializable data object UtilitiesTab : NavKey

@Serializable data object CreateContent : NavKey
@Serializable data object ProfileSettings : NavKey

// Phase 3 Utility Keys
@Serializable data object VaultScreen : NavKey
@Serializable data object CalendarScreen : NavKey
@Serializable data object TasksScreen : NavKey
@Serializable data object MediaPlayerScreen : NavKey
