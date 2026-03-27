package com.ratger.acreative.commands.edit

import com.destroystokyo.paper.profile.PlayerProfile
import com.destroystokyo.paper.profile.ProfileProperty
import org.bukkit.Bukkit
import java.util.UUID

object EditPlayerProfileCopyHelper {
    @Suppress("DEPRECATION")
    fun copyProfile(source: PlayerProfile): PlayerProfile {
        val clone = runCatching { Bukkit.createProfile(source.uniqueId, source.name) }
            .getOrElse { Bukkit.createProfile(source.uniqueId ?: UUID.randomUUID()) }
        source.properties.forEach { clone.setProperty(ProfileProperty(it.name, it.value, it.signature)) }
        return clone
    }
}
