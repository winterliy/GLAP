package net.minecraft.server.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

public class GameRuleCommandAliasesTest {
    @Test
    void keepsTheOldDaylightCycleNameAsAnAlias() {
        assertEquals(List.of("doDaylightCycle"), GameRuleCommand.legacyAliases("advance_time"));
    }

    @Test
    void keepsTheOldWeatherCycleNameAsAnAlias() {
        assertEquals(List.of("doWeatherCycle"), GameRuleCommand.legacyAliases("advance_weather"));
    }
}
