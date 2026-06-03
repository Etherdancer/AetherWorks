package org.example.aetherworks.ui.trust

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.example.aetherworks.crypto.KeyManager

@Composable
fun RemoteLinkExchangeScreen(modifier: Modifier = Modifier, onBack: () -> Unit) {
    val context = LocalContext.current
    val keyManager = remember { KeyManager(context) }
    
    // Generate a one-time rendezvous token
    val rendezvousToken = remember { java.util.UUID.randomUUID().toString() }
    val pubKeyFingerprint = remember { "fingerprint-placeholder" } // TODO: actual Ed25519 fingerprint
    
    val sigBase64 = remember {
        val sig = keyManager.signData(rendezvousToken.toByteArray(Charsets.UTF_8))
        android.util.Base64.encodeToString(sig, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
    }
    
    val shareLink = "aetherworks://rendezvous?pk=$pubKeyFingerprint&token=$rendezvousToken&sig=$sigBase64"

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Remote Trust Verification", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Text(
                "WARNING: Remote link exchange is vulnerable to Man-In-The-Middle attacks if the channel you use to send the link is compromised. Send this link only through secure, end-to-end encrypted messaging.",
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Step 1: Send Your Link", style = MaterialTheme.typography.titleMedium)
        Text("Send this link to the person you want to trust. They must open it in AetherWorks.", modifier = Modifier.padding(top = 4.dp))
        
        OutlinedTextField(
            value = shareLink,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        
        Button(
            onClick = {
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, shareLink)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, null)
                context.startActivity(shareIntent)
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Share Link")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Step 2: Wait for Connection", style = MaterialTheme.typography.titleMedium)
        Text("Once they open your link, AetherWorks will establish a secure connection using Tor hidden services to complete the handshake.", modifier = Modifier.padding(top = 4.dp))
        
        CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel")
        }
    }
}
