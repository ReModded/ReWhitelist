package dev.remodded.rewhitelist.command

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.command.CommandSource
import dev.remodded.rewhitelist.ReWhitelist
import dev.remodded.rewhitelist.Whitelist
import dev.remodded.rewhitelist.command.WhitelistManagementSubcommand.whitelistManagementSubcommand
import dev.remodded.rewhitelist.utils.CommandUtils
import dev.remodded.rewhitelist.utils.CommandUtils.argument
import dev.remodded.rewhitelist.utils.CommandUtils.literal
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

object WhitelistCommand {
    fun register() {
        val cm = ReWhitelist.Companion.server.commandManager
        val command = BrigadierCommand(createCommand())
        cm.register(cm.metaBuilder(command).plugin(ReWhitelist.Companion.plugin).build(), command)
    }

    private fun createCommand(): LiteralArgumentBuilder<CommandSource> {
        return literal("whitelist")
            .requires(CommandUtils.permissionRequirement("rewhitelist.command.whitelist"))
            .then(
                literal("reload")
                    .requires(CommandUtils.permissionRequirement("rewhitelist.command.whitelist.reload"))
                    .executes { ctx -> reload(ctx.source) }
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
                    .executes { ctx -> createNewWhitelistHelper(ctx.source) }
            )
            .then(
                argument("whitelist", StringArgumentType.string())
                    .whitelistManagementSubcommand { ctx -> StringArgumentType.getString(ctx, "whitelist") }
            )
            .whitelistManagementSubcommand { _ -> "default" }
            .executes { ctx -> help(ctx.source) }
    }

    private fun reload(src: CommandSource): Int {
        ReWhitelist.Companion.plugin.reload()
        src.sendMessage(Component.text("Whitelist has been reloaded", NamedTextColor.YELLOW))
        return 0
    }

    private fun help(src: CommandSource): Int {
        src.sendMessage(Component.text("/whitelist <reload/create/add/remove/list/on/off>", NamedTextColor.GRAY))
        src.sendMessage(Component.text("/whitelist [group] <add/remove/list/on/off>", NamedTextColor.GRAY))
        return 0
    }

    private fun createNewWhitelistHelper(src: CommandSource): Int {
        src.sendMessage(Component.text("/whitelist create <group>", NamedTextColor.GRAY))
        return 0
    }

    private fun createNewWhitelist(src: CommandSource, whitelistName: String): Int {
        var whitelist = ReWhitelist.Companion.getWhitelist(whitelistName)
        if(whitelist != null) {
            src.sendMessage(Component.text("Whitelist (${whitelist.name}) already exists", NamedTextColor.RED))
            return 1
        }

        whitelist = Whitelist.Companion.createNew(whitelistName)
        ReWhitelist.Companion.whitelists.add(whitelist)

        src.sendMessage(Component.text("Whitelist (${whitelist.name}) has been created", NamedTextColor.GREEN))
        return 0
    }
}
