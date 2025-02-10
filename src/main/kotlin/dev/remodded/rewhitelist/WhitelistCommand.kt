package dev.remodded.rewhitelist

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.command.CommandSource
import dev.remodded.rewhitelist.entries.Entry
import dev.remodded.rewhitelist.utils.CommandUtils
import dev.remodded.rewhitelist.utils.CommandUtils.argument
import dev.remodded.rewhitelist.utils.CommandUtils.literal
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.feature.pagination.Pagination
import net.kyori.adventure.text.format.NamedTextColor

object WhitelistCommand {
    fun register() {
        val cm = ReWhitelist.server.commandManager
        val command = BrigadierCommand(createCommand())
        cm.register(cm.metaBuilder(command).plugin(ReWhitelist.plugin).build(), command)
    }

    private fun createCommand(): LiteralArgumentBuilder<CommandSource> {
        return literal("whitelist")
            .requires(CommandUtils.permissionRequirement("rewhitelist.command.whitelist"))
            .then(
                literal("reload")
                    .requires(CommandUtils.permissionRequirement("rewhitelist.command.whitelist.reload"))
                    .executes { ctx ->
                        ReWhitelist.plugin.reload()
                        ctx.source.sendMessage(Component.text("Whitelist has been reloaded", NamedTextColor.YELLOW))
                        0
                    }
            )
            .then(
                literal("create")
                    .requires(CommandUtils.permissionRequirement("rewhitelist.command.whitelist.create"))
                    .then(
                        argument("whitelist", StringArgumentType.string())
                            .executes { ctx ->
                                createNewWhitelist(ctx.source, StringArgumentType.getString(ctx, "whitelist"))
                            }
                    )
                    .executes { ctx ->
                        ctx.source.sendMessage(Component.text("/whitelist create <group>", NamedTextColor.GRAY))
                        0
                    }
            )
            .then(
                argument("whitelist", StringArgumentType.string())
                    .addWhitelistManagementNodes { ctx -> StringArgumentType.getString(ctx, "whitelist") }
            )
            .addWhitelistManagementNodes { _ -> "default" }
            .executes { ctx ->
                ctx.source.sendMessage(Component.text("/whitelist <reload/create/add/remove/list/on/off>", NamedTextColor.GRAY))
                ctx.source.sendMessage(Component.text("/whitelist [group] <add/remove/list/on/off>", NamedTextColor.GRAY))
                0
            }
    }

    private fun <T: ArgumentBuilder<CommandSource, T>> T.addWhitelistManagementNodes(whitelistNameResolver: (CommandContext<CommandSource>)->String): T {
        return then(
            literal("add")
                .requires(CommandUtils.permissionRequirement("rewhitelist.command.whitelist.add"))
                .apply {
                    for (entryFactory in ReWhitelist.entryRegistry.getAll().values)
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
    }

    private fun createNewWhitelist(src: CommandSource, whitelistName: String): Int {
        var whitelist = ReWhitelist.getWhitelist(whitelistName)
        if(whitelist != null) {
            src.sendMessage(Component.text("Whitelist (${whitelist.name}) already exists", NamedTextColor.RED))
            return 1
        }

        whitelist = Whitelist.createNew(whitelistName)
        ReWhitelist.whitelists.add(whitelist)

        src.sendMessage(Component.text("Whitelist (${whitelist.name}) has been created", NamedTextColor.GREEN))
        return 0
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
        val maxEntryTypeLength = ReWhitelist.entryRegistry.getAll().keys.maxOfOrNull { t -> t.length } ?: 4

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
                        .append(Component.text()
                            .append(Component.text(" [", NamedTextColor.GRAY))
                            .append(Component.text("remove", NamedTextColor.RED)
                                .clickEvent(ClickEvent.runCommand("/whitelist remove $entry"))
                                .hoverEvent(
                                    HoverEvent.showText(Component.text()
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

    private fun getWhitelist(src: CommandSource, whitelistName: String): Whitelist? {
        val whitelist = ReWhitelist.getWhitelist(whitelistName)

        if (whitelist == null)
            src.sendMessage(Component.text("Selected ($whitelistName) whitelist doesn't exists", NamedTextColor.RED))

        return whitelist
    }
}
