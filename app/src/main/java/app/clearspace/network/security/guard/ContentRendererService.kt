package app.clearspace.network.security.guard

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

/**
 * Shelter-Style Content Renderer (Isolated Process)
 * This Service is designed to be declared with `android:isolatedProcess="true"`
 * in the AndroidManifest.xml. It runs in a highly restricted sandbox with no
 * access to the rest of the application's memory, file system, or network.
 * 
 * It is used solely to parse and render potentially malicious content (like 
 * complex markdown or untrusted images) safely.
 */
import app.clearspace.network.IContentRenderer

class ContentRendererService : Service() {

    private val binder = object : IContentRenderer.Stub() {
        override fun renderMarkdownToHtml(rawMarkdown: String?): String {
            if (rawMarkdown == null) return ""
            
            // 1. Sanitize HTML entities to prevent XSS
            var html = rawMarkdown
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")

            // 2. Parse Markdown
            html = html.replace(Regex("\\*\\*(.*?)\\*\\*")) { "<b>${it.groupValues[1]}</b>" }
            html = html.replace(Regex("\\*(.*?)\\*")) { "<i>${it.groupValues[1]}</i>" }
            html = html.replace(Regex("^# (.*?)$", RegexOption.MULTILINE)) { "<h1>${it.groupValues[1]}</h1>" }
            html = html.replace(Regex("^## (.*?)$", RegexOption.MULTILINE)) { "<h2>${it.groupValues[1]}</h2>" }
            // FIX C1: Allowlist href schemes — only https:// and aetherworks:// are permitted.
            // javascript:, intent:, file:, data: and all other schemes are replaced with "#"
            html = html.replace(Regex("\\[(.*?)\\]\\((.*?)\\)")) {
                val linkText = it.groupValues[1]
                val rawUrl   = it.groupValues[2].trim()
                val safeUrl  = if (rawUrl.startsWith("https://") || rawUrl.startsWith("aetherworks://")) rawUrl else "#"
                "<a href=\"$safeUrl\">$linkText</a>"
            }
            // FIX C1: URL-encode wiki-link titles to prevent deep link parameter injection
            html = html.replace(Regex("\\[\\[(.*?)\\]\\]")) {
                val title   = it.groupValues[1]
                val encoded = java.net.URLEncoder.encode(title, "UTF-8")
                "<a href=\"aetherworks://content?title=$encoded\">$title</a>"
            }
            html = html.replace("\n", "<br/>")


            // 3. Wrap in a basic stylesheet for WebView
            val style = "body { font-family: sans-serif; padding: 16px; font-size: 16px; color: #333333; background-color: #f9f9f9; }"
            return "<html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"><style>$style</style></head><body>$html</body></html>"
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "ContentRendererService bound in isolated process.")
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "ContentRendererService created in isolated sandbox.")
    }

    companion object {
        private const val TAG = "ContentRenderer"
    }
}
