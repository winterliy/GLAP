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
            "effect" -> translateEffect(tokens) ?: return null
            "replaceitem" -> translateReplaceItem(tokens) ?: return null
            else -> return null
        }
        return if (hasPrefix) "/$rewritten" else rewritten
    }

    private fun String.isSnbtCompound() = startsWith('{') && endsWith('}')

    private fun translateEffect(tokens: List<Token>): String? {
        if (tokens.size == 3 && tokens[2].value == "clear") return "effect clear ${tokens[1].value}"
        if (tokens.size !in 3..6) return null

        val target = tokens[1].value
        val effect = tokens[2].value
        if (effect.matches(Regex("\\d+"))) return null

        val duration = tokens.getOrNull(3)?.value?.toIntOrNull()
        if (tokens.size >= 4 && duration == null) return null
        if (duration == 0) return if (tokens.size == 4) "effect clear $target $effect" else null
        if (duration != null && duration < 0 && duration != -1) return null
        if (duration != null && duration > 1_000_000) return null

        val amplifier = tokens.getOrNull(4)?.value?.toIntOrNull()
        if (tokens.size >= 5 && amplifier !in 0..255) return null
        val hideParticles = tokens.getOrNull(5)?.value?.lowercase()
        if (tokens.size == 6 && hideParticles !in setOf("true", "false")) return null

        val parts = mutableListOf("effect", "give", target, effect)
        if (duration == -1) parts += "infinite" else if (duration != null) parts += duration.toString()
        if (amplifier != null) parts += amplifier.toString()
        if (hideParticles != null) parts += hideParticles
        return parts.joinToString(" ")
    }

    private fun translateReplaceItem(tokens: List<Token>): String? {
        val mode = tokens.getOrNull(1)?.value ?: return null
        val slotIndex: Int
        val itemIndex: Int
        val output = mutableListOf("item", "replace")

        when (mode) {
            "entity" -> {
                if (tokens.size !in 5..6) return null
                output += listOf("entity", tokens[2].value)
                slotIndex = 3
                itemIndex = 4
            }
            "block" -> {
                if (tokens.size !in 7..8) return null
                output += listOf("block", tokens[2].value, tokens[3].value, tokens[4].value)
                slotIndex = 5
                itemIndex = 6
            }
            else -> return null
        }

        val legacySlot = tokens[slotIndex].value
        if (!legacySlot.startsWith("slot.")) return null
        val slot = legacySlot.removePrefix("slot.")
        if (!slot.isSupportedLegacySlot()) return null

        val item = tokens[itemIndex].value
        if (item.matches(Regex("\\d+")) || !item.matches(Regex("(?:[a-z0-9_.-]+:)?[a-z0-9_./-]+"))) return null

        val count = tokens.getOrNull(itemIndex + 1)?.value?.toIntOrNull()
        if (itemIndex + 1 < tokens.size && count !in 1..99) return null

        output += listOf(slot, "with", item)
        if (count != null) output += count.toString()
        return output.joinToString(" ")
    }

    private fun String.isSupportedLegacySlot(): Boolean = when {
        this in setOf("weapon", "weapon.mainhand", "weapon.offhand", "armor.head", "armor.chest", "armor.legs", "armor.feet") -> true
        startsWith("hotbar.") -> removePrefix("hotbar.").toIntOrNull() in 0..8
        startsWith("inventory.") -> removePrefix("inventory.").toIntOrNull() in 0..26
        startsWith("enderchest.") -> removePrefix("enderchest.").toIntOrNull() in 0..26
        startsWith("container.") -> removePrefix("container.").toIntOrNull() in 0..53
        else -> false
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
