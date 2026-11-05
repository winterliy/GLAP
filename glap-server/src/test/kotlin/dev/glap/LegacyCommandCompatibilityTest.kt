package dev.glap

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LegacyCommandCompatibilityTest {
    @Test
    fun `rewrites blockdata into current data merge syntax`() {
        assertEquals(
            "data merge block 10 64 -3 {CustomName:\"A B\",Items:[{id:\"minecraft:stone\",Count:1b}]}",
            LegacyCommandCompatibility.translate("blockdata 10 64 -3 {CustomName:\"A B\",Items:[{id:\"minecraft:stone\",Count:1b}]}")
        )
    }

    @Test
    fun `preserves optional slash and nested SNBT with quoted whitespace`() {
        assertEquals(
            "/data merge block ~ ~1 ~-2 {display:{Name:\"A B\"},Items:[{tag:{note:\"two words\"}}]}",
            LegacyCommandCompatibility.translate("/blockdata ~ ~1 ~-2 {display:{Name:\"A B\"},Items:[{tag:{note:\"two words\"}}]}")
        )
    }

    @Test
    fun `does not translate an incomplete SNBT compound`() {
        assertNull(LegacyCommandCompatibility.translate("blockdata 0 0 0 {CustomName:\"missing end\""))
    }

    @Test
    fun `leaves unknown commands alone`() {
        assertNull(LegacyCommandCompatibility.translate("unknown 1 2 3"))
    }

    @Test
    fun `rewrites entitydata and preserves nested SNBT`() {
        assertEquals(
            "data merge entity @e[type=minecraft:pig] {CustomName:\"A B\",Tags:[\"one two\"]}",
            LegacyCommandCompatibility.translate("entitydata @e[type=minecraft:pig] {CustomName:\"A B\",Tags:[\"one two\"]}")
        )
    }

    @Test
    fun `rewrites entitydata with optional slash`() {
        assertEquals(
            "/data merge entity @s {CustomName:\"Example\"}",
            LegacyCommandCompatibility.translate("/entitydata @s {CustomName:\"Example\"}")
        )
    }

    @Test
    fun `rewrites xp points to experience add`() {
        assertEquals("experience add @s 5 points", LegacyCommandCompatibility.translate("xp 5", sourceHasEntity = true))
        assertEquals("/experience add @s -5 points", LegacyCommandCompatibility.translate("/xp -5", sourceHasEntity = true))
    }

    @Test
    fun `rewrites xp levels and explicit targets`() {
        assertEquals("experience add @s 7 levels", LegacyCommandCompatibility.translate("xp 7L", sourceHasEntity = true))
        assertEquals("experience add Alex -2 levels", LegacyCommandCompatibility.translate("xp -2l Alex"))
        assertEquals("experience add @a 12 points", LegacyCommandCompatibility.translate("xp 12 @a"))
    }

    @Test
    fun `xp without a target from a non entity source stays incomplete`() {
        assertEquals("experience add", LegacyCommandCompatibility.translate("xp 5"))
    }

    @Test
    fun `rewrites effect clear and give forms`() {
        assertEquals("effect clear Alex", LegacyCommandCompatibility.translate("effect Alex clear"))
        assertEquals("effect give @a minecraft:speed", LegacyCommandCompatibility.translate("effect @a minecraft:speed"))
        assertEquals("effect give @s minecraft:speed 45 2 true", LegacyCommandCompatibility.translate("effect @s minecraft:speed 45 2 true"))
    }

    @Test
    fun `effect zero duration clears and numeric ids stay unsupported`() {
        assertEquals("effect clear @p minecraft:speed", LegacyCommandCompatibility.translate("effect @p minecraft:speed 0"))
        assertNull(LegacyCommandCompatibility.translate("effect @p 1 30 0"))
    }

    @Test
    fun `rewrites infinite effect duration`() {
        assertEquals("effect give @s minecraft:strength infinite 1 true", LegacyCommandCompatibility.translate("effect @s minecraft:strength -1 1 true"))
    }

    @Test
    fun `rewrites replaceitem entity and normalizes slots`() {
        assertEquals(
            "item replace entity @p armor.head with minecraft:diamond_helmet 1",
            LegacyCommandCompatibility.translate("replaceitem entity @p slot.armor.head minecraft:diamond_helmet 1")
        )
    }

    @Test
    fun `rewrites replaceitem block and leaves count defaulted`() {
        assertEquals(
            "item replace block ~ ~-1 ~ container.0 with stone",
            LegacyCommandCompatibility.translate("replaceitem block ~ ~-1 ~ slot.container.0 stone")
        )
    }

    @Test
    fun `does not rewrite replaceitem legacy damage or nbt`() {
        assertNull(LegacyCommandCompatibility.translate("replaceitem entity @s slot.weapon.mainhand minecraft:stone 1 2"))
        assertNull(LegacyCommandCompatibility.translate("replaceitem entity @s slot.weapon.mainhand minecraft:stone{display:{Name:\"Old\"}} 1"))
        assertNull(LegacyCommandCompatibility.translate("replaceitem entity @s slot.weapon.mainhand 1 1"))
    }
}
