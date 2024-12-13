package dev.remodded.rewhitelist.entries

import com.moandjiezana.toml.Toml
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import com.mojang.brigadier.context.CommandContext
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import dev.remodded.rewhitelist.ReWhitelist
import dev.remodded.rewhitelist.Whitelist
import dev.remodded.rewhitelist.utils.OfflinePlayerUtils
import java.util.*

class NickEntry private constructor(factory: Entry.Factory<*>, val nick: String, var uuid: UUID?) : Entry(factory) {
    override fun match(player: Player): Boolean {
        if (ReWhitelist.config.useNicksUUIDs) {
            if (uuid == null) {
                uuid = OfflinePlayerUtils.getOfflinePlayerUUID(nick)
                ReWhitelist.whitelists.forEach(Whitelist::save)
            }

            if (uuid != null)
                return player.uniqueId == uuid
        }

        return player.username == nick
    }

    override fun toString(): String {
        return nick
    }


    object Factory : Entry.Factory<NickEntry>() {
        override val type = "nick"

        override fun save(entry: Entry): MutableMap<String, String> {
            entry as NickEntry

            val map = mutableMapOf(
                "nick" to entry.nick,
            )

            if (ReWhitelist.server.configuration.isOnlineMode && entry.uuid != null)
                map["uuid"] = entry.uuid.toString()

            return map
        }

        override fun fromToml(toml: Toml): NickEntry {
            return NickEntry(this, toml.getString("nick"), toml.getString("uuid", null)?.let { UUID.fromString(it) })
        }

        override fun getCommandNode(entryConsumer: (CommandContext<CommandSource>, NickEntry) -> Unit): ArgumentBuilder<CommandSource, *> {
            return argument<CommandSource, String>("nick", StringArgumentType.string())
                .executes { ctx ->
                    val nick = StringArgumentType.getString(ctx, "nick")
                    var uuid: UUID? = null

                    if (ReWhitelist.server.configuration.isOnlineMode)
                        uuid = OfflinePlayerUtils.getOfflinePlayerUUID(nick)

                    entryConsumer(ctx, NickEntry(this, nick, uuid))
                    0
                }
        }
    }
}
