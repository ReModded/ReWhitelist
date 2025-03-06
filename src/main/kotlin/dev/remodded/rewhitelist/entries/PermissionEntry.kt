package dev.remodded.rewhitelist.entries

import com.google.gson.JsonObject
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import com.mojang.brigadier.context.CommandContext
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player


class PermissionEntry private constructor(factory: Factory, private val permission: String) : Entry(factory) {
    override fun match(player: Player): Boolean {
        return player.hasPermission(permission)
    }

    override fun toString(): String {
        return permission
    }


    object Factory : Entry.Factory<PermissionEntry>() {
        override val type = "permission"

        override fun save(entry: PermissionEntry): JsonObject {
            return JsonObject().apply {
                addProperty("permission", entry.permission)
            }
        }

        override fun load(data: JsonObject): PermissionEntry {
            return PermissionEntry(
                this,
                data["permission"].asString,
            )
        }

        override fun getCommandNode(entryConsumer: (CommandContext<CommandSource>, PermissionEntry) -> Unit): ArgumentBuilder<CommandSource, *> {
            return argument<CommandSource, String>("permission", StringArgumentType.string())
                .executes { ctx ->
                    entryConsumer(ctx, PermissionEntry(this, StringArgumentType.getString(ctx, "permission")))
                    0
                }
        }
    }
}
