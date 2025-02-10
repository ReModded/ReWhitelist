package dev.remodded.rewhitelist

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import dev.remodded.rewhitelist.command.WhitelistCommand
import dev.remodded.rewhitelist.entries.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Path
import kotlin.io.path.createDirectory


@Plugin(id = "rewhitelist")
class ReWhitelist @Inject constructor(
    @DataDirectory val configDirectory: Path,
    server: ProxyServer
) {
    init {
        logger = LoggerFactory.getLogger("ReWhitelist")
        Companion.server = server
        plugin = this
    }

    @Subscribe
    fun onProxyInitialization(ev: ProxyInitializeEvent) {
        config = Config.load(configDirectory)

        registerBuiltinEntries()

        loadAllWhitelists()

        server.eventManager.register(plugin, PlayerListener)
        WhitelistCommand.register()
    }

    fun reload() {
        config = Config.load(configDirectory)
        loadAllWhitelists()
    }

    @Subscribe
    fun onProxyShutdown(ev: ProxyShutdownEvent) {
        logger.info("Bye!")
    }

    private fun registerBuiltinEntries() {
        entryRegistry.register(UUIDEntry.Factory)
        entryRegistry.register(NickEntry.Factory)
        entryRegistry.register(RegexEntry.Factory)
        entryRegistry.register(GroupEntry.Factory)
        entryRegistry.register(PermissionEntry.Factory)
    }

    private fun loadAllWhitelists() {
        val directory = File("whitelists")
        try { directory.toPath().createDirectory() } catch (_: FileAlreadyExistsException) {}

        whitelists.clear()
        logger.info("Loading whitelists")

        val defaultFile = File(directory, "default.toml")
        if(defaultFile.exists())
            addWhitelistFromFile(defaultFile)
        else {
            logger.info("Generating default whitelist")
            whitelists.add(Whitelist.createNew("default"))
        }

        directory.listFiles { f -> f.isFile && f.extension == "toml" && f.nameWithoutExtension != "default" }?.forEach(this::addWhitelistFromFile)
    }

    private fun addWhitelistFromFile(file: File) {
        val whitelist = file.nameWithoutExtension
        whitelists.add(Whitelist.load(whitelist))
    }

    companion object {
        lateinit var plugin: ReWhitelist
        lateinit var logger: Logger
        lateinit var server: ProxyServer
        lateinit var config: Config

        val whitelists = mutableListOf<Whitelist>()
        val entryRegistry = EntryRegistry()

        fun getWhitelist(whitelistName: String): Whitelist? {
            return whitelists.firstOrNull { w -> w.name.equals(whitelistName, true) }
        }
    }
}
