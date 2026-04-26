package com.ratger.acreative.menus.banner.service

import org.bukkit.entity.Player
import kotlin.math.max

class BannerPermissionLimitResolver {
    fun resolveLimit(player: Player, defaultLimit: Int, limitsByPermission: Map<String, Int>): Int {
        var resolved = defaultLimit
        limitsByPermission.forEach { (permission, value) ->
            if (!player.hasPermission(permission)) {
                return@forEach
            }
            if (value < 0) {
                resolved = -1
                return@forEach
            }
            if (resolved < 0) {
                return@forEach
            }
            resolved = max(resolved, value)
        }
        return resolved
    }

    fun formatLimit(limit: Int): String = if (limit < 0) "∞" else limit.toString()
}
