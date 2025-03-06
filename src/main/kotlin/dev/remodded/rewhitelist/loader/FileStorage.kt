package dev.remodded.rewhitelist.loader

import dev.remodded.rewhitelist.Config
import dev.remodded.rewhitelist.Whitelist
import java.io.File
import kotlin.io.path.Path

abstract class FileStorage(
    config: Config.Storage,
) : WhitelistStorage {

    protected val directory = Path(config.directory)

    init {
        directory.toFile().mkdirs()
    }

    override fun load(): List<Whitelist> {
        return directory.toFile()
            .listFiles { f -> f.isFile && f.extension == "toml" }
            .map(::loadFromFile)
    }

    protected abstract fun loadFromFile(file: File): Whitelist
}
