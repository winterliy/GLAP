package dev.glap

import net.minecraft.SharedConstants
import com.mojang.serialization.Dynamic
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.util.datafix.DataFixers
import net.minecraft.util.datafix.fixes.BlockStateData
import net.minecraft.util.datafix.fixes.EntityBlockStateFix
import net.minecraft.util.datafix.fixes.References
import java.util.concurrent.ConcurrentHashMap

/** Rewrites supported legacy command forms before the current Brigadier tree parses them. */
object LegacyCommandCompatibility {
    private val legacyBlockStates = ConcurrentHashMap<Int, String>()
    private val legacyEntityTypes = ConcurrentHashMap<String, String>()
    private val legacyEffectIds = listOf(
        "speed", "slowness", "haste", "mining_fatigue", "strength", "instant_health", "instant_damage",
        "jump_boost", "nausea", "regeneration", "resistance", "fire_resistance", "water_breathing",
        "invisibility", "blindness", "night_vision", "hunger", "weakness", "poison", "wither", "health_boost",
        "absorption", "saturation", "glowing", "levitation", "luck", "unluck"
    )
    private val legacyEnchantments = mapOf(
        0 to "protection", 1 to "fire_protection", 2 to "feather_falling", 3 to "blast_protection",
        4 to "projectile_protection", 5 to "respiration", 6 to "aqua_affinity", 7 to "thorns",
        8 to "depth_strider", 9 to "frost_walker", 10 to "binding_curse", 16 to "sharpness",
        17 to "smite", 18 to "bane_of_arthropods", 19 to "knockback", 20 to "fire_aspect",
        21 to "looting", 22 to "sweeping_edge", 32 to "efficiency", 33 to "silk_touch",
        34 to "unbreaking", 35 to "fortune", 48 to "power", 49 to "punch", 50 to "flame",
        51 to "infinity", 61 to "luck_of_the_sea", 62 to "lure", 70 to "mending", 71 to "vanishing_curse"
    )
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
                val target = tokens.getOrNull(2)?.value?.let { translateLegacySelector(it) ?: return null }
                    ?: if (sourceHasEntity) "@s" else return "experience add"
                "experience add $target $amount ${if (levels) "levels" else "points"}"
            }
            "kill" -> {
                if (tokens.size != 2 || !tokens[1].value.startsWith('@')) return null
                val original = tokens[1].value
                val target = translateLegacySelector(original) ?: return null
                if (target == original) return null
                "kill $target"
            }
            "difficulty" -> {
                if (tokens.size != 2) return null
                val raw = tokens[1].value
                val difficulty = when (raw.lowercase()) {
                    "0", "p", "peaceful" -> "peaceful"
                    "1", "e", "easy" -> "easy"
                    "2", "n", "normal" -> "normal"
                    "3", "h", "hard" -> "hard"
                    else -> return null
                }
                if (raw == difficulty) return null
                "difficulty $difficulty"
            }
            "gamemode", "defaultgamemode" -> {
                if (tokens.size !in 2..(if (root == "gamemode") 3 else 2)) return null
                val mode = when (tokens[1].value) {
                    "0", "s" -> "survival"
                    "1", "c" -> "creative"
                    "2", "a" -> "adventure"
                    "3", "sp" -> "spectator"
                    else -> return null
                }
                "$root $mode" + (tokens.getOrNull(2)?.let { " ${it.value}" } ?: "")
            }
            "enchant" -> {
                if (tokens.size !in 3..4) return null
                val rawEnchantment = tokens[2].value
                val enchantment = if (rawEnchantment == "sweeping" || rawEnchantment == "minecraft:sweeping") {
                    "sweeping_edge"
                } else {
                    legacyEnchantments[rawEnchantment.toIntOrNull() ?: return null] ?: return null
                }
                val level = tokens.getOrNull(3)?.value
                if (level != null && level.toIntOrNull() == null) return null
                "enchant ${tokens[1].value} minecraft:$enchantment" + (level?.let { " $it" } ?: "")
            }
            "effect" -> translateEffect(tokens) ?: return null
            "replaceitem" -> translateReplaceItem(tokens) ?: return null
            "execute" -> translateExecute(tokens) ?: return null
            else -> return null
        }
        return if (hasPrefix) "/$rewritten" else rewritten
    }

    private fun String.isSnbtCompound() = startsWith('{') && endsWith('}')

    private fun translateExecute(tokens: List<Token>): String? {
        if (tokens.size < 6) return null
        val originalTarget = tokens[1].value
        if (!originalTarget.matches(Regex("@[pares](?:\\[.*])?|[A-Za-z0-9_]{1,16}|[0-9a-fA-F]{8}-(?:[0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}"))) return null
        val target = translateLegacySelector(originalTarget) ?: return null
        val positionedCoordinates = tokens.subList(2, 5).mapIndexed { index, token ->
            normalizeLegacyCoordinate(token.value, centerCorrect = index != 1) ?: return null
        }
        var commandStart = 5
        var condition = ""
        if (tokens[5].value == "detect") {
            if (tokens.size < 12) return null
            val detectCoordinates = tokens.subList(6, 9).map { normalizeLegacyBlockCoordinate(it.value) ?: return null }
            val block = tokens[9].value
            val metadata = tokens[10].value
            condition = when {
                metadata == "-1" || metadata == "*" -> {
                    if (block in setOf("bedrock", "minecraft:bedrock")) {
                        " if block ${detectCoordinates.joinToString(" ")} $block"
                    } else {
                        val direct = translateLegacyWildcardBlock(block, detectCoordinates)
                        if (direct != null) {
                            " if predicate $direct"
                        } else {
                            val detectPosition = tokens.subList(6, 9).mapIndexed { index, token ->
                                normalizeLegacyCoordinate(token.value, centerCorrect = index != 1) ?: return null
                            }
                            val predicate = translateLegacyWildcardBlock(block, listOf("~", "~", "~")) ?: return null
                            " positioned ${detectPosition.joinToString(" ")} if predicate $predicate at @s positioned ${positionedCoordinates.joinToString(" ")}"
                        }
                    }
                }
                metadata.toIntOrNull() in 0..15 -> {
                    val mappedBlock = translateLegacyBlockState(block, metadata.toInt()) ?: return null
                    " if block ${detectCoordinates.joinToString(" ")} $mappedBlock"
                }
                else -> return null
            }
            commandStart = 11
        }
        val nested = tokens.drop(commandStart).joinToString(" ") { it.value }.removePrefix("/")
        val translated = translate(nested, sourceHasEntity = true) ?: nested
        return "execute as $target at @s positioned ${positionedCoordinates.joinToString(" ")}$condition run $translated"
    }

    private fun normalizeLegacyCoordinate(coordinate: String, centerCorrect: Boolean): String? {
        val relative = coordinate.startsWith('~')
        val number = if (relative) coordinate.drop(1) else coordinate
        if (relative && number.isEmpty()) return "~"
        if (!number.matches(Regex("[+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)"))) return null
        var normalized = number.removePrefix("+")
        if (!relative && centerCorrect && '.' !in normalized) normalized += ".0"
        return (if (relative) "~" else "") + normalized
    }

    private fun normalizeLegacyBlockCoordinate(coordinate: String): String? {
        val normalized = normalizeLegacyCoordinate(coordinate, centerCorrect = false) ?: return null
        if (normalized.startsWith('~')) return normalized
        val number = normalized.toDoubleOrNull() ?: return null
        if (!number.isFinite() || number < Int.MIN_VALUE || number > Int.MAX_VALUE) return null
        return kotlin.math.floor(number).toInt().toString()
    }

    private fun translateLegacyWildcardBlock(block: String, coordinates: List<String>): String? {
        val offsets = coordinates.map { coordinate ->
            if (!coordinate.startsWith('~')) return null
            val offset = coordinate.drop(1)
            if (offset.isEmpty()) 0 else offset.toIntOrNull() ?: return null
        }
        val states = (0..15).map { metadata -> translateLegacyBlockState(block, metadata) ?: return null }.distinct()
        val prefix = "type:\"minecraft:location_check\",offsetX:${offsets[0]},offsetY:${offsets[1]},offsetZ:${offsets[2]}"
        if (states.none { '[' in it }) {
            val names = states.sorted().joinToString(",") { "\"$it\"" }
            return "{$prefix,predicate:{block:{blocks:[$names]}}}"
        }
        val terms = states.sorted().joinToString(",") { state ->
            val name = state.substringBefore('[')
            val properties = state.substringAfter('[', "").removeSuffix("]")
            val stateField = if (properties.isEmpty()) "" else properties.split(',').joinToString(",", ",state:{", "}") {
                "${it.substringBefore('=')}:\"${it.substringAfter('=')}\""
            }
            "{$prefix,predicate:{block:{blocks:[\"$name\"]$stateField}}}"
        }
        return "{type:\"minecraft:any_of\",terms:[$terms]}"
    }

    private fun translateLegacyBlockState(block: String, metadata: Int): String? {
        val oldName = if (':' in block) block else "minecraft:$block"
        val id = block.toIntOrNull() ?: EntityBlockStateFix.getBlockId(oldName)
        if (id !in 0..255 || (id == 0 && oldName != "minecraft:air" && block != "0")) return null
        val state = (id shl 4) or metadata
        legacyBlockStates[state]?.let { return it }
        val mapped = convertLegacyBlockState(id, state) ?: return null
        return legacyBlockStates.putIfAbsent(state, mapped) ?: mapped
    }

    private fun convertLegacyBlockState(id: Int, legacyState: Int): String? {
        val flattened = BlockStateData.getTag(legacyState)
        SharedConstants.tryDetectVersion()
        val updated = DataFixers.getDataFixer().update(
            References.BLOCK_STATE, flattened, 1451, SharedConstants.getCurrentVersion().dataVersion().version()
        )
        val name = updated.get("id").asString("")
        if (name.isEmpty() || (id != 0 && name == "minecraft:air")) return null
        val values = updated.get("properties").result().orElse(null)?.asMapOpt()?.result()?.orElse(null)
            // These states are not encoded in 1.12 block metadata and must not narrow a legacy match.
            ?.filter { entry -> entry.first.asString("") !in setOf("snowy", "waterlogged") }
            ?.map { entry -> "${entry.first.asString("")}=${entry.second.asString("")}" }?.sorted()?.toList() ?: emptyList()
        if (values.isEmpty()) return name
        return "$name${values.joinToString(",", "[", "]")}"
    }

    private fun translateLegacySelector(target: String): String? {
        if (!target.startsWith('@') || !target.contains('[')) return target
        val contents = target.substringAfter('[').dropLast(1)
        if (contents.any { it in "\"'{}[]" }) return target
        val arguments = contents.split(',')
        val converted = mutableListOf<String>()
        val legacyKeys = mutableSetOf("rm", "r", "lm", "l", "m", "c")
        var selector = target.substringBefore('[')
        for ((minimumKey, maximumKey, destination) in listOf(Triple("rm", "r", "distance"), Triple("lm", "l", "level"))) {
            val minimum = arguments.filter { it.substringBefore('=') == minimumKey }
            val maximum = arguments.filter { it.substringBefore('=') == maximumKey }
            if (minimum.isEmpty() && maximum.isEmpty()) continue
            if (minimum.size > 1 || maximum.size > 1 || arguments.any { it.substringBefore('=') == destination }) return null
            val low = minimum.singleOrNull()?.substringAfter('=', "") ?: ""
            val high = maximum.singleOrNull()?.substringAfter('=', "") ?: ""
            if ((minimum.isNotEmpty() && !low.matches(Regex("\\d+"))) || (maximum.isNotEmpty() && !high.matches(Regex("\\d+")))) return null
            converted += "$destination=$low..$high"
        }
        val modes = arguments.filter { it.substringBefore('=') == "m" }
        if (modes.isNotEmpty()) {
            if (modes.size != 1 || arguments.any { it.substringBefore('=') == "gamemode" }) return null
            val rawMode = modes.single().substringAfter('=', "")
            val negated = rawMode.startsWith('!')
            val mode = when (rawMode.removePrefix("!")) {
                "0", "s", "survival" -> "survival"
                "1", "c", "creative" -> "creative"
                "2", "a", "adventure" -> "adventure"
                "3", "sp", "spectator" -> "spectator"
                else -> return null
            }
            converted += "gamemode=${if (negated) "!" else ""}$mode"
        }
        val counts = arguments.filter { it.substringBefore('=') == "c" }
        if (counts.isNotEmpty()) {
            if (counts.size != 1 || arguments.any { it.substringBefore('=') in setOf("sort", "limit") }) return null
            val count = counts.single().substringAfter('=', "").toIntOrNull() ?: return null
            if (count == Int.MIN_VALUE) return null
            // In 1.12, c=1 gives the source entity priority even when it is not nearest.
            if (count == 1 && selector != "@r" && selector != "@s") return null
            if (selector != "@s") {
                val random = selector == "@r"
                if (count == 0 && selector == "@p") selector = "@a"
                if (random && (count == 0 || arguments.any { it.substringBefore('=') == "type" })) {
                    selector = if (arguments.any { it.substringBefore('=') == "type" }) "@e" else "@a"
                }
                converted += "sort=${if (random) "random" else if (count < 0) "furthest" else "nearest"}"
                if (count != 0) converted += "limit=${kotlin.math.abs(count)}"
            }
        }
        val entityTypes = arguments.filter { it.substringBefore('=') == "type" }
        if (entityTypes.size == 1) {
            val rawType = entityTypes.single().substringAfter('=', "")
            val negated = rawType.startsWith('!')
            val entityType = rawType.removePrefix("!")
            if (entityType.matches(Regex("[A-Za-z0-9_.:-]+"))) {
                val updatedType = translateLegacyEntityType(entityType) ?: return null
                if (updatedType != entityType && updatedType != "minecraft:$entityType") {
                    converted += "type=${if (negated) "!" else ""}$updatedType"
                    legacyKeys += "type"
                }
            }
        }
        val scoreArguments = arguments.filter { it.substringBefore('=').startsWith("score_") }
        if (scoreArguments.isNotEmpty()) {
            if (arguments.any { it.substringBefore('=') == "scores" }) return null
            val scores = sortedMapOf<String, Pair<Int?, Int?>>()
            for (argument in scoreArguments) {
                val key = argument.substringBefore('=').removePrefix("score_")
                val minimum = key.endsWith("_min") && key.length > 4
                val objective = if (minimum) key.removeSuffix("_min") else key
                if (!objective.matches(Regex("[A-Za-z0-9_.+-]+"))) return null
                val value = argument.substringAfter('=', "").toIntOrNull() ?: return null
                val bounds = scores[objective] ?: (null to null)
                if ((minimum && bounds.first != null) || (!minimum && bounds.second != null)) return null
                scores[objective] = if (minimum) value to bounds.second else bounds.first to value
            }
            converted += scores.entries.joinToString(",", "scores={", "}") { (objective, bounds) ->
                "$objective=${bounds.first ?: ""}..${bounds.second ?: ""}"
            }
        }
        if (converted.isEmpty() && counts.isEmpty()) return target
        val remaining = arguments.filterNot { it.substringBefore('=') in legacyKeys || it.substringBefore('=').startsWith("score_") }.filter { it.isNotEmpty() }
        val output = remaining + converted
        return selector + if (output.isEmpty()) "" else output.joinToString(",", "[", "]")
    }

    private fun translateLegacyEntityType(entityType: String): String? {
        // These old IDs represented variants that became distinct modern entity types.
        when (entityType) {
            "Zombie" -> return "#minecraft:glap/legacy_zombie"
            "Skeleton" -> return "#minecraft:glap/legacy_skeleton"
            "EntityHorse" -> return "#minecraft:glap/legacy_horse"
            "Guardian" -> return "#minecraft:glap/legacy_guardian"
        }
        legacyEntityTypes[entityType]?.let { return it }
        SharedConstants.tryDetectVersion()
        val tag = CompoundTag()
        tag.putString("id", entityType)
        val version = if (entityType.any(Char::isUpperCase)) 704 else 1343
        val updated = DataFixers.getDataFixer().update(
            References.ENTITY, Dynamic(NbtOps.INSTANCE, tag), version,
            SharedConstants.getCurrentVersion().dataVersion().version()
        )
        val mapped = updated.get("id").asString(entityType)
        if (legacyEntityTypes.size < 1024) legacyEntityTypes.putIfAbsent(entityType, mapped)
        return mapped
    }

    private fun translateEffect(tokens: List<Token>): String? {
        if (tokens.size >= 3 && tokens[2].value == "clear") {
            val target = translateLegacySelector(tokens[1].value) ?: return null
            return "effect clear $target"
        }
        if (tokens.size < 3) return null

        val target = translateLegacySelector(tokens[1].value) ?: return null
        val rawEffect = tokens[2].value
        val effect = if (rawEffect.matches(Regex("\\d+"))) {
            val id = rawEffect.toIntOrNull() ?: return null
            "minecraft:${legacyEffectIds.getOrNull(id - 1) ?: return null}"
        } else rawEffect

        val duration = tokens.getOrNull(3)?.value?.toIntOrNull()
        if (tokens.size >= 4 && duration == null) return null
        if (duration != null && duration < 0 && duration != -1) return null
        if (duration != null && duration > 1_000_000) return null

        val amplifier = tokens.getOrNull(4)?.value?.toIntOrNull()
        if (tokens.size >= 5 && amplifier !in 0..255) return null
        val hideParticles = tokens.getOrNull(5)?.value?.equals("true", ignoreCase = true)?.toString()

        if (duration == 0) return "effect clear $target $effect"

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
