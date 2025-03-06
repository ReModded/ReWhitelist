package dev.remodded.rewhitelist.entries

import com.google.gson.JsonObject
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import net.kyori.adventure.text.Component

abstract class Entry(factory: Factory<*>) {
    @Suppress("UNCHECKED_CAST")
    val factory: Factory<Entry> = factory as Factory<Entry>

    abstract fun match(player: Player): Boolean

    abstract class Factory<T: Entry> {
        abstract val type: String

        abstract fun load(data: JsonObject): T
        abstract fun save(entry: T): JsonObject

        abstract fun getCommandNode(entryConsumer: (CommandContext<CommandSource>, T) -> Unit): ArgumentBuilder<CommandSource, *>

        open fun getHelp(): List<Component> {
            return listOf(
                Component.text("<$type>")
            )
        }
    }
}




