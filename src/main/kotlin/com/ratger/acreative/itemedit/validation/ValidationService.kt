package com.ratger.acreative.itemedit.validation

import com.ratger.acreative.itemedit.api.ItemAction
import com.ratger.acreative.itemedit.api.ItemContext
import com.ratger.acreative.itemedit.container.ContainerSupport
import com.ratger.acreative.itemedit.effects.EffectActionsSupport
import com.ratger.acreative.itemedit.equippable.EquippableSupport
import com.ratger.acreative.itemedit.experimental.EffectSupport
import com.ratger.acreative.itemedit.trim.TrimPotSupport
import net.kyori.adventure.key.Key
import org.bukkit.block.Lockable
import org.bukkit.Material
import org.bukkit.Registry
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockStateMeta
import org.bukkit.inventory.meta.Damageable

class ValidationService {
    fun isValidKey(raw: String): Boolean = runCatching { Key.key(raw) }.isSuccess

    fun isMapEditable(item: ItemStack): Boolean = item.type == Material.FILLED_MAP

    fun isValidMapId(value: Int): Boolean = value >= 0

    fun validate(action: ItemAction, context: ItemContext, player: Player): Boolean {
        val meta = context.item.itemMeta
        when (action) {
            is ItemAction.SetDamage -> {
                if (action.value < 0) return false
                val damageable = meta as? Damageable
                val maxDamage = if (damageable?.hasMaxDamage() == true) damageable.maxDamage else null
                if (maxDamage != null && action.value > maxDamage) return false
            }

            is ItemAction.SetMaxDamage -> {
                val value = action.value ?: return true
                if (value <= 0) return false
                val stack = if (meta?.hasMaxStackSize() == true) meta.maxStackSize else null
                if (stack != null && stack > 1) {
                    return false
                }
            }

            is ItemAction.SetMaxStackSize -> {
                val value = action.value ?: return true
                if (value <= 0 || value > 99) return false
                val damageable = meta as? Damageable
                val maxDamage = if (damageable?.hasMaxDamage() == true) damageable.maxDamage else null
                if (value > 1 && maxDamage != null) return false
            }

            is ItemAction.EnchantAdd -> {
                if (action.level <= 0) return false
                if (action.level > 127) return false
            }

            is ItemAction.PotionEffectAdd -> {
                if (!context.snapshot.isPotion) return false
                if (action.duration < 0 || action.amplifier < 0) return false
                if (action.duration > Int.MAX_VALUE / 2) return false
                if (action.amplifier > 255) return false
            }

            is ItemAction.PotionColor -> {
                if (!context.snapshot.isPotion) return false
            }
            is ItemAction.ConsumableConsumeSeconds -> {
                if (!action.value.isFinite()) return false
                if (action.value <= 0f) return false
                if (action.value > 60f) return false
            }
            is ItemAction.FoodNutrition -> {
                if (action.value < 0) return false
                if (action.value > 1000) return false
            }
            is ItemAction.FoodSaturation -> {
                if (!action.value.isFinite()) return false
                if (action.value < 0f) return false
                if (action.value > 1000f) return false
            }
            is ItemAction.ToolSetDefaultMiningSpeed -> {
                if (!action.value.isFinite()) return false
                if (action.value < 0f) return false
            }
            is ItemAction.ToolSetDamagePerBlock -> {
                if (action.value < 0) return false
            }
            is ItemAction.SetUseCooldown -> {
                if (!action.seconds.isFinite()) return false
                if (action.seconds <= 0f) return false
                val group = action.cooldownGroup
                if (group != null && !isValidKey(group.asString())) return false
            }
            is ItemAction.ConsumableEffectAdd -> {
                val message = EffectActionsSupport.validateSpec(action.spec, this)
                if (message != null) return false
            }
            is ItemAction.DeathProtectionEffectAdd -> {
                val message = EffectActionsSupport.validateSpec(action.spec, this)
                if (message != null) return false
            }
            is ItemAction.ConsumableEffectRemove -> {
                val effectCount = EffectSupport.consumableEffectCount(context.item)
                    ?: return false
                if (action.index !in 0 until effectCount) return false
            }
            is ItemAction.DeathProtectionEffectRemove -> {
                val effectCount = EffectSupport.deathProtectionEffectCount(context.item)
                    ?: return false
                if (action.index !in 0 until effectCount) return false
            }

            is ItemAction.HeadSetFromTexture -> {
                if (!context.snapshot.isHead) return false
                if (action.base64.isBlank()) return false
            }

            ItemAction.HeadClear -> {
                if (!context.snapshot.isHead) return false
            }

            is ItemAction.HeadSetFromName -> {
                if (!context.snapshot.isHead) return false
                if (!action.name.matches(Regex("^[A-Za-z0-9_]{3,16}$"))) return false
            }

            is ItemAction.HeadSetFromOnline -> {
                if (!context.snapshot.isHead) return false
                if (action.name.isBlank()) return false
            }

            ItemAction.TrimClear -> {
                if (!context.snapshot.isArmor) return false
                if (meta !is org.bukkit.inventory.meta.ArmorMeta) return false
            }
            is ItemAction.TrimSet -> {
                if (!context.snapshot.isArmor) return false
                if (meta !is org.bukkit.inventory.meta.ArmorMeta) return false
            }
            ItemAction.PotClear,
            is ItemAction.PotSet,
            is ItemAction.PotSetSide -> {
                if (context.item.type != Material.DECORATED_POT) return false
                val materials = when (action) {
                    is ItemAction.PotSet -> listOf(action.back, action.left, action.right, action.front)
                    is ItemAction.PotSetSide -> listOf(action.material)
                    else -> emptyList()
                }
                val unsupported = materials.firstOrNull { !TrimPotSupport.potDecorationMaterialIds.contains(it.key.asString()) }
                if (unsupported != null) return false
            }
            is ItemAction.FrameSetInvisibility -> {
                if (context.item.type != Material.ITEM_FRAME && context.item.type != Material.GLOW_ITEM_FRAME) {
                    return false
                }
            }
            is ItemAction.AttributeAdd, is ItemAction.AttributeClear, is ItemAction.AttributeRemove -> {
                if (!context.snapshot.isArmor) {
                    return false
                }
            }
            is ItemAction.EquippableSetEquipSound -> {
                val key = action.keyOrDefault
                if (key != null) {
                    if (!isValidKey(key.asString())) return false
                    val namespaced = NamespacedKey.fromString(key.asString())
                    if (namespaced == null || Registry.SOUND_EVENT.get(namespaced) == null) {
                        return false
                    }
                }
                if (key == null && EquippableSupport.prototypeSnapshot(context.item) == null) {
                    return false
                }
            }
            is ItemAction.EquippableSetCameraOverlay -> {
                val key = action.keyOrNull
                if (key != null && !isValidKey(key.asString())) return false
                if (!EquippableSupport.hasExistingOrPrototype(context.item)) {
                    return false
                }
            }
            is ItemAction.EquippableSetAssetId -> {
                val key = action.keyOrNull
                if (key != null && !isValidKey(key.asString())) return false
                if (!EquippableSupport.hasExistingOrPrototype(context.item)) {
                    return false
                }
            }
            is ItemAction.EquippableSetDispensable,
            is ItemAction.EquippableSetSwappable,
            is ItemAction.EquippableSetDamageOnHurt -> {
                if (!EquippableSupport.hasExistingOrPrototype(context.item)) {
                    return false
                }
            }
            ItemAction.RemainderSetFromOffhand -> {
                val offhand = player.inventory.itemInOffHand
                if (offhand.type == Material.AIR || offhand.amount <= 0) {
                    return false
                }
            }
            ItemAction.LockSetFromOffhand -> {
                if (!context.snapshot.isShulker) return false
                val offhand = player.inventory.itemInOffHand
                if (offhand.type == Material.AIR || offhand.amount <= 0) {
                    return false
                }
                val blockStateMeta = meta as? BlockStateMeta ?: return false
                val state = blockStateMeta.blockState
                if (state !is Lockable) return false
            }
            ItemAction.LockClear -> {
                if (!context.snapshot.isShulker) return false
                val blockStateMeta = meta as? BlockStateMeta ?: return false
                val state = blockStateMeta.blockState
                if (state !is Lockable) return false
            }
            is ItemAction.ContainerSetSlotFromOffhand -> {
                val capacity = ContainerSupport.containerCapacity(context.item.type)
                    ?: return false
                if (action.index < 0 || action.index >= capacity) {
                    return false
                }
                if (!ContainerSupport.supportsStableContainerEditing(context.item)) {
                    return false
                }
                val offhand = player.inventory.itemInOffHand
                if (ContainerSupport.isEmpty(offhand)) {
                    return false
                }
            }

            else -> Unit
        }
        return true
    }
}
