package dev.remodded.rewhitelist

import com.moandjiezana.toml.Toml
import com.moandjiezana.toml.TomlWriter
import com.velocitypowered.api.proxy.Player
import dev.remodded.rewhitelist.entries.Entry
import java.io.File

class Whitelist private constructor(val name: String) {

    var enabled: Boolean = false
        private set

    val entries = mutableListOf<Entry>()

    private val file = getWhitelistFile(name)

    constructor(name: String, toml: Toml) : this(name) {
        enabled = toml.getBoolean("enabled", false)
    }

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
        val entriesList = mutableListOf<Map<String, *>>()

        entries.forEach { entry ->
            val entryMap = entry.factory.save(entry)
            entryMap["type"] = entry.factory.type
            entriesList.add(entryMap)
        }

        TomlWriter().write(mapOf("enabled" to enabled, "whitelist" to entriesList), file)
    }

    companion object {
        fun createNew(whitelistName: String): Whitelist {
            getWhitelistFile(whitelistName).createNewFile()
            val whitelist = load(whitelistName)
            whitelist.save()
            return whitelist
        }

        fun load(whitelistName: String): Whitelist {
            val toml = Toml().read(getWhitelistFile(whitelistName))

            val whitelist = Whitelist(whitelistName, toml)

            toml.getTables("whitelist")?.forEachIndexed { i, entry ->
                try {
                    val type = entry.getString("type")
                    val factory = ReWhitelist.entryRegistry.get(type) ?: return@forEachIndexed
                    whitelist.entries.add(factory.fromToml(entry))
                } catch (ex: Exception) {
                    ReWhitelist.logger.error("Unable to read whitelist $i entry \n(${entry.entrySet().joinToString("\n") { e -> "${e.key}:${e.value}" }})")
                    ex.printStackTrace()
                }
            }

            return whitelist
        }

        private fun getWhitelistFile(name: String): File {
            return File("whitelists/$name.toml")
        }
    }
}
