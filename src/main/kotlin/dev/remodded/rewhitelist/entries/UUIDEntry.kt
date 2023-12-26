package dev.remodded.rewhitelist.entries

import com.moandjiezana.toml.Toml
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import com.mojang.brigadier.context.CommandContext
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import java.util.*


class UUIDEntry private constructor(factory: Entry.Factory<*>, private val uuid: UUID): Entry(factory) {
    override fun match(player: Player): Boolean {
        return player.uniqueId == uuid
    }

    override fun toString(): String {
        return uuid.toString()
    }


    object Factory: Entry.Factory<UUIDEntry>() {
        override val type = "uuid"

        override fun save(entry: Entry): MutableMap<String, String> {
            return mutableMapOf("uuid" to (entry as UUIDEntry).uuid.toString())
        }

        override fun fromToml(toml: Toml): UUIDEntry {
            return UUIDEntry(this, UUID.fromString(toml.getString("uuid")))
        }

        override fun getCommandNode(entryConsumer: (CommandContext<CommandSource>, UUIDEntry) -> Unit): ArgumentBuilder<CommandSource, *> {
            return argument<CommandSource, String>("uuid", StringArgumentType.string())
                .executes { ctx ->
                    entryConsumer(ctx, UUIDEntry(this, UUID.fromString(StringArgumentType.getString(ctx, "uuid"))))
                    0
                }
        }
    }
}
