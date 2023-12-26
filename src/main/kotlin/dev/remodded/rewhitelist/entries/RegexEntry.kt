package dev.remodded.rewhitelist.entries

import com.moandjiezana.toml.Toml
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import com.mojang.brigadier.context.CommandContext
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player


class RegexEntry(factory: Entry.Factory<*>, val regex: Regex) : Entry(factory) {
    override fun match(player: Player): Boolean {
        return regex.matches(player.username)
    }

    override fun toString(): String {
        return regex.toString()
    }


    object Factory : Entry.Factory<RegexEntry>() {
        override val type = "regex"

        override fun save(entry: Entry): MutableMap<String, String> {
            return mutableMapOf("regex" to (entry as RegexEntry).regex.toString())
        }

        override fun fromToml(toml: Toml): RegexEntry {
            return RegexEntry(this, Regex(toml.getString("regex")))
        }

        override fun getCommandNode(entryConsumer: (CommandContext<CommandSource>, RegexEntry) -> Unit): ArgumentBuilder<CommandSource, *> {
            return argument<CommandSource, String>("regex", StringArgumentType.string())
                .executes { ctx ->
                    entryConsumer(ctx, RegexEntry(this, Regex(StringArgumentType.getString(ctx, "regex"))))
                    0
                }
        }
    }
}
