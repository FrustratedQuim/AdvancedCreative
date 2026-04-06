@file:Suppress("UnstableApiUsage")

package com.ratger.acreative.itemedit.show

import com.ratger.acreative.itemedit.effects.ConsumeEffectsAdapter
import com.ratger.acreative.itemedit.meta.LegacyMetaKeySupport
import io.papermc.paper.datacomponent.DataComponentTypes
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Material
import org.bukkit.Registry
import org.bukkit.Sound
import org.bukkit.block.Lockable
import org.bukkit.inventory.meta.BlockStateMeta
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ArmorMeta
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.inventory.meta.SkullMeta

object ShowViewAdapters {
    private val plain = PlainTextComponentSerializer.plainText()

    data class ConsumableShowView(
        val consumeSeconds: Float,
        val animation: String,
        val hasConsumeParticles: Boolean,
        val sound: String?,
        val effects: List<String>
    )

    data class DeathProtectionShowView(
        val effects: List<String>
    )

    data class FoodShowView(
        val nutrition: Int,
        val saturation: Float,
        val canAlwaysEat: Boolean
    )

    data class ToolShowView(
        val defaultMiningSpeed: Float,
        val damagePerBlock: Int,
        val rules: Int,
        val speedRules: Int
    )

    data class UseCooldownShowView(
        val seconds: Float,
        val cooldownGroup: String?
    )

    data class RemainderShowView(
        val typeKey: String,
        val amount: Int,
        val name: String?,
        val loreLines: Int,
        val enchants: Int
    )

    data class PlaceDestroySummary(
        val placeableCount: Int,
        val destroyableCount: Int
    )

    data class HeadProfileSummary(
        val hasProfile: Boolean,
        val hasTextures: Boolean,
        val texturesBase64Length: Int?
    )

    data class TrimSummary(
        val patternKey: String,
        val materialKey: String
    )

    data class LockSummary(
        val material: String,
        val amount: Int
    )

    fun consumable(item: ItemStack): ConsumableShowView? {
        val consumable = item.getData(DataComponentTypes.CONSUMABLE) ?: return null
        return ConsumableShowView(
            consumeSeconds = consumable.consumeSeconds(),
            animation = consumable.animation().toString(),
            hasConsumeParticles = consumable.hasConsumeParticles(),
            sound = consumable.sound().asString(),
            effects = consumable.consumeEffects().map(ConsumeEffectsAdapter::render)
        )
    }

    fun deathProtection(item: ItemStack): DeathProtectionShowView? {
        val deathProtection = item.getData(DataComponentTypes.DEATH_PROTECTION) ?: return null
        return DeathProtectionShowView(
            effects = deathProtection.deathEffects().map(ConsumeEffectsAdapter::render)
        )
    }

    fun food(item: ItemStack): FoodShowView? {
        val food = item.getData(DataComponentTypes.FOOD) ?: return null
        return FoodShowView(
            nutrition = food.nutrition(),
            saturation = food.saturation(),
            canAlwaysEat = food.canAlwaysEat()
        )
    }

    fun tool(item: ItemStack): ToolShowView? {
        val tool = item.getData(DataComponentTypes.TOOL) ?: return null
        return ToolShowView(
            defaultMiningSpeed = tool.defaultMiningSpeed(),
            damagePerBlock = tool.damagePerBlock(),
            rules = tool.rules().size,
            speedRules = tool.rules().count { it.speed() != null }
        )
    }

    fun useCooldown(item: ItemStack): UseCooldownShowView? {
        val useCooldown = item.getData(DataComponentTypes.USE_COOLDOWN) ?: return null
        return UseCooldownShowView(
            seconds = useCooldown.seconds(),
            cooldownGroup = useCooldown.cooldownGroup()?.asString()
        )
    }

    fun remainder(item: ItemStack): RemainderShowView? {
        val remainder = item.getData(DataComponentTypes.USE_REMAINDER) ?: return null
        val remainderItem = remainder.transformInto()
        val remainderMeta = remainderItem.itemMeta
        return RemainderShowView(
            typeKey = remainderItem.type.key.asString(),
            amount = remainderItem.amount,
            name = remainderMeta?.displayName()?.let(plain::serialize)?.takeIf { it.isNotBlank() },
            loreLines = remainderMeta?.lore()?.size ?: 0,
            enchants = remainderMeta?.enchants?.size ?: 0
        )
    }

    fun placeDestroy(meta: ItemMeta?): PlaceDestroySummary {
        return PlaceDestroySummary(
            placeableCount = LegacyMetaKeySupport.placeableCount(meta),
            destroyableCount = LegacyMetaKeySupport.destroyableCount(meta)
        )
    }

    fun headProfile(meta: SkullMeta): HeadProfileSummary {
        val profile = meta.playerProfile ?: return HeadProfileSummary(
            hasProfile = false,
            hasTextures = false,
            texturesBase64Length = null
        )
        val textures = profile.properties.firstOrNull { it.name == "textures" }?.value
        return HeadProfileSummary(
            hasProfile = true,
            hasTextures = textures != null,
            texturesBase64Length = textures?.length
        )
    }

    fun trim(meta: ArmorMeta): TrimSummary? {
        val trim = meta.trim ?: return null
        val patternKey = Registry.TRIM_PATTERN.getKey(trim.pattern)?.asString() ?: "<unknown>"
        val materialKey = Registry.TRIM_MATERIAL.getKey(trim.material)?.asString() ?: "<unknown>"
        return TrimSummary(patternKey = patternKey, materialKey = materialKey)
    }

    fun materialId(material: Material): String = Registry.MATERIAL.getKey(material)?.asString() ?: "<unknown>"

    fun lock(meta: ItemMeta?): LockSummary? {
        val blockStateMeta = meta as? BlockStateMeta ?: return null
        val lockable = blockStateMeta.blockState as? Lockable ?: return null
        if (!lockable.isLocked) return null
        val lockRaw = lockable.lock
        val lockMaterial = lockRaw.substringBefore('[').takeIf { it.isNotBlank() } ?: "<unknown>"
        return LockSummary(material = lockMaterial, amount = 1)
    }

    fun enchantmentsSummary(meta: ItemMeta?): String {
        val entries = meta?.enchants?.entries ?: return "<none>"
        return entries.joinToString {
            val id = Registry.ENCHANTMENT.getKey(it.key)?.asString() ?: "<unknown>"
            "$id:${it.value}"
        }
    }

    fun potionEffectSummary(meta: PotionMeta): List<String> {
        return meta.customEffects.map {
            val id = Registry.MOB_EFFECT.getKey(it.type)?.asString() ?: "<unknown>"
            "$id dur=${it.duration} amp=${it.amplifier}"
        }
    }

    fun soundId(sound: Sound): String = Registry.SOUNDS.getKey(sound)?.asString() ?: "<unknown>"
}
