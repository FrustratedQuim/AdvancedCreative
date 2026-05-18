package com.ratger.acreative.commands.disguise.service

import com.ratger.acreative.commands.disguise.DisguisePermissions
import com.ratger.acreative.commands.disguise.model.DisguiseRequest
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.Bukkit
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.permissions.Permissible

class DisguiseAccessPolicy(private val hooker: FunctionHooker) {
    companion object {
        private const val BLOCKED_DISGUISES_PATH = "blocked-disguises"
    }

    val donationRestrictedEntities = setOf(
        EntityType.WITHER,
        EntityType.ENDER_DRAGON,
        EntityType.GIANT,
        EntityType.WARDEN
    )

    val blockedDisguiseEntities: Set<EntityType>
        get() = hooker.configManager.config.getStringList(BLOCKED_DISGUISES_PATH)
            .mapNotNull(::parseEntityType)
            .toSet()

    fun parseEntityType(type: String?): EntityType? {
        return type?.let { raw ->
            runCatching { EntityType.valueOf(raw.uppercase()) }.getOrNull()
        }
    }

    fun isBlockedDisguiseType(type: EntityType): Boolean {
        return type in blockedDisguiseEntities
    }

    fun canUseTextDisguise(permissible: Permissible): Boolean {
        return permissible.hasPermission(DisguisePermissions.TEXT)
    }

    fun isSlimeType(type: EntityType): Boolean {
        return type == EntityType.SLIME || type == EntityType.MAGMA_CUBE
    }

    fun normalizeSlimeSize(size: Int?): Int? {
        return size?.coerceIn(DisguiseRequest.SLIME_SIZE_RANGE.first, DisguiseRequest.SLIME_SIZE_RANGE.last)
    }

    fun resolveIdentityKey(entityType: EntityType, textDisplayRaw: String?, slimeSize: Int?): String {
        return when {
            entityType == EntityType.TEXT_DISPLAY -> "${entityType.name}:${textDisplayRaw.orEmpty()}"
            isSlimeType(entityType) -> "${entityType.name}:size=${normalizeSlimeSize(slimeSize) ?: 1}"
            else -> entityType.name
        }
    }

    fun findOnlinePlayer(name: String): Player? {
        return Bukkit.getOnlinePlayers().firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}
