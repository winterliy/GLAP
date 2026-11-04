package dev.glap

import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.server.MinecraftServer
import org.bukkit.command.CommandSender
import java.util.Locale
import java.util.concurrent.CompletableFuture

@DslMarker
annotation class GlapCommandDsl

/** A parsed command invocation. Values returned by typed arguments retain their Kotlin type. */
class GlapCommandContext internal constructor(
    val sender: CommandSender,
    val server: MinecraftServer,
    private val values: Map<String, Any>
) {
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> argument(name: String): T = values[name] as? T
        ?: error("No parsed command argument named '$name'")
}

interface GlapArgumentType<T : Any> {
    fun parse(token: String): T?
    fun suggest(sender: CommandSender, prefix: String): List<String> = emptyList()
}

object GlapArguments {
    val word: GlapArgumentType<String> = object : GlapArgumentType<String> {
        override fun parse(token: String) = token.takeIf(String::isNotEmpty)
    }

    val integer: GlapArgumentType<Int> = object : GlapArgumentType<Int> {
        override fun parse(token: String) = token.toIntOrNull()
    }

    val greedyString: GlapArgumentType<String> = object : GlapArgumentType<String> {
        override fun parse(token: String) = token.takeIf(String::isNotEmpty)
    }
}

internal class CommandNode(
    val name: String?,
    val argumentName: String? = null,
    val parser: ((String) -> Any?)? = null,
    val suggester: ((CommandSender, String) -> List<String>)? = null,
    val greedy: Boolean = false
) {
    val children = mutableListOf<CommandNode>()
    var handler: ((GlapCommandContext) -> Unit)? = null
}

@GlapCommandDsl
class GlapCommandNodeBuilder internal constructor(private val node: CommandNode) {
    fun executes(handler: (GlapCommandContext) -> Unit) {
        node.handler = handler
    }

    fun literal(name: String, block: GlapCommandNodeBuilder.() -> Unit = {}) {
        require(name.isNotBlank() && name.none(Char::isWhitespace)) { "Command literal must be one word" }
        CommandNode(name.lowercase(Locale.ROOT)).also { child ->
            node.children += child
            GlapCommandNodeBuilder(child).block()
        }
    }

    fun <T : Any> argument(
        name: String,
        type: GlapArgumentType<T>,
        greedy: Boolean = false,
        block: GlapCommandNodeBuilder.() -> Unit = {}
    ) {
        require(name.isNotBlank()) { "Argument name cannot be blank" }
        val child = CommandNode(
            name = null,
            argumentName = name,
            parser = { token -> type.parse(token) },
            suggester = { sender, prefix -> type.suggest(sender, prefix) },
            greedy = greedy
        )
        node.children += child
        GlapCommandNodeBuilder(child).block()
    }
}

private class GlapCommandTree(block: GlapCommandNodeBuilder.() -> Unit) {
    private val root = CommandNode(name = null).also { GlapCommandNodeBuilder(it).block() }

    fun execute(server: MinecraftServer, sender: CommandSender, arguments: Array<out String>): Boolean {
        val tokens = arguments.toList()
        var node = root
        val values = linkedMapOf<String, Any>()
        var index = 0

        while (index < tokens.size) {
            val token = tokens[index]
            val literal = node.children.firstOrNull { it.name.equals(token, ignoreCase = true) }
            if (literal != null) {
                node = literal
                index++
                continue
            }

            val argument = node.children.firstOrNull { child -> child.parser?.invoke(token) != null }
                ?: return unknown(sender)
            val raw = if (argument.greedy) tokens.drop(index).joinToString(" ") else token
            val value = argument.parser!!.invoke(raw) ?: return unknown(sender)
            values[argument.argumentName!!] = value
            node = argument
            index = if (argument.greedy) tokens.size else index + 1
        }

        val handler = node.handler ?: return unknown(sender)
        handler(GlapCommandContext(sender, server, values))
        return true
    }

    fun suggest(sender: CommandSender, arguments: Array<out String>): List<String> {
        if (arguments.isEmpty()) return root.children.mapNotNull(CommandNode::name)
        val path = arguments.dropLast(1)
        val prefix = arguments.last().lowercase(Locale.ROOT)
        var node = root
        for (token in path) {
                node = node.children.firstOrNull { it.name?.equals(token, ignoreCase = true) == true }
                ?: node.children.firstOrNull { child -> child.parser?.invoke(token) != null }
                ?: return emptyList()
        }
        return node.children.flatMap { child ->
            child.name?.let { listOf(it) }
                ?: child.suggester?.invoke(sender, prefix).orEmpty()
        }.filter { it.lowercase(Locale.ROOT).startsWith(prefix) }.distinct().sorted()
    }

    private fun unknown(sender: CommandSender): Boolean {
        sender.sendMessage(net.kyori.adventure.text.Component.text("사용법: /glap <version|status|reload>", NamedTextColor.RED))
        return true
    }
}

class GlapCommandRouterImpl : GlapCommandRouter {
    private val commands = GlapCommandTree {
        executes { showVersion(it) }
        literal("version") { executes { showVersion(it) } }
        literal("status") {
            executes { context ->
                val settings = GlapConfig.settings()
                context.sender.sendMessage(net.kyori.adventure.text.Component.text(
                    "GLAP1 ${GlapVersionFetcher.CURRENT_VERSION} · 설정 버전 ${settings.configVersion()}", NamedTextColor.GOLD
                ))
                context.sender.sendMessage(net.kyori.adventure.text.Component.text("업데이트 확인: ${enabled(settings.updatesEnabled())}", NamedTextColor.GRAY))
                context.sender.sendMessage(net.kyori.adventure.text.Component.text("Paper Metrics 설정: ${enabled(settings.paperMetricsEnabled())}", NamedTextColor.GRAY))
                context.sender.sendMessage(net.kyori.adventure.text.Component.text("Paper Metrics 현재 상태: ${enabled(GlapConfig.metricsEnabledAtStartup())}", NamedTextColor.GRAY))
                if (settings.paperMetricsEnabled() != GlapConfig.metricsEnabledAtStartup()) {
                    context.sender.sendMessage(net.kyori.adventure.text.Component.text("Paper Metrics 변경은 서버를 재시작해야 적용돼요.", NamedTextColor.YELLOW))
                }
            }
        }
        literal("reload") {
            executes { context ->
                check(context.server.isSameThread()) { "GLAP settings must be reloaded on the server thread" }
                if (GlapConfig.load()) {
                    context.sender.sendMessage(net.kyori.adventure.text.Component.text("glap.yml을 다시 불러왔어요.", NamedTextColor.GREEN))
                } else {
                    context.sender.sendMessage(net.kyori.adventure.text.Component.text("glap.yml 설정을 적용하지 못했어요: ${GlapConfig.lastLoadError()}", NamedTextColor.RED))
                }
            }
        }
    }

    override fun execute(server: MinecraftServer, sender: CommandSender, arguments: Array<out String>): Boolean {
        check(server.isSameThread()) { "GLAP commands must execute on the server thread" }
        return commands.execute(server, sender, arguments)
    }

    override fun suggest(server: MinecraftServer, sender: CommandSender, arguments: Array<out String>): List<String> {
        check(server.isSameThread()) { "GLAP command suggestions must run on the server thread" }
        return commands.suggest(sender, arguments)
    }

    private fun showVersion(context: GlapCommandContext) {
        context.sender.sendMessage(net.kyori.adventure.text.Component.text("업데이트를 확인하는 중이에요...", NamedTextColor.GRAY))
        CompletableFuture.supplyAsync { GlapVersionFetcher().versionMessage }
            .thenAccept { message -> context.server.execute { context.sender.sendMessage(message) } }
    }

    private fun enabled(value: Boolean) = if (value) "켜짐" else "꺼짐"
}

