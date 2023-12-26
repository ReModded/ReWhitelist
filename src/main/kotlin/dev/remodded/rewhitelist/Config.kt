package dev.remodded.rewhitelist

import com.moandjiezana.toml.Toml
import com.moandjiezana.toml.TomlWriter
import com.velocitypowered.api.event.ResultedEvent
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Path
import kotlin.io.path.createDirectory

class Config(config: Toml) {

    val denied: ResultedEvent.ComponentResult = ResultedEvent.ComponentResult.denied(
        LegacyComponentSerializer.legacyAmpersand().deserialize(config.getString("messages.deny", "&cYou're not invited to the party..."))
    )


    companion object {
        fun load(configDirectory: Path): Config {
            try {
                val configFile = configDirectory.resolve("config.toml").toFile()
                try {
                    configDirectory.createDirectory()
                } catch (_: FileAlreadyExistsException) {}
                
                if (configFile.createNewFile())
                   TomlWriter().write(mapOf(
                       "messages" to mapOf(
                           "deny" to "&cYou're not invited to the party..."
                       )
                   ), configFile)

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
