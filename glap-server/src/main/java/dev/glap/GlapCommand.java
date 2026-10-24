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
    private static final List<String> SUBCOMMANDS = List.of("version", "status", "reload");
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
        final String sub = args.length == 0 ? "version" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "version" -> {
                sender.sendMessage(text("업데이트를 확인하는 중이에요...", NamedTextColor.GRAY));
                CompletableFuture.supplyAsync(() -> new GlapVersionFetcher().getVersionMessage())
                    .thenAccept(message -> this.server.execute(() -> sender.sendMessage(message)));
            }
            case "status" -> {
                final GlapSettings settings = GlapConfig.settings();
                sender.sendMessage(text("GLAP1 " + GlapVersionFetcher.CURRENT_VERSION + " · 설정 버전 " + settings.configVersion(), NamedTextColor.GOLD));
                sender.sendMessage(text("업데이트 확인: " + enabled(settings.updatesEnabled()), NamedTextColor.GRAY));
                sender.sendMessage(text("Paper Metrics 설정: " + enabled(settings.paperMetricsEnabled()), NamedTextColor.GRAY));
                sender.sendMessage(text("Paper Metrics 현재 상태: " + enabled(GlapConfig.metricsEnabledAtStartup()), NamedTextColor.GRAY));
                if (settings.paperMetricsEnabled() != GlapConfig.metricsEnabledAtStartup()) {
                    sender.sendMessage(text("Paper Metrics 변경은 서버를 재시작해야 적용돼요.", NamedTextColor.YELLOW));
                }
            }
            case "reload" -> {
                if (GlapConfig.load()) {
                    sender.sendMessage(text("glap.yml을 다시 불러왔어요.", NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(text("glap.yml 설정을 적용하지 못했어요: " + GlapConfig.lastLoadError(), NamedTextColor.RED));
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

    private static String enabled(final boolean value) {
        return value ? "켜짐" : "꺼짐";
    }
}
