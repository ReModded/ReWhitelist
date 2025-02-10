package dev.remodded.rewhitelist.command

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import com.velocitypowered.api.command.CommandSource
import dev.remodded.rewhitelist.ReWhitelist
import dev.remodded.rewhitelist.Whitelist
import dev.remodded.rewhitelist.utils.CommandUtils
import dev.remodded.rewhitelist.utils.CommandUtils.argument
import dev.remodded.rewhitelist.utils.CommandUtils.literal
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import java.util.concurrent.CompletableFuture
import kotlin.jvm.optionals.getOrNull

object WhitelistSettingsSubcommand {
    fun whitelistSettingsSubcommand(whitelistResolver: (CommandContext<CommandSource>)-> Whitelist): LiteralArgumentBuilder<CommandSource> =
        literal("settings")
            .requires(CommandUtils.permissionRequirement("rewhitelist.command.whitelist.settings"))
            .executes { ctx -> help(ctx.source) }
            .then(
                literal("servers")
                    .requires(CommandUtils.permissionRequirement("rewhitelist.command.whitelist.settings.servers"))
                    .executes { ctx -> serversHelp(ctx.source) }
                    .then(
                        literal("add")
                            .requires(CommandUtils.permissionRequirement("rewhitelist.command.whitelist.settings.servers.edit"))
                            .then(
                                argument("server", StringArgumentType.string())
                                    .suggests { _, builder -> suggestRegisteredServers(builder) }
                                    .executes { ctx -> addServer(ctx.source, whitelistResolver(ctx), StringArgumentType.getString(ctx, "server")) }
                            )
                    )
                    .then(
                        literal("remove")
                            .requires(CommandUtils.permissionRequirement("rewhitelist.command.whitelist.settings.servers.edit"))
                            .then(
                                argument("server", StringArgumentType.string())
                                    .suggests { ctx, builder -> suggestAddedServers(whitelistResolver(ctx), builder) }
                                    .executes { ctx -> removeServer(ctx.source, whitelistResolver(ctx), StringArgumentType.getString(ctx, "server")) }
                            )
                    )
                    .then(
                        literal("list")
                            .requires(CommandUtils.permissionRequirement("rewhitelist.command.whitelist.settings.servers.list"))
                            .executes { ctx -> listServers(ctx.source, whitelistResolver(ctx)) }
                    )
                    .then(
                        literal("clear")
                            .requires(CommandUtils.permissionRequirement("rewhitelist.command.whitelist.settings.servers.edit"))
                            .executes { ctx -> clearServers(ctx.source, whitelistResolver(ctx)) }
                    )
            )


    private fun help(src: CommandSource): Int {
        src.sendMessage(Component.text("ReWhitelist Help:"))
        src.sendMessage(Component.text("/whitelist [group] settings servers <...>", NamedTextColor.GRAY))
        return 0
    }

    private fun serversHelp(src: CommandSource): Int {
        src.sendMessage(Component.text("ReWhitelist Help:"))
        src.sendMessage(Component.text("/whitelist [group] settings servers add <server>", NamedTextColor.GRAY))
        src.sendMessage(Component.text("/whitelist [group] settings servers remove <server>", NamedTextColor.GRAY))
        src.sendMessage(Component.text("/whitelist [group] settings servers list", NamedTextColor.GRAY))
        src.sendMessage(Component.text("/whitelist [group] settings servers clear", NamedTextColor.GRAY))
        return 0
    }

    private fun addServer(src: CommandSource, whitelist: Whitelist, serverName: String): Int {
        whitelist.servers.add(serverName)
        whitelist.save()
        src.sendMessage(Component.text("Server ($serverName) has been added to whitelist (${whitelist.name})", NamedTextColor.GREEN))

        val server = ReWhitelist.server.getServer(serverName).getOrNull()
        if (server == null)
            src.sendMessage(Component.text("Server ($serverName) isn't registered with Velocity", NamedTextColor.YELLOW))
        return 0
    }

    private fun removeServer(src: CommandSource, whitelist: Whitelist, serverName: String): Int {
        whitelist.servers.remove(serverName)
        whitelist.save()
        src.sendMessage(Component.text("Server ($serverName) has been removed from whitelist (${whitelist.name})", NamedTextColor.GREEN))
        return 0
    }

    private fun listServers(src: CommandSource, whitelist: Whitelist): Int {
        src.sendMessage(Component.text()
            .append(Component.text("Whitelist "))
            .append(Component.text(whitelist.name, NamedTextColor.YELLOW)
                .clickEvent(ClickEvent.runCommand("/whitelist ${whitelist.name} settings servers list"))
                .hoverEvent(HoverEvent.showText(Component.text("Refresh", NamedTextColor.GREEN)))
            )
            .append(Component.text(" servers:"))
            .build()
        )
        whitelist.servers.forEach { src.sendMessage(Component.text()
            .append(Component.text(" - ", NamedTextColor.GRAY))
            .append(Component.text("$it    "))
            .append(Component.text("REMOVE", NamedTextColor.RED)
                .clickEvent(ClickEvent.runCommand("/whitelist ${whitelist.name} settings servers remove $it"))
                .hoverEvent(HoverEvent.showText(Component.text("Remove server", NamedTextColor.YELLOW)))
            )
            .build()
        )}
        return 0
    }

    private fun clearServers(src: CommandSource, whitelist: Whitelist): Int {
        whitelist.servers.clear()
        whitelist.save()
        src.sendMessage(Component.text("Whitelist (${whitelist.name}) servers has been cleared", NamedTextColor.GREEN))
        return 0
    }



    private fun suggestRegisteredServers(builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        ReWhitelist.server.allServers.forEach { builder.suggest(it.serverInfo.name) }
        return builder.buildFuture()
    }

    private fun suggestAddedServers(whitelist: Whitelist, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        whitelist.servers.forEach { builder.suggest(it) }
        return builder.buildFuture()
    }
}
