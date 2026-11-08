package dev.glap;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.resources.Identifier;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

class LegacyFunctionCompatibilityTest {
    @Test
    void legacyCommandCanBeLoadedInFunction() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        dispatcher.register(Commands.literal("data")
            .then(Commands.argument("rest", StringArgumentType.greedyString()).executes(context -> 1)));
        CommandSourceStack source = mock(CommandSourceStack.class);
        assertDoesNotThrow(() -> CommandFunction.fromLines(
            Identifier.parse("glap:legacy"), dispatcher, source,
            List.of("blockdata 0 0 0 {foo:1}", "data merge block 0 0 0 {foo:1}")));
    }

    @Test
    void legacyAndModernExecuteCanBeLoadedInFunction() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        dispatcher.register(Commands.literal("execute").then(Commands.literal("as")
            .then(Commands.argument("rest", StringArgumentType.greedyString()).executes(context -> 1))));
        CommandSourceStack source = mock(CommandSourceStack.class);
        assertDoesNotThrow(() -> CommandFunction.fromLines(
            Identifier.parse("glap:execute"), dispatcher, source,
            List.of("execute @p ~ ~ ~ say hello", "execute as @p run say hello")));
    }
}
