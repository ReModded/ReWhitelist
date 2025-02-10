package dev.remodded.rewhitelist

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.LoginEvent
import com.velocitypowered.api.event.player.ServerPreConnectEvent
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.server.RegisteredServer
import kotlin.jvm.optionals.getOrNull


object PlayerListener {
    @Subscribe
    fun onPlayerJoin(ev: LoginEvent) {
        if (isPlayerAllowed(ev.player, null))
            return

        ev.result = ReWhitelist.config.denied
    }

    @Subscribe(priority = (-1).toShort())
    fun onPlayerChangeServer(ev: ServerPreConnectEvent) {
        val server = ev.result.server.getOrNull()
        if (server == null) // Some other plugin already prevented the connection
            return

        if (isPlayerAllowed(ev.player, server))
            return

        ev.result = ServerPreConnectEvent.ServerResult.denied()
    }

    private fun isPlayerAllowed(player: Player, server: RegisteredServer?): Boolean {
        if (!ReWhitelist.whitelists[0].enabled)
            return true

        for (whitelist in ReWhitelist.whitelists) {
            // If the whitelist is disabled, skip it
            if (!whitelist.enabled)
                continue

            // If the whitelist has server restrictions, skip it if the server is not in the list
            if (server != null && whitelist.servers.isNotEmpty() && !whitelist.servers.contains(server.serverInfo.name))
                continue

            // If the whitelist is enabled and the player is allowed, return true
            if (whitelist.isPlayerAllowed(player)) {
                if (server != null)
                    ReWhitelist.logger.info("[Player Allowed] ${player.username} allowed by whitelist ${whitelist.name}")
                return true
            }
        }

        return false
    }
}
