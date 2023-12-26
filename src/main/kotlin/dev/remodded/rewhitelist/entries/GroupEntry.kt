package dev.remodded.rewhitelist.entries

import com.moandjiezana.toml.Toml
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import com.mojang.brigadier.context.CommandContext
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player


class GroupEntry private constructor(factory: Entry.Factory<*>, val group: String) : Entry(factory) {
    override fun match(player: Player): Boolean {
        return player.hasPermission("group.$group")
    }

    override fun toString(): String {
        return group
    }


    object Factory : Entry.Factory<GroupEntry>() {
        override val type = "group"

        override fun save(entry: Entry): MutableMap<String, String> {
            return mutableMapOf("group" to (entry as GroupEntry).group)
        }

        override fun fromToml(toml: Toml): GroupEntry {
            return GroupEntry(this, toml.getString("group"))
        }

        override fun getCommandNode(entryConsumer: (CommandContext<CommandSource>, GroupEntry) -> Unit): ArgumentBuilder<CommandSource, *> {
            return argument<CommandSource, String>("group", StringArgumentType.string())
                .executes { ctx ->
                    entryConsumer(ctx, GroupEntry(this, StringArgumentType.getString(ctx, "group")))
                    0
                }
        }
    }
}
