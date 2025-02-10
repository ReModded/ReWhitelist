package dev.remodded.rewhitelist.command

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.command.VelocityBrigadierMessage
import dev.remodded.rewhitelist.ReWhitelist
import dev.remodded.rewhitelist.Whitelist
import dev.remodded.rewhitelist.command.WhitelistSettingsSubcommand.whitelistSettingsSubcommand
import dev.remodded.rewhitelist.entries.Entry
import dev.remodded.rewhitelist.utils.CommandUtils
import dev.remodded.rewhitelist.utils.CommandUtils.argument
import dev.remodded.rewhitelist.utils.CommandUtils.literal
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.feature.pagination.Pagination
import net.kyori.adventure.text.format.NamedTextColor

object WhitelistManagementSubcommand {
    fun <T: ArgumentBuilder<CommandSource, T>> T.whitelistManagementSubcommand(whitelistNameResolver: (CommandContext<CommandSource>)->String): T {
        val whitelistResolver = {ctx: CommandContext<CommandSource> -> getWhitelist(whitelistNameResolver(ctx)) }

        return then(
            literal("add")
                .requires(CommandUtils.permissionRequirement("rewhitelist.command.whitelist.add"))
                .apply {
                    for (entryFactory in ReWhitelist.entryRegistry.getAll().values)
                        then(
                            literal(entryFactory.type)
                                .then(entryFactory.getCommandNode { ctx, entry ->
                                    addEntry(ctx.source, whitelistResolver(ctx), entry)
                                })
                                .executes { ctx -> addEntryHelp(ctx.source, entryFactory) }
                        )
                }
                .executes { ctx -> addEntryHelp(ctx.source) }
        )
        .then(
            literal("remove")
                .requires(CommandUtils.permissionRequirement("rewhitelist.command.whitelist.remove"))
                .then(
                    argument("entry", StringArgumentType.string())
                        .executes { ctx -> removeWhitelistEntry(ctx.source, whitelistResolver(ctx), StringArgumentType.getString(ctx, "entry")) }
                )
        )
        .then(
            literal("on")
                .requires(CommandUtils.permissionRequirement("rewhitelist.command.whitelist.on"))
                .executes { ctx -> switchWhitelist(ctx.source, whitelistResolver(ctx), true) }
        )
        .then(
            literal("off")
                .requires(CommandUtils.permissionRequirement("rewhitelist.command.whitelist.off"))
                .executes { ctx -> switchWhitelist(ctx.source, whitelistResolver(ctx), false) }
        )
        .then(
            literal("list")
                .requires(CommandUtils.permissionRequirement("rewhitelist.command.whitelist.list"))
                .then(
                    argument("page", IntegerArgumentType.integer(1))
                        .executes { ctx -> sendWhitelist(ctx.source, whitelistResolver(ctx), IntegerArgumentType.getInteger(ctx, "page")) }
                )
                .executes { ctx -> sendWhitelist(ctx.source, whitelistResolver(ctx), 1) }
        )
        .then(whitelistSettingsSubcommand(whitelistResolver))
        .executes { ctx -> sendWhitelist(ctx.source, whitelistResolver(ctx), 1) }
    }

    private fun addEntryHelp(src: CommandSource): Int {
        src.sendMessage(Component.text("ReWhitelist Help:"))
        for (entry in ReWhitelist.entryRegistry.getAll().values) {
            val help = entry.getHelp().let {
                if (it.isEmpty())
                    Component.empty()
                else
                    it[0]
            }
            src.sendMessage(Component.text("/whitelist [group] add ${entry.type} ", NamedTextColor.GRAY).append(help))
        }
        return 0
    }

    private fun addEntryHelp(src: CommandSource, entry: Entry.Factory<*>): Int {
        src.sendMessage(Component.text("ReWhitelist Help:"))
        for (help in entry.getHelp())
            src.sendMessage(Component.text("/whitelist [group] add ${entry.type} ", NamedTextColor.GRAY).append(help))
        return 0
    }

    private fun addEntry(src: CommandSource, whitelist: Whitelist, entry: Entry) {
        whitelist.entries.add(entry)
        whitelist.save()

        src.sendMessage(Component.text("New ${entry.factory.type} entry has been added to whitelist (${whitelist.name})", NamedTextColor.GREEN))
    }

    private fun removeWhitelistEntry(src: CommandSource, whitelist: Whitelist, entryValue: String): Int {
        whitelist.entries.forEach { entry ->
            if (entryValue == entry.toString()) {
                whitelist.entries.remove(entry)
                whitelist.save()

                src.sendMessage(Component.text("Entry ($entryValue) has been removed from whitelist (${whitelist.name})", NamedTextColor.GREEN))
                return 0
            }
        }

        src.sendMessage(Component.text("No entry ($entryValue) found in whitelist (${whitelist.name})", NamedTextColor.YELLOW))
        return 1
    }

    private fun switchWhitelist(src: CommandSource, whitelist: Whitelist, enable: Boolean): Int {
        if (whitelist.enabled == enable) {
            src.sendMessage(Component.text("Whitelist (${whitelist.name}) was already " + if(enable) "enabled" else "disabled", NamedTextColor.YELLOW))
            return 1
        }

        if (enable) {
            whitelist.enable()
            src.sendMessage(Component.text("Whitelist (${whitelist.name}) has been enabled", NamedTextColor.GREEN))
        } else {
            whitelist.disable()
            src.sendMessage(Component.text("Whitelist (${whitelist.name}) has been disabled", NamedTextColor.GREEN))
        }
        return 0
    }

    private fun sendWhitelist(src: CommandSource, whitelist: Whitelist, page: Int): Int {
        val maxEntryTypeLength = ReWhitelist.entryRegistry.getAll().keys.maxOfOrNull { t -> t.length } ?: 4

        val header = Component.text()
            .append(Component.text("Whitelist "))
            .append(
                Component.text(whitelist.name, NamedTextColor.YELLOW)
                    .clickEvent(ClickEvent.runCommand("/whitelist ${whitelist.name}"))
                    .hoverEvent(HoverEvent.showText(Component.text("Refresh", NamedTextColor.GREEN)))
            )
            .append(Component.text(" [", NamedTextColor.GRAY))
            .apply {
                if (whitelist.enabled)
                    it.append(
                        Component.text("on", NamedTextColor.GREEN)
                            .clickEvent(ClickEvent.runCommand("/whitelist ${whitelist.name} off"))
                            .hoverEvent(HoverEvent.showText(Component.text("Disable whitelist",NamedTextColor.RED)))
                    )
                else
                    it.append(
                        Component.text("off", NamedTextColor.RED)
                            .clickEvent(ClickEvent.runCommand("/whitelist ${whitelist.name} on"))
                            .hoverEvent(HoverEvent.showText(Component.text("Enable whitelist", NamedTextColor.GREEN)))
                    )
            }
            .append(Component.text("]", NamedTextColor.GRAY))
            .build()

        Pagination.builder()
            .renderer(object : Pagination.Renderer {
                override fun renderEmpty(): Component {
                    val header = renderHeader(header, 0, 0)
                    val line = Component.text("-".repeat((50 - length(header)) / 2), NamedTextColor.DARK_GRAY)
                    return Component.text()
                        .append(line)
                        .append(header)
                        .append(line)
                        .append(Component.newline())
                        .append(Component.text("NO ENTRIES", NamedTextColor.GRAY))
                        .append(Component.newline())
                        .append(Component.text("-".repeat(50), NamedTextColor.DARK_GRAY))
                        .build()
                }

                fun length(component: Component): Int {
                    return (if (component is TextComponent) component.content().length else 0) +
                        component.children().sumOf { length(it) }
                }
            })
            .width(50).build(
            header,
            Pagination.Renderer.RowRenderer<Entry> { entry, index ->
                if (entry == null)
                    return@RowRenderer emptyList()
                listOf<Component>(
                    Component.text()
                        .append(Component.text(entry.factory.type.replaceFirstChar { c -> c.titlecase() }
                            .padEnd(maxEntryTypeLength, ' '), NamedTextColor.GOLD))
                        .append(Component.text(" Entry: ", NamedTextColor.BLUE))
                        .append(Component.text(entry.toString(), NamedTextColor.GREEN))
                        .append(
                            Component.text()
                                .append(Component.text(" [", NamedTextColor.GRAY))
                                .append(
                                    Component.text("remove", NamedTextColor.RED)
                                        .clickEvent(ClickEvent.runCommand("/whitelist ${whitelist.name} remove $entry"))
                                        .hoverEvent(
                                            HoverEvent.showText(
                                                Component.text()
                                                    .append(Component.text("Remove entry ", NamedTextColor.RED))
                                                    .append(Component.text(entry.toString(), NamedTextColor.YELLOW))
                                            )
                                        )
                                )
                                .append(Component.text("]", NamedTextColor.GRAY))
                        )
                        .build()
                )
            }
        ) { p -> "/whitelist ${whitelist.name} list $p" }.render(whitelist.entries, page).forEach(src::sendMessage)

        return 0
    }

    private val UNKNOWN_WHITELIST = DynamicCommandExceptionType{ arg -> VelocityBrigadierMessage.tooltip(Component.text("Selected ($arg) whitelist doesn't exists", NamedTextColor.RED)) }
    private fun getWhitelist(whitelistName: String): Whitelist {
        val whitelist = ReWhitelist.getWhitelist(whitelistName)

        if (whitelist == null) throw UNKNOWN_WHITELIST.create(whitelistName)

        return whitelist
    }
}
