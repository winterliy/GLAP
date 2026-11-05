package dev.glap

/** Rewrites supported legacy command forms before the current Brigadier tree parses them. */
object LegacyCommandCompatibility {
    /** Returns a current command string when [input] uses a supported legacy form. */
    @JvmStatic
    fun translate(input: String, sourceHasEntity: Boolean = false): String? {
        val hasPrefix = input.startsWith('/')
        val command = if (hasPrefix) input.substring(1) else input
        val rootEnd = command.indexOfFirst(Char::isWhitespace).let { if (it < 0) command.length else it }
        val root = command.substring(0, rootEnd)
        val tokens = tokenize(command) ?: return null
        val rewritten = when (root) {
            "blockdata" -> {
                if (tokens.size != 5 || !tokens[4].value.isSnbtCompound()) return null
                "data merge block ${tokens[1].value} ${tokens[2].value} ${tokens[3].value} ${tokens[4].value}"
            }
            "entitydata" -> {
                if (tokens.size != 3 || !tokens[2].value.isSnbtCompound()) return null
                "data merge entity ${tokens[1].value} ${tokens[2].value}"
            }
            "xp" -> {
                if (tokens.size !in 2..3) return null
                val legacyAmount = tokens[1].value
                val levels = legacyAmount.endsWith('L', ignoreCase = true)
                val amount = if (levels) legacyAmount.dropLast(1) else legacyAmount
                if (!amount.matches(Regex("-?\\d+"))) return null
                val target = tokens.getOrNull(2)?.value ?: if (sourceHasEntity) "@s" else return "experience add"
                "experience add $target $amount ${if (levels) "levels" else "points"}"
            }
            else -> return null
        }
        return if (hasPrefix) "/$rewritten" else rewritten
    }

    private fun String.isSnbtCompound() = startsWith('{') && endsWith('}')

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
