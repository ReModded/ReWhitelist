package dev.remodded.rewhitelist.utils

import com.velocitypowered.api.util.UuidUtils
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URI
import java.util.*

object OfflinePlayerUtils {
    fun getOfflinePlayerUUID(username: String): UUID? {
        try {
            val url = URI("https://api.mojang.com/users/profiles/minecraft/$username").toURL()
            val urlConnection = url.openConnection()
            BufferedReader(InputStreamReader(urlConnection.getInputStream())).use { reader ->
                val json = reader.readText()
                val index = json.indexOf("\"id\" : \"") + 8
                val string = json.substring(index, json.indexOf("\"", index))
                return UuidUtils.fromUndashed(string)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return null
    }
}
