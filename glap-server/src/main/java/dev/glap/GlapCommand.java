package dev.glap;

import java.util.List;
import java.util.ServiceLoader;
import net.minecraft.server.MinecraftServer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

/** Registers the GLAP command with Paper's command map. */
public final class GlapCommand extends Command {
    private static final GlapCommandRouter ROUTER = ServiceLoader.load(GlapCommandRouter.class)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("GLAP command router provider is missing"));

    private final MinecraftServer server;

    private GlapCommand(final MinecraftServer server) {
        super("glap");
        this.server = server;
        this.description = "GLAP1 버전 확인과 설정 다시 불러오기";
        this.usageMessage = "/glap <version|status|reload>";
        this.setPermission("glap.command");
    }

    public static void register(final MinecraftServer server) {
        server.server.getCommandMap().register("glap", "GLAP", new GlapCommand(server));
    }

    @Override
    public boolean execute(final CommandSender sender, final String commandLabel, final String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }
        return ROUTER.execute(this.server, sender, args);
    }

    @Override
    public List<String> tabComplete(final CommandSender sender, final String alias, final String[] args) {
        if (!sender.hasPermission(this.getPermission())) {
            return List.of();
        }
        return ROUTER.suggest(this.server, sender, args);
    }
}
