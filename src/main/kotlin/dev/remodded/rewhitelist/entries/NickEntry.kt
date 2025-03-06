package dev.remodded.rewhitelist.entries

import com.google.gson.JsonObject
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import com.mojang.brigadier.context.CommandContext
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import dev.remodded.rewhitelist.ReWhitelist
import dev.remodded.rewhitelist.Whitelist
import dev.remodded.rewhitelist.utils.OfflinePlayerUtils
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import java.util.*

class NickEntry private constructor(factory: Factory, val nick: String, var uuid: UUID?) : Entry(factory) {

    var validUUID = true

    override fun match(player: Player): Boolean {
        if (ReWhitelist.config.useNicksUUIDs && validUUID) {
            if (uuid == null) {
                uuid = OfflinePlayerUtils.getOfflinePlayerUUID(nick)
                ReWhitelist.whitelists.forEach(Whitelist::save)
            }

            if (uuid != null)
                return player.uniqueId == uuid
            else
                validUUID = false
        }

        return player.username == nick
    }

    override fun toString(): String {
        return nick
    }


    object Factory : Entry.Factory<NickEntry>() {
        override val type = "nick"

        override fun save(entry: NickEntry): JsonObject {
            return JsonObject().apply {
                addProperty("nick", entry.nick)
                addProperty("uuid", entry.uuid?.toString())
            }
        }

        override fun load(data: JsonObject): NickEntry {
            return NickEntry(
                this,
                data["nick"].asString,
                data["uuid"]?.let { UUID.fromString(it.asString) },
            )
        }

        override fun getCommandNode(entryConsumer: (CommandContext<CommandSource>, NickEntry) -> Unit): ArgumentBuilder<CommandSource, *> {
            return argument<CommandSource, String>("nick", StringArgumentType.string())
                .executes { ctx ->
                    val nick = StringArgumentType.getString(ctx, "nick")
                    var uuid: UUID? = null

                    if (ReWhitelist.server.configuration.isOnlineMode) {
                        uuid = OfflinePlayerUtils.getOfflinePlayerUUID(nick)
                        if (uuid == null)
                            ctx.source.sendMessage(Component.text("Couldn't find uuid for nick '$nick'. Make sure that that it's valid online-mode username.", NamedTextColor.YELLOW))
                    }

                    entryConsumer(ctx, NickEntry(this, nick, uuid))
                    0
                }
        }
    }
}
