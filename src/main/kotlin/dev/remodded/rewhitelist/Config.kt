package dev.remodded.rewhitelist

import com.moandjiezana.toml.Toml
import com.velocitypowered.api.event.ResultedEvent
import dev.remodded.rewhitelist.loader.WhitelistStorage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import java.io.FileOutputStream
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Path
import kotlin.io.path.createDirectory

class Config(config: Toml) {

    val debug: Boolean = config.getBoolean("debug", false)

    val denied: ResultedEvent.ComponentResult = ResultedEvent.ComponentResult.denied(
        LegacyComponentSerializer.legacyAmpersand().deserialize(config.getString("messages.deny", "&cYou're not invited to the party..."))
    )

    val useNicksUUIDs: Boolean = config.getBoolean("useNicksUUIDs", true) && ReWhitelist.server.configuration.isOnlineMode

    val storage = Storage(config.getTable("storage") ?: Toml())

    val integrations = Integrations(config.getTable("integrations") ?: Toml())

    val uuidCacheDuration: Long = config.getLong("uuidCacheDuration", 600)


    class Storage(table: Toml) {
        val type = WhitelistStorage.Type.valueOf(table.getString("type", WhitelistStorage.Type.TOML_FILE.name))
        val directory: String = table.getString("directory", "whitelists")
    }

    class Integrations(table: Toml) {
        val floodgate: Boolean = table.getBoolean("floodgate", true) && ReWhitelist.server.pluginManager.isLoaded("floodgate")
    }

    companion object {
        fun load(configDirectory: Path): Config {
            try {
                val configFile = configDirectory.resolve("config.toml").toFile()
                try {
                    configDirectory.createDirectory()
                } catch (_: FileAlreadyExistsException) {}

                if (configFile.createNewFile())
                    Config::class.java.getResourceAsStream("/config.toml").use { input ->
                        FileOutputStream(configFile).use { output ->
                            input!!.copyTo(output)
                        }
                    }

                val config = Toml().read(configFile)

                return Config(config)

            } catch (ex: Exception) {
                ReWhitelist.logger.error("Problem with loading config")
                ex.printStackTrace()
                return Config(Toml())
            }
        }
    }
}
