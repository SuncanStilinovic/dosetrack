package com.dosetrack.export

/** Rules for a new backup password. Existing backups are always opened with whatever password they have. */
object PasswordPolicy {

    const val MIN_LENGTH = 12

    data class Rule(val description: String, val isMet: (String) -> Boolean)

    val rules: List<Rule> = listOf(
        Rule("At least $MIN_LENGTH characters") { it.length >= MIN_LENGTH },
        Rule("At least one capital letter") { pw -> pw.any { it.isUpperCase() } },
        Rule("At least one number") { pw -> pw.any { it.isDigit() } },
        Rule("At least one special character (e.g. ! ? # @ -)") { pw -> pw.any(::isSpecial) },
    )

    fun isAcceptable(password: String): Boolean = rules.all { it.isMet(password) }

    /** Anything visible that isn't a letter or digit: punctuation, symbols, emoji. */
    private fun isSpecial(c: Char) = !c.isLetterOrDigit() && !c.isWhitespace()
}
