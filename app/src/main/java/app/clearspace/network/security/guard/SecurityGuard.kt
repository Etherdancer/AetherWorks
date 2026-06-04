package app.clearspace.network.security.guard

/**
 * SecurityGuard
 * Central utility for sanitizing all user inputs and incoming P2P data.
 * Protects against XSS, SQLi (as defense-in-depth), and buffer exhaustion.
 */
object SecurityGuard {

    private const val MAX_STRING_LENGTH = 10000 // Prevent memory bombs

    /**
     * Sanitizes plain text input by stripping HTML/XML tags,
     * removing control characters, and truncating to a safe length.
     */
    fun sanitizeText(input: String, maxLength: Int = MAX_STRING_LENGTH): String {
        if (input.isBlank()) return ""

        // 1. Truncate
        var safeInput = if (input.length > maxLength) input.substring(0, maxLength) else input

        // 2. Strip HTML/XML tags using a strict regex
        safeInput = safeInput.replace(Regex("<[^>]*>"), "")

        // 3. Remove non-printable control characters (except common formatting like \n \t)
        safeInput = safeInput.replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]"), "")

        return safeInput.trim()
    }

    /**
     * Validates and sanitizes a username/alias.
     * Stricter rules: Alphanumeric and basic punctuation only.
     */
    fun sanitizeAlias(input: String): String {
        val maxAliasLength = 50
        val sanitized = sanitizeText(input, maxAliasLength)
        // Allow only letters, numbers, spaces, and basic safe symbols
        return sanitized.replace(Regex("[^a-zA-Z0-9 \\-_.]"), "").trim()
    }

    /**
     * Defense-in-depth: Checks if an input looks like a SQL injection payload.
     * Note: Room uses parameterized queries, so SQLi is already mitigated. This is just an extra log/drop mechanism for P2P data.
     */
    fun containsSqlInjectionAttempt(input: String): Boolean {
        val sqliRegex = Regex("(?i)(.*)(drop table|insert into|delete from|update .* set|union all|select .* from)(.*)")
        return sqliRegex.matches(input)
    }
}
