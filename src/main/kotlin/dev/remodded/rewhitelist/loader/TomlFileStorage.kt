package dev.remodded.rewhitelist.loader

import com.google.gson.JsonObject
import com.moandjiezana.toml.Toml
import com.moandjiezana.toml.TomlWriter
import dev.remodded.rewhitelist.Config
import dev.remodded.rewhitelist.ReWhitelist
import dev.remodded.rewhitelist.Whitelist
import dev.remodded.rewhitelist.utils.ordinal
import java.io.File

class TomlFileStorage(
    config: Config.Storage,
) : FileStorage(config) {

    override fun save(whitelist: Whitelist) {
        val file = directory.resolve("${whitelist.name}.toml").toFile()

        TomlWriter().write(mapOf(
            "enabled" to whitelist.enabled,
            "servers" to whitelist.servers,
            "whitelist" to WhitelistStorage.serializeEntries(whitelist),
        ), file)
    }

    override fun loadFromFile(file: File): Whitelist  {
        val toml = Toml().read(file)

        val whitelist = Whitelist(
            file.nameWithoutExtension,
            toml.getBoolean("enabled", false),
        )

        whitelist.servers.addAll(toml.getList<String>("servers", emptyList()))
        whitelist.entries.addAll(toml.getTables("whitelist")?.mapIndexedNotNull { i, it ->
            try {
                val entryData = it.to(JsonObject::class.java)
                val type = (entryData["type"] ?: return@mapIndexedNotNull null).asString
                val factory = ReWhitelist.entryRegistry.get(type) ?: return@mapIndexedNotNull null
                factory.load(entryData)
            } catch (ex: Exception) {
                ReWhitelist.logger.error("Unable to read whitelist ${i.ordinal()} entry \n(${it.entrySet().joinToString("\n") { e -> "${e.key}:${e.value}" }})")
                ex.printStackTrace()
                null
            }
        } ?: emptyList())

        return whitelist
    }
}
