package com.ratger.acreative.menus.banner.service

import com.ratger.acreative.menus.banner.model.BannerProfileSnapshot
import com.ratger.acreative.menus.edit.head.LicensedProfileLookupService
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.util.BukkitHelper
import ru.violence.coreapi.common.api.user.User
import java.util.concurrent.CompletableFuture

class BannerPlayerLookupService(
    private val licensedProfileLookupService: LicensedProfileLookupService
) {
    fun findUser(name: String): User? = BukkitHelper.getUser(name).orElse(null)

    fun resolveSkinSnapshotAsync(user: User): CompletableFuture<BannerProfileSnapshot?> {
        val onlineSnapshot = onlineSnapshot(user.name)
        if (onlineSnapshot != null) {
            return CompletableFuture.completedFuture(onlineSnapshot)
        }

        return licensedProfileLookupService.lookupLicensedProfileAsync(user.name)
            .handle { payload, error ->
                if (error != null || payload == null || payload.textureValue.isBlank()) {
                    null
                } else {
                    BannerProfileSnapshot(payload.textureValue, payload.textureSignature)
                }
            }
    }

    private fun onlineSnapshot(name: String): BannerProfileSnapshot? {
        val player = Bukkit.getPlayerExact(name)
            ?: Bukkit.getOnlinePlayers().firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: return null

        return snapshotFromPlayer(player)
    }

    private fun snapshotFromPlayer(player: Player): BannerProfileSnapshot? {
        val property = player.playerProfile.properties.firstOrNull { it.name.equals("textures", ignoreCase = true) } ?: return null
        return BannerProfileSnapshot(property.value, property.signature)
    }
}
