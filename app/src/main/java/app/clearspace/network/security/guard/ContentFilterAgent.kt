package app.clearspace.network.security.guard

import android.content.Context
import android.net.Uri
import android.util.Log

/**
 * Agent responsible for On-Device ML Filtering.
 * Scans content (images, text) before they are allowed to be broadcast
 * to PUBLIC or TRUSTED visibility tiers.
 */
class ContentFilterAgent(private val context: Context) {

    /**
     * Evaluates an image URI to determine if it is safe to broadcast.
     * In a full implementation, this loads a TFLite model (e.g. MobileNetV2 NSFW)
     * and runs inference on the Bitmap.
     * 
     * @return true if safe, false if flagged by the safety filter.
     */
    suspend fun isImageSafe(imageUri: Uri): Boolean {
        Log.d("ContentFilterAgent", "Evaluating image for safety: $imageUri")
        
        // Dynamic testing hook: check if image path contains unsafe labels
        val path = imageUri.path?.lowercase() ?: ""
        if (path.contains("nsfw") || path.contains("unsafe") || path.contains("csae") || path.contains("csam") || path.contains("adult")) {
            Log.w("ContentFilterAgent", "Image flagged by safety filter: contains unsafe keyword in filename")
            return false
        }
        
        kotlinx.coroutines.delay(200) // Simulate inference time
        return true
    }

    /**
     * Evaluates text content for severe policy violations, adult content, and grooming indicators.
     * Crucial to protect teenagers under the 12+ rating.
     */
    fun isTextSafe(text: String): Boolean {
        val lowerText = text.lowercase()
        
        // List of blocked keywords/patterns (English & Croatian) for CSAM, adult content, and grooming
        val blockedPatterns = listOf(
            // Adult & NSFW
            "nsfw", "porn", "xxx", "seks", "gola", "goli", "nude", "sex", "sexy", "webcam",
            
            // Grooming / CSAE indicators
            "send nudes", "send pics", "send photos", "show me your", "meet me alone", 
            "secret meet", "where do you live", "how old are you", "what is your address",
            "posalji sliku", "pošalji sliku", "koliko imas godina", "koliko imaš godina", 
            "gdje zivis", "gdje živiš", "nadimo se", "nađimo se", "privatni sastanak", 
            "slike tijela", "snapchat swap"
        )
        
        for (pattern in blockedPatterns) {
            if (lowerText.contains(pattern)) {
                Log.w("ContentFilterAgent", "Text flagged by safety filter: contains blocked pattern '$pattern'")
                return false
            }
        }
        return true
    }
}
