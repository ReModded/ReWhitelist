package dev.remodded.rewhitelist.command

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.velocitypowered.api.command.CommandSource
import dev.remodded.rewhitelist.ReWhitelist
import dev.remodded.rewhitelist.Whitelist
import dev.remodded.rewhitelist.command.WhitelistSettingsSubcommand.whitelistSettingsSubcommand
import dev.remodded.rewhitelist.entries.Entry
import dev.remodded.rewhitelist.utils.CommandUtils
import dev.remodded.rewhitelist.utils.CommandUtils.argument
import dev.remodded.rewhitelist.utils.CommandUtils.literal
import net.kyori.adventure.text.Component
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
                    for (entryFactory in ReWhitelist.Companion.entryRegistry.getAll().values)
                        then(
                            literal(entryFactory.type)
                                .then(entryFactory.getCommandNode { ctx, entry ->
                                    addWhitelistEntry(ctx.source, whitelistNameResolver(ctx), entry)
                                })
                        )
                }
                .executes { ctx ->
                    ctx.source.sendMessage(Component.text("/whitelist [group] add <type> <value>", NamedTextColor.GRAY))
                    0
                }
        )
        .then(
            literal("remove")
                .requires(CommandUtils.permissionRequirement("rewhitelist.command.whitelist.remove"))
                .then(
                    argument("entry", StringArgumentType.string())
                        .executes { ctx ->
                            removeWhitelistEntry(ctx.source, whitelistNameResolver(ctx), StringArgumentType.getString(ctx, "entry"))
                        }
                )
        )
        .then(
            literal("on")
                .requires(CommandUtils.permissionRequirement("rewhitelist.command.whitelist.on"))
                .executes { ctx ->
                    switchWhitelist(ctx.source, whitelistNameResolver(ctx), true)
                }
        )
        .then(
            literal("off")
                .requires(CommandUtils.permissionRequirement("rewhitelist.command.whitelist.off"))
                .executes { ctx ->
                    switchWhitelist(ctx.source, whitelistNameResolver(ctx), false)
                }
        )
        .then(
            literal("list")
                .requires(CommandUtils.permissionRequirement("rewhitelist.command.whitelist.list"))
                .then(
                    argument("page", IntegerArgumentType.integer(1))
                        .executes { ctx ->
                            sendWhitelist(ctx.source, whitelistNameResolver(ctx), IntegerArgumentType.getInteger(ctx, "page"))
                        }
                )
                .executes { ctx ->
                    sendWhitelist(ctx.source, whitelistNameResolver(ctx), 1)
                }
        )
        .then(whitelistSettingsSubcommand(whitelistResolver))
    }

    private fun addWhitelistEntry(src: CommandSource, whitelistName: String, entry: Entry) {
        val whitelist = getWhitelist(src, whitelistName) ?: return
        whitelist.entries.add(entry)
        whitelist.save()

        src.sendMessage(Component.text("New ${entry.factory.type} entry has been added to whitelist (${whitelist.name})", NamedTextColor.GREEN))
    }

    private fun removeWhitelistEntry(src: CommandSource, whitelistName: String, entryValue: String): Int {
        val whitelist = getWhitelist(src, whitelistName) ?: return 1

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

    private fun switchWhitelist(src: CommandSource, whitelistName: String, enable: Boolean): Int {
        val whitelist = getWhitelist(src, whitelistName) ?: return 1

        if (whitelist.enabled == enable) {
            src.sendMessage(Component.text("Whitelist (${whitelist.name}) was already " + if(enable) "enabled" else "disabled", NamedTextColor.YELLOW))
            return 0
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

    private fun sendWhitelist(src: CommandSource, whitelistName: String, page: Int): Int {
        val whitelist = getWhitelist(src, whitelistName) ?: return 1
        val maxEntryTypeLength = ReWhitelist.Companion.entryRegistry.getAll().keys.maxOfOrNull { t -> t.length } ?: 4

        Pagination.builder().width(50).build(
            Component.text()
                .append(Component.text("Whitelist "))
                .append(Component.text("[", NamedTextColor.GRAY))
                .apply {
                    if(whitelist.enabled)
                        it.append(Component.text("on", NamedTextColor.GREEN))
                    else
                        it.append(Component.text("off", NamedTextColor.RED))
                }
                .append(Component.text("]", NamedTextColor.GRAY))
                .build(),
            Pagination.Renderer.RowRenderer<Entry> { entry, _ ->
                if(entry == null)
                    return@RowRenderer emptyList()
                listOf<Component>(
                    Component.text()
                        .append(Component.text(entry.factory.type.replaceFirstChar { c -> c.titlecase() }.padEnd(maxEntryTypeLength, ' ') , NamedTextColor.GOLD))
                        .append(Component.text(" Entry: ", NamedTextColor.BLUE))
                        .append(Component.text(entry.toString(), NamedTextColor.GREEN))
                        .append(
                            Component.text()
                                .append(Component.text(" [", NamedTextColor.GRAY))
                                .append(
                                    Component.text("remove", NamedTextColor.RED)
                                        .clickEvent(ClickEvent.runCommand("/whitelist remove $entry"))
                                        .hoverEvent(
                                            HoverEvent.showText(
                                                Component.text()
                                                    .append(Component.text("Remove entry ", NamedTextColor.RED))
                                                    .append(Component.text(entry.toString(), NamedTextColor.YELLOW))
                                            )))
                                .append(Component.text("]", NamedTextColor.GRAY))
                        )
                        .build()
                )
            }
        ) { p -> "/whitelist list $p" }.render(whitelist.entries, page).forEach(src::sendMessage)

        return 0
    }

    private val UNKNOWN_WHITELIST = DynamicCommandExceptionType{ arg -> VelocityBrigadierMessage.tooltip(Component.text("Selected ($arg) whitelist doesn't exists", NamedTextColor.RED)) }
    private fun getWhitelist(whitelistName: String): Whitelist {
        val whitelist = ReWhitelist.getWhitelist(whitelistName)

        if (whitelist == null) throw UNKNOWN_WHITELIST.create(whitelistName)

        return whitelist
    }
}
