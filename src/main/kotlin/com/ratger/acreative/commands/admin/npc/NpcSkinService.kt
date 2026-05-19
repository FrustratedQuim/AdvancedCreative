package com.ratger.acreative.commands.admin.npc

import com.ratger.acreative.menus.edit.head.LicensedProfileLookupService
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

class NpcSkinService(
    private val licensedProfileLookupService: LicensedProfileLookupService
) {
    sealed interface ResolutionResult {
        data class Success(val skin: NpcSkin) : ResolutionResult
        data object UnknownPlayer : ResolutionResult
    }

    fun resolveAsync(input: String): CompletableFuture<ResolutionResult> {
        return resolveLicensedPlayerSkinAsync(input.trim())
    }

    private fun resolveLicensedPlayerSkinAsync(playerName: String): CompletableFuture<ResolutionResult> {
        onlineSnapshot(playerName)?.let { snapshot ->
            return CompletableFuture.completedFuture(
                ResolutionResult.Success(
                    NpcSkin(
                        textureValue = snapshot.value,
                        textureSignature = snapshot.signature
                    )
                )
            )
        }

        return resolveLicensedSkinByNameAsync(playerName)
    }

    private fun resolveLicensedSkinByNameAsync(playerName: String): CompletableFuture<ResolutionResult> {
        return licensedProfileLookupService.lookupLicensedProfileAsync(playerName)
            .handle { payload, error ->
                if (error != null || payload == null || payload.textureValue.isBlank()) {
                    ResolutionResult.UnknownPlayer
                } else {
                    ResolutionResult.Success(
                        NpcSkin(
                            textureValue = payload.textureValue,
                            textureSignature = payload.textureSignature
                        )
                    )
                }
            }
    }

    private fun onlineSnapshot(name: String): OnlineSkinSnapshot? {
        val player = Bukkit.getPlayerExact(name)
            ?: Bukkit.getOnlinePlayers().firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: return null

        return snapshotFromPlayer(player)
    }

    private fun snapshotFromPlayer(player: Player): OnlineSkinSnapshot? {
        val property = player.playerProfile.properties.firstOrNull { it.name.equals("textures", ignoreCase = true) } ?: return null
        return OnlineSkinSnapshot(property.value, property.signature)
    }

    private data class OnlineSkinSnapshot(
        val value: String,
        val signature: String?
    )
}
