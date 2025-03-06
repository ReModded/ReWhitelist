package dev.remodded.rewhitelist

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.permission.PermissionsSetupEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.permission.PermissionFunction
import com.velocitypowered.api.permission.PermissionProvider
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import dev.remodded.rewhitelist.command.WhitelistCommand
import dev.remodded.rewhitelist.entries.*
import dev.remodded.rewhitelist.loader.TomlFileStorage
import dev.remodded.rewhitelist.loader.WhitelistStorage
import dev.remodded.rewhitelist.utils.OfflinePlayerUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path


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

    lateinit var storage: WhitelistStorage

    @Subscribe
    fun onProxyInitialization(ev: ProxyInitializeEvent) {
        config = Config.load(configDirectory)
        initStorage()
        OfflinePlayerUtils.reload()

        registerBuiltinEntries()

        loadAllWhitelists()

        server.eventManager.register(plugin, PlayerListener)
        WhitelistCommand.register()
    }

    @Subscribe
    fun permission(ev: PermissionsSetupEvent) {
        if (config.debug)
            ev.provider = PermissionProvider{ s -> PermissionFunction.ALWAYS_TRUE }
    }

    fun reload() {
        config = Config.load(configDirectory)
        initStorage()
        OfflinePlayerUtils.reload()

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
        entryRegistry.register(IPEntry.Factory)
    }

    private fun initStorage() {
        storage = when (config.storage.type) {
            WhitelistStorage.Type.TOML_FILE -> TomlFileStorage(config.storage)
            else -> throw IllegalStateException("Unknown storage type")
        }
    }

    private fun loadAllWhitelists() {
        whitelists.clear()
        logger.info("Loading whitelists")

        val (default, otherWhitelists) = storage.load().partition { it.name == "default" }

        // Make sure there is always a default whitelist at index 0
        if (default.isNotEmpty())
            whitelists.add(default.first())
        else {
            logger.info("Generating default whitelist")
            val defaultWhitelist = Whitelist("default")
            defaultWhitelist.save()
            whitelists.add(defaultWhitelist)
        }

        whitelists.addAll(otherWhitelists)
    }

    companion object {
        lateinit var plugin: ReWhitelist
        lateinit var logger: Logger
        lateinit var server: ProxyServer
        lateinit var config: Config

        val defaultWhitelist get() = whitelists[0]
        val whitelists = mutableListOf<Whitelist>()
        val entryRegistry = EntryRegistry()

        fun getWhitelist(whitelistName: String): Whitelist? {
            return whitelists.firstOrNull { w -> w.name.equals(whitelistName, true) }
        }
    }
}
