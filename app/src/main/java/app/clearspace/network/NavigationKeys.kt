package app.clearspace.network

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey

@Serializable data object FeedTab : NavKey
@Serializable data object SocialTab : NavKey
@Serializable data object LibraryTab : NavKey

@Serializable data class CreateContent(val prefillTitle: String? = null, val prefillBody: String? = null, val forceVisibilityGroup: Boolean = false) : NavKey
@Serializable data object ProfileSettings : NavKey
@Serializable data object AboutSettings : NavKey
@Serializable data object ManageGroups : NavKey

@Serializable data object RemoteLinkExchange : NavKey
@Serializable data object TrustVerification : NavKey
