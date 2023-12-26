package dev.remodded.rewhitelist.entries

import com.moandjiezana.toml.Toml
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import com.mojang.brigadier.context.CommandContext
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player

class NickEntry private constructor(factory: Entry.Factory<*>, val nick: String) : Entry(factory) {
    override fun match(player: Player): Boolean {
        return player.username == nick
    }

    override fun toString(): String {
        return nick
    }


    object Factory : Entry.Factory<NickEntry>() {
        override val type = "nick"

        override fun save(entry: Entry): MutableMap<String, String> {
            return mutableMapOf("nick" to (entry as NickEntry).nick)
        }

        override fun fromToml(toml: Toml): NickEntry {
            return NickEntry(this, toml.getString("nick"))
        }

        override fun getCommandNode(entryConsumer: (CommandContext<CommandSource>, NickEntry) -> Unit): ArgumentBuilder<CommandSource, *> {
            return argument<CommandSource, String>("nick", StringArgumentType.string())
                .executes { ctx ->
                    entryConsumer(ctx, NickEntry(this, StringArgumentType.getString(ctx, "nick")))
                    0
                }
        }
    }
}
