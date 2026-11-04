package dev.glap;

import java.util.List;
import net.minecraft.server.MinecraftServer;
import org.bukkit.command.CommandSender;

/** Java boundary between Paper's command bootstrap and GLAP's Kotlin command tree. */
public interface GlapCommandRouter {
    boolean execute(MinecraftServer server, CommandSender sender, String[] arguments);

    List<String> suggest(MinecraftServer server, CommandSender sender, String[] arguments);
}
