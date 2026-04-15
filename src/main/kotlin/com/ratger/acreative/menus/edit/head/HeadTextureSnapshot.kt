package com.ratger.acreative.menus.edit.head

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta

class HeadTextureSnapshot private constructor(
    val profileName: String?,
    val textureValue: String?,
    val effectiveSource: HeadTextureSource
) {
    val canShowOnlineActive: Boolean = effectiveSource == HeadTextureSource.ONLINE_PLAYER && !profileName.isNullOrBlank()
    val canShowTextureValueActive: Boolean = effectiveSource == HeadTextureSource.TEXTURE_VALUE && !textureValue.isNullOrBlank()
    val canShowLicensedActive: Boolean = effectiveSource == HeadTextureSource.LICENSED_NAME && !profileName.isNullOrBlank()

    companion object {
        fun fromItem(item: ItemStack, sessionSource: HeadTextureSource? = null): HeadTextureSnapshot {
            if (item.type != Material.PLAYER_HEAD) {
                return HeadTextureSnapshot(null, null, HeadTextureSource.NONE)
            }
            val meta = item.itemMeta as? SkullMeta ?: return HeadTextureSnapshot(null, null, HeadTextureSource.NONE)
            val profile = meta.playerProfile
            val profileName = profile?.name?.takeIf { it.isNotBlank() }
            val textureValue = profile
                ?.properties
                ?.firstOrNull { it.name.equals("textures", ignoreCase = true) }
                ?.value
                ?.takeIf { it.isNotBlank() }

            val effective = sessionSource?.takeIf { it != HeadTextureSource.NONE } ?: fallback(profileName, textureValue)

            return HeadTextureSnapshot(
                profileName = profileName,
                textureValue = textureValue,
                effectiveSource = effective
            )
        }

        private fun fallback(profileName: String?, textureValue: String?): HeadTextureSource {
            if (!textureValue.isNullOrBlank()) return HeadTextureSource.TEXTURE_VALUE
            if (!profileName.isNullOrBlank() && Bukkit.getPlayerExact(profileName) != null) {
                return HeadTextureSource.ONLINE_PLAYER
            }
            if (!profileName.isNullOrBlank()) return HeadTextureSource.LICENSED_NAME
            return HeadTextureSource.NONE
        }
    }
}
