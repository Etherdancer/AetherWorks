package app.clearspace.network.auth

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * FIX H2: This composable was previously a dead code authentication path that used a
 * hardcoded dummy salt (ByteArray(16){1}) in its Argon2id derivation, making all
 * derived keys trivially precomputable. Since all authentication is routed through
 * GatekeeperViewModel -> GatekeeperRepository (which uses correct salted Argon2id),
 * this screen is removed.
 *
 * This stub exists only to prevent compile errors if any old import remains.
 * It must never be shown to the user.
 */
@Composable
fun AuthScreen(
    onAuthSuccess: (ByteArray) -> Unit,
    modifier: Modifier = Modifier
) {
    // SECURITY: This screen is intentionally disabled. Authentication must go through
    // GatekeeperViewModel. If you see this screen, there is a navigation bug.
    Text("Error: invalid authentication path. Please restart the app.", modifier = modifier)
    throw IllegalStateException("AuthScreen must not be used. Route authentication through GatekeeperViewModel.")
}
