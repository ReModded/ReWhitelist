package dev.remodded.rewhitelist.entries

import com.moandjiezana.toml.Toml
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import net.kyori.adventure.text.Component

abstract class Entry(val factory: Factory<*>) {
    abstract fun match(player: Player): Boolean

    abstract class Factory<T: Entry> {
        abstract val type: String

        abstract fun save(entry: Entry): MutableMap<String, String>

        abstract fun fromToml(toml: Toml): T

        abstract fun getCommandNode(entryConsumer: (CommandContext<CommandSource>, T) -> Unit): ArgumentBuilder<CommandSource, *>

        open fun getHelp(): List<Component> {
            return listOf(
                Component.text("<$type>")
            )
        }
    }
}




