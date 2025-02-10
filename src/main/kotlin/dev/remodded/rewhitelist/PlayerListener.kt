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
        if (isPlayerAllowed(ev.player))
            return

        ev.result = ReWhitelist.config.denied
    }

    @Subscribe(priority = (-1).toShort())
    fun onPlayerChangeServer(ev: ServerPreConnectEvent) {
        val server = ev.result.server.getOrNull()
        if (server == null) // Some other plugin already prevented the connection
            return

        if (isPlayerAllowed(ev.player))
            return

        ev.result = ServerPreConnectEvent.ServerResult.denied()
    }

    private fun isPlayerAllowed(player: Player): Boolean {
        if (!ReWhitelist.whitelists[0].enabled) 
            return true
        
        for (whitelist in ReWhitelist.whitelists)
            if (whitelist.enabled && whitelist.isPlayerAllowed(player)) {
                ReWhitelist.logger.info("[Player Allowed] ${player.username} allowed by whitelist ${whitelist.name}")
                return true
            }
        
        return false
    }

//    private fun isPlayerAllowed(player: Player): Boolean {
//        var anyWasEnabled = false
//        for (whitelist in ReWhitelist.whitelists) {
//            if(whitelist.enabled) {
//                anyWasEnabled = true
//                if(whitelist.isPlayerAllowed(player)) {
//                    ReWhitelist.logger.info("[Player Allowed] ${player.username} allowed by whitelist ${whitelist.name}")
//                    return true
//                }
//            }
//        }
//
//        return !anyWasEnabled
//    }
}
