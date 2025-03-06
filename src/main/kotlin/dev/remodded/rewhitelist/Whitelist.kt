package dev.remodded.rewhitelist

import com.velocitypowered.api.proxy.Player
import dev.remodded.rewhitelist.entries.Entry

class Whitelist(
    val name: String,
    enabled: Boolean = false,
) {
    var enabled: Boolean = enabled
        private set

    val entries = mutableListOf<Entry>()
    val servers: MutableSet<String> = hashSetOf()

    fun enable() {
        enabled = true
        save()
    }

    fun disable() {
        enabled = false
        save()
    }

    fun isPlayerAllowed(player: Player): Boolean {
        return entries.any { e -> e.match(player) }
    }

    fun save() {
        ReWhitelist.plugin.storage.save(this)
    }
}
