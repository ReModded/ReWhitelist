package dev.remodded.rewhitelist.utils

import com.velocitypowered.api.util.UuidUtils
import dev.remodded.rewhitelist.ReWhitelist
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URI
import java.util.*
import kotlin.jvm.optionals.getOrNull

object OfflinePlayerUtils {
    fun getOfflinePlayerUUID(username: String): UUID? {
        val onlinePlayer = ReWhitelist.server.getPlayer(username).getOrNull()

        if (onlinePlayer != null)
            return onlinePlayer.uniqueId

        val stringUUID = try {
            val url = URI("https://api.mojang.com/users/profiles/minecraft/$username").toURL()
            val urlConnection = url.openConnection()
            BufferedReader(InputStreamReader(urlConnection.getInputStream())).use { reader ->
                val json = reader.readText()

                val index = json.indexOf("\"id\" : \"") + 8
                if (index < 0)
                    return null

                val endIndex = json.indexOf("\"", index)
                if (endIndex < 0)
                    return null

                json.substring(index, endIndex)
            }
        } catch (_: Exception) {
            return null
        }

        return UuidUtils.fromUndashed(stringUUID)
    }
}
