package dev.remodded.rewhitelist.entries

import com.google.gson.JsonObject
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import com.mojang.brigadier.context.CommandContext
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player


class GroupEntry private constructor(factory: Factory, val group: String) : Entry(factory) {
    override fun match(player: Player): Boolean {
        return player.hasPermission("group.$group")
    }

    override fun toString(): String {
        return group
    }


    object Factory : Entry.Factory<GroupEntry>() {
        override val type = "group"

        override fun save(entry: GroupEntry): JsonObject {
            return JsonObject().apply {
                addProperty("group", entry.group)
            }
        }

        override fun load(data: JsonObject): GroupEntry {
            return GroupEntry(this, data.getAsJsonPrimitive("group").asString)
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
