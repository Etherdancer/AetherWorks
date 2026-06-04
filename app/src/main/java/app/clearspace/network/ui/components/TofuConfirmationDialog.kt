package app.clearspace.network.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import app.clearspace.network.security.guard.TofuPendingManager

/**
 * Observes TofuPendingManager and shows a dialog for each new peer connection
 * that requires user confirmation before TOFU pinning proceeds.
 *
 * Place this composable at the root of the app scaffold so it is always visible
 * regardless of which screen the user is on.
 *
 * Security: This dialog prevents the first-connection MITM window (T1557 / OWASP M5).
 * Without user confirmation, an ARP-spoofing attacker on the same LAN could
 * permanently insert themselves as a MITM before TOFU trust is established.
 */
@Composable
fun TofuConfirmationHost() {
    // Poll TofuPendingManager for pending requests.
    // We use a simple polling loop (every 500ms) to avoid requiring a Flow/LiveData
    // refactor of the TofuPendingManager singleton.
    var pendingEntry by remember { mutableStateOf<Pair<String, String>?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            val entries = TofuPendingManager.getPendingRequests()
            if (pendingEntry == null && entries.isNotEmpty()) {
                val first = entries.entries.first()
                pendingEntry = Pair(first.key, first.value)
            }
            delay(500)
        }
    }

    val current = pendingEntry
    if (current != null) {
        TofuConfirmationDialog(
            ip = current.first,
            fingerprintBase64 = current.second,
            onAccept = {
                TofuPendingManager.resolve(current.first, accepted = true)
                pendingEntry = null
            },
            onReject = {
                TofuPendingManager.resolve(current.first, accepted = false)
                pendingEntry = null
            }
        )
    }
}

/**
 * Formats a Base64-encoded SHA-256 fingerprint into a readable colon-separated hex string.
 * Example: "AB:12:CD:34:..."
 */
private fun formatFingerprint(base64: String): String {
    return try {
        val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
        bytes.joinToString(":") { "%02X".format(it) }
    } catch (e: Exception) {
        base64 // Fallback to raw Base64 if decoding fails
    }
}

@Composable
fun TofuConfirmationDialog(
    ip: String,
    fingerprintBase64: String,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val formattedFingerprint = remember(fingerprintBase64) { formatFingerprint(fingerprintBase64) }

    AlertDialog(
        onDismissRequest = onReject, // Dismissing = rejecting (safest default)
        icon = {
            Icon(
                Icons.Default.Lock,
                contentDescription = "Security Check",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text("New Device Detected", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "A nearby device is trying to connect for the first time.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "To make sure no one is intercepting your connection, compare this fingerprint with what the other person sees on their screen.",
                    style = MaterialTheme.typography.bodySmall
                )
                HorizontalDivider()
                Text(
                    "Device: $ip",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    "Security Fingerprint:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = formattedFingerprint,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(10.dp)
                )
                HorizontalDivider()
                Text(
                    "If they match, tap Accept. If they don't match, tap Reject — someone may be trying to intercept your connection.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = {
            Button(onClick = onAccept) {
                Text("Accept")
            }
        },
        dismissButton = {
            Button(
                onClick = onReject,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Reject")
            }
        }
    )
}
