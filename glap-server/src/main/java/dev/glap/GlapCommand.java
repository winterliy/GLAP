package dev.glap;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.server.MinecraftServer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import static net.kyori.adventure.text.Component.text;

/** {@code /glap version} and {@code /glap reload}. */
public final class GlapCommand extends Command {
    private static final List<String> SUBCOMMANDS = List.of("version", "reload");

    private GlapCommand() {
        super("glap");
        this.description = "GLAP1 버전 확인과 설정 다시 불러오기";
        this.usageMessage = "/glap <version|reload>";
        this.setPermission("glap.command");
    }

    public static void register(final MinecraftServer server) {
        server.server.getCommandMap().register("glap", "GLAP", new GlapCommand());
    }

    @Override
    public boolean execute(final CommandSender sender, final String commandLabel, final String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }
        final String sub = args.length == 0 ? "version" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "version" -> {
                sender.sendMessage(text("업데이트를 확인하는 중이에요...", NamedTextColor.GRAY));
                CompletableFuture.supplyAsync(() -> new GlapVersionFetcher().getVersionMessage())
                    .thenAccept(sender::sendMessage);
            }
            case "reload" -> {
                if (GlapConfig.load()) {
                    sender.sendMessage(text("glap.yml을 다시 불러왔어요.", NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(text("glap.yml을 읽지 못했어요. 콘솔 오류를 확인하세요.", NamedTextColor.RED));
                }
            }
            default -> sender.sendMessage(text(this.usageMessage, NamedTextColor.RED));
        }
        return true;
    }

    @Override
    public List<String> tabComplete(final CommandSender sender, final String alias, final String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        final String prefix = args[0].toLowerCase(Locale.ROOT);
        return SUBCOMMANDS.stream().filter(s -> s.startsWith(prefix)).toList();
    }
}
