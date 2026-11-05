package dev.glap

/** Rewrites supported legacy command forms before the current Brigadier tree parses them. */
object LegacyCommandCompatibility {
    /** Returns a current command string when [input] uses a supported legacy form. */
    @JvmStatic
    fun translate(input: String): String? {
        val hasPrefix = input.startsWith('/')
        val command = if (hasPrefix) input.substring(1) else input
        val rootEnd = command.indexOfFirst(Char::isWhitespace).let { if (it < 0) command.length else it }
        if (command.substring(0, rootEnd) != "blockdata") return null

        val tokens = tokenize(command) ?: return null
        if (tokens.size != 5 || !tokens[4].value.startsWith('{') || !tokens[4].value.endsWith('}')) return null

        val rewritten = "data merge block ${tokens[1].value} ${tokens[2].value} ${tokens[3].value} ${tokens[4].value}"
        return if (hasPrefix) "/$rewritten" else rewritten
    }

    private data class Token(val value: String)

    /** Splits only at whitespace outside quotes and balanced SNBT/selector delimiters. */
    private fun tokenize(input: String): List<Token>? {
        val tokens = mutableListOf<Token>()
        var cursor = 0

        while (cursor < input.length) {
            while (cursor < input.length && input[cursor].isWhitespace()) cursor++
            if (cursor == input.length) break

            val start = cursor
            val delimiters = ArrayDeque<Char>()
            var quote: Char? = null
            var escaped = false

            while (cursor < input.length) {
                val character = input[cursor]
                if (quote != null) {
                    when {
                        escaped -> escaped = false
                        character == '\\' -> escaped = true
                        character == quote -> quote = null
                    }
                } else {
                    when (character) {
                        '"', '\'' -> quote = character
                        '{', '[' -> delimiters.addLast(character)
                        '}', ']' -> {
                            val expected = if (character == '}') '{' else '['
                            if (delimiters.lastOrNull() != expected) return null
                            delimiters.removeLast()
                        }
                        else -> if (character.isWhitespace() && delimiters.isEmpty()) break
                    }
                }
                cursor++
            }

            if (quote != null || delimiters.isNotEmpty()) return null
            tokens += Token(input.substring(start, cursor))
        }

        return tokens
    }
}
