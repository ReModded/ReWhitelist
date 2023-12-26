package dev.remodded.rewhitelist.entries

class EntryRegistry {

    private val registeredEntryFactories = mutableMapOf<String, Entry.Factory<*>>()


    fun register(entry: Entry.Factory<*>) {
        registeredEntryFactories[entry.type] = entry
    }

    fun get(type: String): Entry.Factory<*>? {
        return registeredEntryFactories[type]
    }

    fun getAll(): Map<String, Entry.Factory<*>> {
        return registeredEntryFactories
    }
}
