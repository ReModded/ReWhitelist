package dev.remodded.rewhitelist.utils

import com.google.common.cache.CacheBuilder
import com.velocitypowered.api.util.UuidUtils
import dev.remodded.rewhitelist.ReWhitelist
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URI
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.jvm.optionals.getOrNull

object OfflinePlayerUtils {

    private val NULL_UUID = UUID(0, 0)
    private var uuidCache: MutableMap<String, UUID> = createCache()

    fun reload() {
        uuidCache = createCache()
    }

    fun getOfflinePlayerUUID(username: String): UUID? {
        val username = username.lowercase()

        if (uuidCache.containsKey(username)) {
            val cached = uuidCache[username]
            return if (cached == NULL_UUID)
                null
            else
                cached
        }

        val onlinePlayer = ReWhitelist.server.getPlayer(username).getOrNull()

        if (onlinePlayer != null) {
            uuidCache.put(username, onlinePlayer.uniqueId)
            return onlinePlayer.uniqueId
        }

        val stringUUID = try {
            val url = URI("https://api.mojang.com/users/profiles/minecraft/$username").toURL()
            val urlConnection = url.openConnection()
            BufferedReader(InputStreamReader(urlConnection.getInputStream())).use { reader ->
                val json = reader.readText()

                val index = json.indexOf("\"id\" : \"") + 8
                if (index < 0)
                    return@use null

                val endIndex = json.indexOf("\"", index)
                if (endIndex < 0)
                    return@use null

                json.substring(index, endIndex)
            }
        } catch (_: Exception) {
            null
        }

        val result = stringUUID?.let(UuidUtils::fromUndashed)

        uuidCache.put(username, result ?: NULL_UUID)

        return result
    }

    private fun createCache(): MutableMap<String, UUID> {
        return CacheBuilder.newBuilder()
            .expireAfterWrite(ReWhitelist.config.uuidCacheDuration, TimeUnit.SECONDS)
            .build<String, UUID>().asMap()
    }
}
