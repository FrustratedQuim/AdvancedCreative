package com.ratger.acreative.core

import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.util.BukkitHelper
import ru.violence.coreapi.common.api.user.User
import java.util.UUID

class CoreUserIdentityService {
    fun resolveUser(player: Player): User? = BukkitHelper.getUser(player).orElse(null)

    fun resolveUser(playerName: String): User? = BukkitHelper.getUser(playerName).orElse(null)

    fun resolveUser(playerUuid: UUID): User? = BukkitHelper.getUser(playerUuid).orElse(null)

    fun resolveUserId(player: Player): Long? = resolveUser(player)?.id

    fun resolveUserId(playerUuid: UUID): Long? = resolveUser(playerUuid)?.id

    fun resolveUserId(user: User): Long = user.id
}
