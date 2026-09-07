package com.safeshade.platform

/**
 * A stranger-danger quiet word is a phrase the wearer can say or type that
 * sounds ordinary in conversation but tells the guardian something is wrong —
 * "is the pizza still on the counter" rather than a scream or a panic button
 * that would tip off whoever is standing next to them.
 *
 * This file is matching only: given a candidate word and a body of text (a
 * typed message, or a transcript handed in from wherever speech-to-text runs),
 * it decides whether the word was said and can redact it from a transcript
 * before it is stored or shown elsewhere. It does no speech recognition of its
 * own.
 */
class QuietWordMatcher(word: String) {

    private val normalizedWord = normalize(word)
    private val wordTokenCount = normalizedWord.split(' ').filter { it.isNotEmpty() }.size

    /** True when the quiet word appears as a whole word or phrase in [text]. */
    fun matches(text: String): Boolean {
        if (normalizedWord.isEmpty()) return false
        val tokens = normalize(text).split(' ').filter { it.isNotEmpty() }
        if (tokens.size < wordTokenCount) return false
        val wordTokens = normalizedWord.split(' ').filter { it.isNotEmpty() }
        for (start in 0..tokens.size - wordTokenCount) {
            if (tokens.subList(start, start + wordTokenCount) == wordTokens) return true
        }
        return false
    }

    /** Replaces every occurrence of the quiet word in [text] with "•••". */
    fun redact(text: String): String {
        if (normalizedWord.isEmpty()) return text
        val pattern = Regex(
            "(?i)\\b" + Regex.escape(normalizedWord).replace(" ", "\\s+") + "\\b",
        )
        return pattern.replace(text, "•••")
    }

    private companion object {
        fun normalize(input: String): String =
            input.trim()
                .lowercase()
                .filter { it.isLetterOrDigit() || it.isWhitespace() }
                .replace(Regex("\\s+"), " ")
                .trim()
    }
}

sealed interface QuietWordValidation {
    data object Ok : QuietWordValidation
    data object TooShort : QuietWordValidation
    data object TooCommon : QuietWordValidation
    data object ContainsDigitsOnly : QuietWordValidation
}

object QuietWord {

    private val commonWords = setOf(
        "help", "ok", "yes", "no", "hi", "mum", "dad", "please", "stop",
    )

    /** Validates a candidate quiet word before it is saved to settings. */
    fun validate(word: String): QuietWordValidation {
        val trimmed = word.trim().lowercase()
        val letters = trimmed.filter { it.isLetter() }

        return when {
            trimmed.isNotEmpty() && trimmed.all { it.isDigit() || it.isWhitespace() } -> QuietWordValidation.ContainsDigitsOnly
            letters.length < 3 -> QuietWordValidation.TooShort
            commonWords.contains(trimmed) -> QuietWordValidation.TooCommon
            else -> QuietWordValidation.Ok
        }
    }
}
