package dev.remodded.rewhitelist.loader

import dev.remodded.rewhitelist.Whitelist

interface WhitelistStorage {
    fun save(whitelist: Whitelist)
    fun load(): List<Whitelist>


    companion object {
        fun serializeEntries(whitelist: Whitelist): List<Map<String, Any>> {
            return whitelist.entries.map { entry ->
                val entryMap = entry.factory.save(entry)
                entryMap.addProperty("type", entry.factory.type)

                entryMap.entrySet()
                    .filter { e -> e.value.isJsonPrimitive }
                    .associate { e -> e.key to e.value.asJsonPrimitive.let {
                        when {
                            it.isString -> it.asString
                            it.isBoolean -> it.asBoolean
                            it.isNumber -> it.asNumber
                            else -> throw IllegalStateException("Unknown primitive type ${it.javaClass.simpleName}")
                        }
                    } }
            }
        }
    }

    enum class Type {
        TOML_FILE,
//        MYSQL,
//        POSTGRES,
//        REDIS,
    }
}
