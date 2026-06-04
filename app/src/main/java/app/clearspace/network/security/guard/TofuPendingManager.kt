package app.clearspace.network.security.guard

import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.ConcurrentHashMap

/**
 * Bridges the background TLS handshake thread with the UI layer.
 *
 * When a peer's certificate is seen for the first time (TOFU first-use),
 * the TofuTrustManager parks the connection here by creating a
 * CompletableDeferred and suspending. The UI observes [pendingRequests]
 * and presents a confirmation dialog showing the certificate fingerprint.
 * Once the user taps Accept or Reject, [resolve] completes the deferred
 * and the TLS handshake either proceeds or throws CertificateException.
 *
 * Security: Without user confirmation, the first-connection MITM window
 * on hostile LANs (T1557 / OWASP M5) is eliminated.
 */
object TofuPendingManager {

    // IP address -> (hex fingerprint, user-decision deferred)
    private val pending = ConcurrentHashMap<String, Pair<String, CompletableDeferred<Boolean>>>()

    /**
     * Called by TofuTrustManager on a background IO thread.
     * Returns a CompletableDeferred that the caller must await.
     * The deferred completes true (accept) or false (reject) when
     * the user responds in the UI.
     */
    fun requestConfirmation(ip: String, fingerprint: String): CompletableDeferred<Boolean> {
        // If there's already a pending request for this IP (parallel connections),
        // reuse the existing deferred so the user only sees one dialog.
        pending[ip]?.let { (existingFp, existingDeferred) ->
            if (existingFp == fingerprint) return existingDeferred
        }
        val deferred = CompletableDeferred<Boolean>()
        pending[ip] = Pair(fingerprint, deferred)
        return deferred
    }

    /**
     * Returns a snapshot of all currently pending confirmations.
     * Observed by the UI to display dialogs.
     */
    fun getPendingRequests(): Map<String, String> =
        pending.mapValues { it.value.first }

    /**
     * Called by the UI when the user taps Accept (accepted=true) or Reject.
     */
    fun resolve(ip: String, accepted: Boolean) {
        pending.remove(ip)?.second?.complete(accepted)
    }

    /** True if there are any pending confirmations waiting for the user. */
    fun hasPending(): Boolean = pending.isNotEmpty()
}
