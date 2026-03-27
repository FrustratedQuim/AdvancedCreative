package com.ratger.acreative.commands.edit

import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemRarity
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.potion.PotionEffectType

data class EffectApplyEntrySpec(
    val type: PotionEffectType,
    val duration: Int,
    val amplifier: Int,
    val showParticles: Boolean,
    val showIcon: Boolean
)

sealed interface EffectActionSpec {
    data object ClearAllEffects : EffectActionSpec
    data class PlaySound(val key: Key) : EffectActionSpec
    data class RemoveEffects(val effects: List<PotionEffectType>) : EffectActionSpec
    data class TeleportRandomly(val diameter: Float) : EffectActionSpec
    data class ApplyEffects(val probability: Float, val effects: List<EffectApplyEntrySpec>) : EffectActionSpec
}

enum class ToolSpeedScope {
    ALL_BLOCKS,
    EFFECTIVE_ONLY,
    INEFFECTIVE_ONLY
}

sealed interface EditAction {
    data object Show : EditAction
    data class Reset(val scope: String) : EditAction
    data class NameSet(val miniMessage: String) : EditAction
    data object NameClear : EditAction
    data class LoreAdd(val miniMessage: String) : EditAction
    data class LoreSet(val index: Int, val miniMessage: String) : EditAction
    data class LoreRemove(val index: Int) : EditAction
    data object LoreClear : EditAction
    data class SetItemModel(val key: NamespacedKey?) : EditAction
    data class SetUnbreakable(val value: Boolean) : EditAction
    data class SetGlider(val value: Boolean) : EditAction
    data class SetMaxDamage(val value: Int?) : EditAction
    data class SetDamage(val value: Int) : EditAction
    data class SetMaxStackSize(val value: Int?) : EditAction
    data class SetRarity(val value: ItemRarity?) : EditAction
    data class SetTooltipStyle(val value: NamespacedKey?) : EditAction
    data class SetUseCooldown(val seconds: Float, val cooldownGroup: Key?) : EditAction
    data object ClearUseCooldown : EditAction
    data class SetHideTooltip(val value: Boolean) : EditAction
    data class SetHideAdditionalTooltip(val value: Boolean) : EditAction
    data class EnchantAdd(val enchantment: Enchantment, val level: Int) : EditAction
    data class EnchantRemove(val enchantment: Enchantment) : EditAction
    data object EnchantClear : EditAction
    data class SetEnchantmentGlint(val value: Boolean?) : EditAction
    data class TooltipToggle(val key: String, val hide: Boolean) : EditAction
    data class SetCanPlaceOn(val keys: Set<NamespacedKey>) : EditAction
    data class SetCanBreak(val keys: Set<NamespacedKey>) : EditAction
    data class PotionColor(val rgb: Int?) : EditAction
    data class ConsumableToggle(val enabled: Boolean) : EditAction
    data class ConsumableAnimation(val animation: ItemUseAnimation) : EditAction
    data class ConsumableHasParticles(val value: Boolean) : EditAction
    data class ConsumableConsumeSeconds(val value: Float) : EditAction
    data class ConsumableSound(val key: Key?) : EditAction
    data class ConsumableEffectAdd(val spec: EffectActionSpec) : EditAction
    data class ConsumableEffectRemove(val index: Int) : EditAction
    data object ConsumableEffectClear : EditAction
    data class DeathProtectionToggle(val enabled: Boolean) : EditAction
    data class DeathProtectionEffectAdd(val spec: EffectActionSpec) : EditAction
    data class DeathProtectionEffectRemove(val index: Int) : EditAction
    data object DeathProtectionEffectClear : EditAction
    data class FoodNutrition(val value: Int) : EditAction
    data class FoodSaturation(val value: Float) : EditAction
    data class FoodCanAlwaysEat(val value: Boolean) : EditAction
    data object RemainderSetFromOffhand : EditAction
    data object RemainderClear : EditAction
    data object LockSetFromOffhand : EditAction
    data object LockClear : EditAction

    data class EquippableSetSlot(val slot: org.bukkit.inventory.EquipmentSlot) : EditAction
    data object EquippableClear : EditAction
    data class EquippableSetDispensable(val value: Boolean) : EditAction
    data class EquippableSetSwappable(val value: Boolean) : EditAction
    data class EquippableSetDamageOnHurt(val value: Boolean) : EditAction
    data class EquippableSetEquipSound(val keyOrDefault: net.kyori.adventure.key.Key?) : EditAction
    data class EquippableSetCameraOverlay(val keyOrNull: net.kyori.adventure.key.Key?) : EditAction
    data class EquippableSetAssetId(val keyOrNull: net.kyori.adventure.key.Key?) : EditAction
    data class ToolSetDefaultMiningSpeed(val value: Float, val scope: ToolSpeedScope) : EditAction
    data class ToolSetDamagePerBlock(val value: Int) : EditAction
    data object ToolClear : EditAction
    data class PotionEffectAdd(
        val type: PotionEffectType,
        val duration: Int,
        val amplifier: Int,
        val ambient: Boolean,
        val particles: Boolean,
        val icon: Boolean
    ) : EditAction

    data class PotionEffectRemove(val type: PotionEffectType) : EditAction
    data object PotionEffectClear : EditAction
    data class HeadTextureSet(val base64: String) : EditAction
    data object HeadTextureClear : EditAction
    data class AttributeAdd(
        val attribute: Attribute,
        val amount: Double,
        val operation: AttributeModifier.Operation,
        val slotGroup: String?
    ) : EditAction

    data class AttributeRemove(val index: Int) : EditAction
    data object AttributeClear : EditAction
}

data class EditResult(
    val ok: Boolean,
    val lines: List<Component>,
    val warning: Boolean = false
)

data class EditContext(
    val item: ItemStack,
    val snapshot: EditStateSnapshot
)

data class EditStateSnapshot(
    val type: String,
    val amount: Int,
    val hasName: Boolean,
    val loreSize: Int,
    val isPotion: Boolean,
    val isArmor: Boolean,
    val isHead: Boolean,
    val isShulker: Boolean
)

fun PotionMeta.isPotionItem(): Boolean = true
