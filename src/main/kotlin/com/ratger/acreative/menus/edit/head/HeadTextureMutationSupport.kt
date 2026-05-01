package com.ratger.acreative.menus.edit.head

import com.destroystokyo.paper.profile.ProfileProperty
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.util.UUID

class HeadTextureMutationSupport {
    sealed interface MutationResult {
        data object Success : MutationResult
        data class Failure(val reason: String) : MutationResult
    }

    fun applyFromOnlinePlayer(item: ItemStack, name: String): MutationResult {
        val skull = skullMeta(item) ?: return MutationResult.Failure("Не player head")
        val source = Bukkit.getPlayerExact(name) ?: return MutationResult.Failure("Онлайн-игрок <white>$name</white> не найден")
        skull.playerProfile = PlayerProfileCopyHelper.copyProfile(source.playerProfile)
        item.itemMeta = skull
        return MutationResult.Success
    }

    fun applyFromTextureValue(item: ItemStack, textureValue: String): MutationResult {
        if (textureValue.isBlank()) return MutationResult.Failure("Value текстуры не может быть пустым")
        val skull = skullMeta(item) ?: return MutationResult.Failure("Не player head")
        val profile = Bukkit.createProfile(null as UUID?, null)
        profile.setProperty(ProfileProperty("textures", textureValue))
        skull.playerProfile = profile
        item.itemMeta = skull
        return MutationResult.Success
    }

    fun applyFromLicensedPayload(item: ItemStack, payload: LicensedProfileLookupService.LicensedProfilePayload): MutationResult {
        if (payload.textureValue.isBlank()) {
            return MutationResult.Failure("Официальный профиль <white>${payload.canonicalName}</white> не содержит textures property")
        }
        val skull = skullMeta(item) ?: return MutationResult.Failure("Не player head")
        val officialProfile = Bukkit.createProfile(payload.uuid, payload.canonicalName)
        officialProfile.setProperty(ProfileProperty("textures", payload.textureValue, payload.textureSignature))
        skull.playerProfile = officialProfile
        item.itemMeta = skull
        return MutationResult.Success
    }

    fun clearProfile(item: ItemStack): MutationResult {
        val skull = skullMeta(item) ?: return MutationResult.Failure("Не player head")
        skull.playerProfile = null
        item.itemMeta = skull
        return MutationResult.Success
    }

    fun texturesValue(item: ItemStack): String? {
        val skull = skullMeta(item) ?: return null
        return skull.playerProfile?.properties?.firstOrNull { it.name.equals("textures", ignoreCase = true) }?.value
    }

    private fun skullMeta(item: ItemStack): SkullMeta? {
        if (item.type != Material.PLAYER_HEAD) return null
        return item.itemMeta as? SkullMeta
    }
}
