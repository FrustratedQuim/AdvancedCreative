package com.ratger.acreative.commands.admin.npc

import java.util.Locale

class NpcProfileRegistry(
    private val storage: NpcJsonStorage
) {
    private val lock = Any()
    private val profilesByKey = linkedMapOf<String, NpcProfile>()
    private var dirty = false
    private var revision = 0L

    fun load() {
        val loadedProfiles = storage.loadAll()
        synchronized(lock) {
            profilesByKey.clear()
            loadedProfiles.forEach { profile ->
                profilesByKey[keyOf(profile.name)] = profile.copyDeep()
            }
            dirty = false
            revision = 0L
        }
    }

    fun contains(name: String): Boolean = synchronized(lock) {
        keyOf(name) in profilesByKey
    }

    fun find(name: String): NpcProfile? = synchronized(lock) {
        profilesByKey[keyOf(name)]?.copyDeep()
    }

    fun names(): List<String> = synchronized(lock) {
        profilesByKey.values.map { it.name }.sortedBy { it.lowercase(Locale.ROOT) }
    }

    fun snapshot(): List<NpcProfile> = synchronized(lock) {
        profilesByKey.values.map(NpcProfile::copyDeep)
    }

    fun create(profile: NpcProfile): Boolean = synchronized(lock) {
        val key = keyOf(profile.name)
        if (key in profilesByKey) {
            false
        } else {
            profilesByKey[key] = profile.copyDeep()
            dirty = true
            revision++
            true
        }
    }

    fun update(name: String, transform: (NpcProfile) -> NpcProfile): NpcProfile? = synchronized(lock) {
        val key = keyOf(name)
        val current = profilesByKey[key] ?: return null
        val updated = transform(current.copyDeep()).copyDeep()
        profilesByKey[key] = updated
        dirty = true
        revision++
        updated.copyDeep()
    }

    fun remove(name: String): NpcProfile? = synchronized(lock) {
        val removed = profilesByKey.remove(keyOf(name)) ?: return null
        dirty = true
        revision++
        removed.copyDeep()
    }

    fun flushIfDirty() {
        val pending = synchronized(lock) {
            if (!dirty) {
                return
            }
            profilesByKey.values.map(NpcProfile::copyDeep) to revision
        }
        storage.saveAll(pending.first)
        synchronized(lock) {
            if (revision == pending.second) {
                dirty = false
            }
        }
    }

    fun flushAll() {
        val snapshot = synchronized(lock) {
            profilesByKey.values.map(NpcProfile::copyDeep)
        }
        storage.saveAll(snapshot)
        synchronized(lock) {
            dirty = false
        }
    }

    private fun keyOf(name: String): String = name.trim().lowercase(Locale.ROOT)
}
