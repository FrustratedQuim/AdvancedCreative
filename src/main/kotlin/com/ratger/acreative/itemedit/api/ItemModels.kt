package com.ratger.acreative.itemedit.api

import com.ratger.acreative.itemedit.trim.DecoratedPotSide
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemRarity
import org.bukkit.potion.PotionEffectType
import org.bukkit.Material
import org.bukkit.inventory.meta.trim.TrimMaterial
import org.bukkit.inventory.meta.trim.TrimPattern

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

sealed interface ItemAction {
    data object Show : ItemAction
    data class Reset(val scope: String) : ItemAction
    data class NameSet(val miniMessage: String) : ItemAction
    data object NameClear : ItemAction
    data class LoreAdd(val miniMessage: String) : ItemAction
    data class LoreSet(val index: Int, val miniMessage: String) : ItemAction
    data class LoreRemove(val index: Int) : ItemAction
    data object LoreClear : ItemAction
    data class SetItemModel(val key: NamespacedKey?) : ItemAction
    data class SetItemId(val material: Material) : ItemAction
    data class SetUnbreakable(val value: Boolean) : ItemAction
    data class SetGlider(val value: Boolean) : ItemAction
    data class SetMaxDamage(val value: Int?) : ItemAction
    data class SetDamage(val value: Int) : ItemAction
    data class SetMaxStackSize(val value: Int?) : ItemAction
    data class SetRarity(val value: ItemRarity?) : ItemAction
    data class SetTooltipStyle(val value: NamespacedKey?) : ItemAction
    data class SetUseCooldown(val seconds: Float, val cooldownGroup: Key?) : ItemAction
    data object ClearUseCooldown : ItemAction
    data class SetHideTooltip(val value: Boolean) : ItemAction
    data class SetHideAdditionalTooltip(val value: Boolean) : ItemAction
    data class EnchantAdd(val enchantment: Enchantment, val level: Int) : ItemAction
    data class EnchantRemove(val enchantment: Enchantment) : ItemAction
    data object EnchantClear : ItemAction
    data class SetEnchantmentGlint(val value: Boolean?) : ItemAction
    data class TooltipToggle(val key: String, val hide: Boolean) : ItemAction
    data class SetCanPlaceOn(val keys: Set<NamespacedKey>) : ItemAction
    data class SetCanBreak(val keys: Set<NamespacedKey>) : ItemAction
    data class PotionColor(val rgb: Int?) : ItemAction
    data class ConsumableToggle(val enabled: Boolean) : ItemAction
    data class ConsumableAnimation(val animation: ItemUseAnimation) : ItemAction
    data class ConsumableHasParticles(val value: Boolean) : ItemAction
    data class ConsumableConsumeSeconds(val value: Float) : ItemAction
    data class ConsumableSound(val key: Key?) : ItemAction
    data class ConsumableEffectAdd(val spec: EffectActionSpec) : ItemAction
    data class ConsumableEffectRemove(val index: Int) : ItemAction
    data object ConsumableEffectClear : ItemAction
    data class DeathProtectionToggle(val enabled: Boolean) : ItemAction
    data class DeathProtectionEffectAdd(val spec: EffectActionSpec) : ItemAction
    data class DeathProtectionEffectRemove(val index: Int) : ItemAction
    data object DeathProtectionEffectClear : ItemAction
    data class FoodNutrition(val value: Int) : ItemAction
    data class FoodSaturation(val value: Float) : ItemAction
    data class FoodCanAlwaysEat(val value: Boolean) : ItemAction
    data object RemainderSetFromOffhand : ItemAction
    data object RemainderClear : ItemAction
    data object LockSetFromOffhand : ItemAction
    data object LockClear : ItemAction
    data class ContainerSetSlotFromOffhand(val index: Int) : ItemAction

    data class EquippableSetSlot(val slot: org.bukkit.inventory.EquipmentSlot) : ItemAction
    data object EquippableClear : ItemAction
    data class EquippableSetDispensable(val value: Boolean) : ItemAction
    data class EquippableSetSwappable(val value: Boolean) : ItemAction
    data class EquippableSetDamageOnHurt(val value: Boolean) : ItemAction
    data class EquippableSetEquipSound(val keyOrDefault: Key?) : ItemAction
    data class EquippableSetCameraOverlay(val keyOrNull: Key?) : ItemAction
    data class EquippableSetAssetId(val keyOrNull: Key?) : ItemAction
    data class ToolSetDefaultMiningSpeed(val value: Float, val scope: ToolSpeedScope) : ItemAction
    data class ToolSetDamagePerBlock(val value: Int) : ItemAction
    data object ToolClear : ItemAction
    data class PotionEffectAdd(
        val type: PotionEffectType,
        val duration: Int,
        val amplifier: Int,
        val ambient: Boolean,
        val particles: Boolean,
        val icon: Boolean
    ) : ItemAction

    data class PotionEffectRemove(val type: PotionEffectType) : ItemAction
    data object PotionEffectClear : ItemAction
    data object HeadClear : ItemAction
    data class HeadSetFromTexture(val base64: String) : ItemAction
    data class HeadSetFromName(val name: String) : ItemAction
    data class HeadSetFromOnline(val name: String) : ItemAction
    data class AttributeAdd(
        val attribute: Attribute,
        val amount: Double,
        val operation: AttributeModifier.Operation,
        val slotGroup: String?
    ) : ItemAction

    data class AttributeRemove(val index: Int) : ItemAction
    data object AttributeClear : ItemAction
    data class TrimSet(val pattern: TrimPattern, val material: TrimMaterial) : ItemAction
    data object TrimClear : ItemAction
    data object PotClear : ItemAction
    data class PotSet(val back: Material, val left: Material, val right: Material, val front: Material) : ItemAction
    data class PotSetSide(val side: DecoratedPotSide, val material: Material) : ItemAction
}

data class ItemResult(
    val ok: Boolean,
    val lines: List<Component>,
    val warning: Boolean = false
)

data class ItemContext(
    val item: ItemStack,
    val snapshot: ItemSnapshot
)

data class ItemSnapshot(
    val type: String,
    val amount: Int,
    val hasName: Boolean,
    val loreSize: Int,
    val isPotion: Boolean,
    val isArmor: Boolean,
    val isHead: Boolean,
    val isShulker: Boolean
)
