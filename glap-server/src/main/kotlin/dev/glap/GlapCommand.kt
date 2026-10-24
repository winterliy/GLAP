package dev.glap

import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.server.MinecraftServer
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import java.util.concurrent.CompletableFuture

/** `/glap version` and `/glap reload`. */
class GlapCommand : Command("glap") {
    init {
        description = "GLAP1 버전 확인과 설정 다시 불러오기"
        usageMessage = "/glap <version|reload>"
        permission = "glap.command"
    }

    override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
        if (!testPermission(sender)) return true
        when (args.firstOrNull()?.lowercase()) {
            "version", null -> {
                sender.sendMessage(text("업데이트를 확인하는 중이에요...", NamedTextColor.GRAY))
                CompletableFuture.supplyAsync { GlapVersionFetcher().versionMessage }
                    .thenAccept(sender::sendMessage)
            }
            "reload" -> {
                if (GlapConfig.load()) {
                    sender.sendMessage(text("glap.yml을 다시 불러왔어요.", NamedTextColor.GREEN))
                } else {
                    sender.sendMessage(text("glap.yml을 읽지 못했어요. 콘솔 오류를 확인하세요.", NamedTextColor.RED))
                }
            }
            else -> sender.sendMessage(text(usageMessage, NamedTextColor.RED))
        }
        return true
    }

    override fun tabComplete(sender: CommandSender, alias: String, args: Array<out String>): List<String> =
        if (args.size == 1) SUBCOMMANDS.filter { it.startsWith(args[0].lowercase()) } else emptyList()

    companion object {
        private val SUBCOMMANDS = listOf("version", "reload")

        @JvmStatic
        fun register(server: MinecraftServer) {
            server.server.commandMap.register("glap", "GLAP", GlapCommand())
        }
    }
}
