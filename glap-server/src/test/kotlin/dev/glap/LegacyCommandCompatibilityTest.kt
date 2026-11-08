package dev.glap

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LegacyCommandCompatibilityTest {
    @Test
    fun `legacy execute accepts UUID targets and names matching modern keywords`() {
        val uuid = "12345678-1234-1234-1234-123456789abc"
        assertEquals(
            "execute as $uuid at @s positioned ~ ~ ~ run say hello",
            LegacyCommandCompatibility.translate("execute $uuid ~ ~ ~ say hello")
        )
        assertEquals(
            "execute as as at @s positioned ~ ~ ~ run say hello",
            LegacyCommandCompatibility.translate("execute as ~ ~ ~ say hello")
        )
    }
    @Test
    fun `legacy execute absolute integer coordinates are not centered`() {
        assertEquals(
            "execute as @p at @s positioned 12.0 64 -3.0 run say hello",
            LegacyCommandCompatibility.translate("execute @p 12 64 -3 say hello")
        )
    }
    @Test
    fun `legacy execute converts minimum and maximum selector radius`() {
        assertEquals(
            "execute as @e[distance=2..10] at @s positioned ~ ~ ~ run say found",
            LegacyCommandCompatibility.translate("execute @e[rm=2,r=10] ~ ~ ~ say found")
        )
    }

    @Test
    fun `legacy execute converts selector level and radius ranges together`() {
        assertEquals(
            "execute as @a[distance=2..10,level=5..20] at @s positioned ~ ~ ~ run say found",
            LegacyCommandCompatibility.translate("execute @a[rm=2,r=10,lm=5,l=20] ~ ~ ~ say found")
        )
    }

    @Test
    fun `legacy selector level bounds support one sided ranges and reject conflicts`() {
        assertEquals(
            "execute as @a[level=..20] at @s positioned ~ ~ ~ run say found",
            LegacyCommandCompatibility.translate("execute @a[l=20] ~ ~ ~ say found")
        )
        assertEquals(
            "execute as @a[level=5..] at @s positioned ~ ~ ~ run say found",
            LegacyCommandCompatibility.translate("execute @a[lm=5] ~ ~ ~ say found")
        )
        assertNull(LegacyCommandCompatibility.translate("execute @a[l=20,level=1..10] ~ ~ ~ say found"))
        assertNull(LegacyCommandCompatibility.translate("execute @a[l=abc] ~ ~ ~ say found"))
    }
    @Test
    fun `legacy execute detects any state of a named block`() {
        assertEquals(
            "execute as @a at @s positioned ~ ~ ~ if block ~ ~-1 ~ minecraft:bedrock run say found",
            LegacyCommandCompatibility.translate("execute @a ~ ~ ~ detect ~ ~-1 ~ minecraft:bedrock -1 say found")
        )
    }

    @Test
    fun `legacy execute wildcard detects block variants with state properties`() {
        val command = LegacyCommandCompatibility.translate("execute @a ~ ~ ~ detect ~ ~-1 ~ log -1 say found")
        assertTrue(command?.contains("minecraft:oak_log") == true)
        assertTrue(command?.contains("axis:\"x\"") == true)
        assertTrue(command?.contains("axis:\"y\"") == true)
    }

    @Test
    fun `legacy execute does not silently discard block metadata`() {
        assertEquals(35, net.minecraft.util.datafix.fixes.EntityBlockStateFix.getBlockId("minecraft:wool"))
        assertEquals("minecraft:red_wool", net.minecraft.util.datafix.fixes.BlockStateData.getTag(574).get("Name").asString(""))
        net.minecraft.SharedConstants.tryDetectVersion()
        val upgraded = net.minecraft.util.datafix.DataFixers.getDataFixer().update(
            net.minecraft.util.datafix.fixes.References.BLOCK_STATE,
            net.minecraft.util.datafix.fixes.BlockStateData.getTag(574), 1451,
            net.minecraft.SharedConstants.getCurrentVersion().dataVersion().version()
        )
        assertEquals("minecraft:red_wool", upgraded.get("id").asString(""), upgraded.toString())
        assertEquals(
            "execute as @a at @s positioned ~ ~ ~ if block ~ ~-1 ~ minecraft:red_wool run say found",
            LegacyCommandCompatibility.translate("execute @a ~ ~ ~ detect ~ ~-1 ~ wool 14 say found")
        )
        assertEquals(
            "execute as @a at @s positioned ~ ~ ~ if block ~ ~-1 ~ minecraft:red_wool run say found",
            LegacyCommandCompatibility.translate("execute @a ~ ~ ~ detect ~ ~-1 ~ 35 14 say found")
        )
        assertEquals(
            "execute as @a at @s positioned ~ ~ ~ if block ~ ~-1 ~ minecraft:oak_log[axis=x] run say found",
            LegacyCommandCompatibility.translate("execute @a ~ ~ ~ detect ~ ~-1 ~ log 4 say found")
        )
        assertNull(LegacyCommandCompatibility.translate("execute @a ~ ~ ~ detect ~ ~-1 ~ wool 99 say found"))
        val wildcard = LegacyCommandCompatibility.translate("execute @a ~ ~ ~ detect ~ ~-1 ~ stone -1 say found")
        assertTrue(wildcard?.contains("type:\"minecraft:location_check\"") == true)
        assertTrue(wildcard?.contains("minecraft:granite") == true)
        assertTrue(wildcard?.contains("minecraft:polished_andesite") == true)
    }
    @Test
    fun `legacy execute runs at each target position with coordinates relative to it`() {
        assertEquals(
            "execute as @a at @s positioned ~ ~1 ~-2 run say hello world",
            LegacyCommandCompatibility.translate("execute @a ~ ~1 ~-2 say hello world")
        )
    }

    @Test
    fun `legacy execute recursively translates nested legacy commands`() {
        assertEquals(
            "/execute as @p at @s positioned ~ ~ ~ run effect give @s minecraft:speed 10",
            LegacyCommandCompatibility.translate("/execute @p ~ ~ ~ effect @s minecraft:speed 10")
        )
    }

    @Test
    fun `modern execute syntax remains untouched`() {
        assertNull(LegacyCommandCompatibility.translate("execute as @a at @s positioned ~ ~ ~ run say hello"))
    }
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
