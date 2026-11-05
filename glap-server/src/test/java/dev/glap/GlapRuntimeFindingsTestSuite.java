package dev.glap;

import net.minecraft.server.commands.GameRuleCommandAliasesTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({GitHubUpdateCheckerTest.class, GameRuleCommandAliasesTest.class})
class GlapRuntimeFindingsTestSuite {
}
