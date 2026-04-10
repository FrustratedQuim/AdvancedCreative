package com.ratger.acreative.itemedit.validation

import com.ratger.acreative.itemedit.api.ItemAction
import com.ratger.acreative.itemedit.api.ItemContext
import com.ratger.acreative.itemedit.api.ItemResult
import com.ratger.acreative.itemedit.container.ContainerSupport
import com.ratger.acreative.itemedit.effects.EffectActionsSupport
import com.ratger.acreative.itemedit.equippable.EquippableSupport
import com.ratger.acreative.itemedit.experimental.EffectSupport
import com.ratger.acreative.itemedit.trim.TrimPotSupport
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.block.Lockable
import org.bukkit.Material
import org.bukkit.Registry
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockStateMeta
import org.bukkit.inventory.meta.Damageable

class ValidationService {
    private val mini = MiniMessage.miniMessage()

    fun fail(message: String): ItemResult {
        return ItemResult(false, listOf(mini.deserialize("<red>$message")))
    }

    fun isValidKey(raw: String): Boolean = runCatching { Key.key(raw) }.isSuccess

    fun isMapEditable(item: ItemStack): Boolean = item.type == Material.FILLED_MAP

    fun isValidMapId(value: Int): Boolean = value >= 0

    fun validate(action: ItemAction, context: ItemContext, player: Player): ItemResult? {
        val meta = context.item.itemMeta
        when (action) {
            is ItemAction.SetDamage -> {
                if (action.value < 0) return fail("damage не может быть отрицательным")
                val damageable = meta as? Damageable
                val maxDamage = if (damageable?.hasMaxDamage() == true) damageable.maxDamage else null
                if (maxDamage != null && action.value > maxDamage) return fail("damage не может быть больше max_damage ($maxDamage)")
            }

            is ItemAction.SetMaxDamage -> {
                val value = action.value ?: return null
                if (value <= 0) return fail("max_damage должен быть > 0")
                val stack = if (meta?.hasMaxStackSize() == true) meta.maxStackSize else null
                if (stack != null && stack > 1) {
                    return fail("max_damage конфликтует с max_stack_size > 1")
                }
            }

            is ItemAction.SetMaxStackSize -> {
                val value = action.value ?: return null
                if (value <= 0 || value > 99) return fail("max_stack_size должен быть в диапазоне 1..99")
                val damageable = meta as? Damageable
                val maxDamage = if (damageable?.hasMaxDamage() == true) damageable.maxDamage else null
                if (value > 1 && maxDamage != null) return fail("max_stack_size > 1 конфликтует с max_damage")
            }

            is ItemAction.EnchantAdd -> {
                if (action.level <= 0) return fail("уровень чар должен быть > 0")
                if (action.level > 127) return fail("уровень чар должен быть в диапазоне 1..127")
            }

            is ItemAction.PotionEffectAdd -> {
                if (!context.snapshot.isPotion) return fail("Эта ветка только для potion/splash/lingering/tipped_arrow")
                if (action.duration < 0 || action.amplifier < 0) return fail("duration/amplifier не могут быть отрицательными")
                if (action.duration > Int.MAX_VALUE / 2) return fail("duration слишком большой")
                if (action.amplifier > 255) return fail("amplifier > 255 не допускается")
            }

            is ItemAction.PotionColor -> {
                if (!context.snapshot.isPotion) return fail("Цвет зелий доступен только для potion предметов")
            }
            is ItemAction.ConsumableConsumeSeconds -> {
                if (!action.value.isFinite()) return fail("consume_seconds должен быть конечным числом")
                if (action.value <= 0f) return fail("consume_seconds должен быть > 0")
                if (action.value > 60f) return fail("consume_seconds слишком большой")
            }
            is ItemAction.FoodNutrition -> {
                if (action.value < 0) return fail("nutrition не может быть отрицательным")
                if (action.value > 1000) return fail("nutrition слишком большой")
            }
            is ItemAction.FoodSaturation -> {
                if (!action.value.isFinite()) return fail("saturation должен быть конечным числом")
                if (action.value < 0f) return fail("saturation не может быть отрицательным")
                if (action.value > 1000f) return fail("saturation слишком большой")
            }
            is ItemAction.ToolSetDefaultMiningSpeed -> {
                if (!action.value.isFinite()) return fail("tool speed должен быть конечным числом")
                if (action.value < 0f) return fail("tool speed не может быть отрицательным")
            }
            is ItemAction.ToolSetDamagePerBlock -> {
                if (action.value < 0) return fail("tool damage_per_block не может быть отрицательным")
            }
            is ItemAction.SetUseCooldown -> {
                if (!action.seconds.isFinite()) return fail("use_cooldown seconds должен быть конечным числом")
                if (action.seconds <= 0f) return fail("use_cooldown seconds должен быть > 0")
                val group = action.cooldownGroup
                if (group != null && !isValidKey(group.asString())) return fail("Некорректный namespaced key для use_cooldown group")
            }
            is ItemAction.ConsumableEffectAdd -> {
                val message = EffectActionsSupport.validateSpec(action.spec, this)
                if (message != null) return fail(message)
            }
            is ItemAction.DeathProtectionEffectAdd -> {
                val message = EffectActionsSupport.validateSpec(action.spec, this)
                if (message != null) return fail(message)
            }
            is ItemAction.ConsumableEffectRemove -> {
                val effectCount = EffectSupport.consumableEffectCount(context.item)
                    ?: return fail("У предмета нет компонента consumable")
                if (action.index !in 0 until effectCount) return fail("Некорректный индекс effect_remove")
            }
            is ItemAction.DeathProtectionEffectRemove -> {
                val effectCount = EffectSupport.deathProtectionEffectCount(context.item)
                    ?: return fail("У предмета нет компонента death_protection")
                if (action.index !in 0 until effectCount) return fail("Некорректный индекс effect_remove")
            }

            is ItemAction.HeadSetFromTexture -> {
                if (!context.snapshot.isHead) return fail("Эта ветка только для player_head")
                if (action.base64.isBlank()) return fail("texture base64 пустая")
            }

            ItemAction.HeadClear -> {
                if (!context.snapshot.isHead) return fail("Эта ветка только для player_head")
            }

            is ItemAction.HeadSetFromName -> {
                if (!context.snapshot.isHead) return fail("Эта ветка только для player_head")
                if (!action.name.matches(Regex("^[A-Za-z0-9_]{3,16}$"))) return fail("Некорректный licensed_name (ожидается 3..16: A-Z a-z 0-9 _)")
            }

            is ItemAction.HeadSetFromOnline -> {
                if (!context.snapshot.isHead) return fail("Эта ветка только для player_head")
                if (action.name.isBlank()) return fail("Укажите ник онлайн-игрока")
            }

            ItemAction.TrimClear -> {
                if (!context.snapshot.isArmor) return fail("Эта ветка только для armor items")
                if (meta !is org.bukkit.inventory.meta.ArmorMeta) return fail("Item meta не поддерживает ArmorMeta")
            }
            is ItemAction.TrimSet -> {
                if (!context.snapshot.isArmor) return fail("Эта ветка только для armor items")
                if (meta !is org.bukkit.inventory.meta.ArmorMeta) return fail("Item meta не поддерживает ArmorMeta")
            }
            ItemAction.PotClear,
            is ItemAction.PotSet,
            is ItemAction.PotSetSide -> {
                if (context.item.type != Material.DECORATED_POT) return fail("Эта ветка только для minecraft:decorated_pot")
                val materials = when (action) {
                    is ItemAction.PotSet -> listOf(action.back, action.left, action.right, action.front)
                    is ItemAction.PotSetSide -> listOf(action.material)
                    else -> emptyList()
                }
                val unsupported = materials.firstOrNull { !TrimPotSupport.potDecorationMaterialIds.contains(it.key.asString()) }
                if (unsupported != null) return fail("Недопустимый pot item id: ${unsupported.key.asString()}")
            }
            is ItemAction.FrameSetInvisibility -> {
                if (context.item.type != Material.ITEM_FRAME && context.item.type != Material.GLOW_ITEM_FRAME) {
                    return fail("Эта ветка только для minecraft:item_frame и minecraft:glow_item_frame")
                }
            }
            is ItemAction.AttributeAdd, is ItemAction.AttributeClear, is ItemAction.AttributeRemove -> {
                if (!context.snapshot.isArmor) {
                    return fail("attribute modifiers в этой команде доступны только для armor items")
                }
            }
            is ItemAction.EquippableSetEquipSound -> {
                val key = action.keyOrDefault
                if (key != null) {
                    if (!isValidKey(key.asString())) return fail("Некорректный namespaced key для equip_sound")
                    val namespaced = NamespacedKey.fromString(key.asString())
                    if (namespaced == null || Registry.SOUND_EVENT.get(namespaced) == null) {
                        return fail("Неизвестный sound key для equip_sound")
                    }
                }
                if (key == null && EquippableSupport.prototypeSnapshot(context.item) == null) {
                    return fail("Для этого material нельзя восстановить default equip sound")
                }
            }
            is ItemAction.EquippableSetCameraOverlay -> {
                val key = action.keyOrNull
                if (key != null && !isValidKey(key.asString())) return fail("Некорректный namespaced key для camera_overlay")
                if (!EquippableSupport.hasExistingOrPrototype(context.item)) {
                    return fail("Сначала /dedit equippable slot ...")
                }
            }
            is ItemAction.EquippableSetAssetId -> {
                val key = action.keyOrNull
                if (key != null && !isValidKey(key.asString())) return fail("Некорректный namespaced key для asset_id")
                if (!EquippableSupport.hasExistingOrPrototype(context.item)) {
                    return fail("Сначала /dedit equippable slot ...")
                }
            }
            is ItemAction.EquippableSetDispensable,
            is ItemAction.EquippableSetSwappable,
            is ItemAction.EquippableSetDamageOnHurt -> {
                if (!EquippableSupport.hasExistingOrPrototype(context.item)) {
                    return fail("Сначала /dedit equippable slot ...")
                }
            }
            ItemAction.RemainderSetFromOffhand -> {
                val offhand = player.inventory.itemInOffHand
                if (offhand.type == Material.AIR || offhand.amount <= 0) {
                    return fail("Для remainder set держите предмет во второй руке.")
                }
            }
            ItemAction.LockSetFromOffhand -> {
                if (!context.snapshot.isShulker) return fail("Эта ветка только для shulker box item")
                val offhand = player.inventory.itemInOffHand
                if (offhand.type == Material.AIR || offhand.amount <= 0) {
                    return fail("Для lock set держите предмет-ключ во второй руке.")
                }
                val blockStateMeta = meta as? BlockStateMeta ?: return fail("Item meta не поддерживает block state (BlockStateMeta)")
                val state = blockStateMeta.blockState
                if (state !is Lockable) return fail("Block state этого shulker не поддерживает lock API")
            }
            ItemAction.LockClear -> {
                if (!context.snapshot.isShulker) return fail("Эта ветка только для shulker box item")
                val blockStateMeta = meta as? BlockStateMeta ?: return fail("Item meta не поддерживает block state (BlockStateMeta)")
                val state = blockStateMeta.blockState
                if (state !is Lockable) return fail("Block state этого shulker не поддерживает lock API")
            }
            is ItemAction.ContainerSetSlotFromOffhand -> {
                val capacity = ContainerSupport.containerCapacity(context.item.type)
                    ?: return fail("Этот предмет не поддерживает /dedit container")
                if (action.index < 0 || action.index >= capacity) {
                    return fail("Для ${context.item.type.key.asString()} доступно $capacity слотов: 0..${capacity - 1}")
                }
                if (!ContainerSupport.supportsStableContainerEditing(context.item)) {
                    return fail("Для ${context.item.type.key.asString()} стабильный BlockState путь контейнера недоступен")
                }
                val offhand = player.inventory.itemInOffHand
                if (ContainerSupport.isEmpty(offhand)) {
                    return fail("Во второй руке должен быть предмет (не AIR) для установки в container slot")
                }
            }

            else -> Unit
        }
        return null
    }
}
