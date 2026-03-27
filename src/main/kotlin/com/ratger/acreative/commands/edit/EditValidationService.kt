package com.ratger.acreative.commands.edit

import io.papermc.paper.datacomponent.DataComponentTypes
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.block.Lockable
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.BlockStateMeta
import org.bukkit.inventory.meta.Damageable

class EditValidationService {
    private val mini = MiniMessage.miniMessage()

    fun fail(player: Player, message: String): EditResult {
        return EditResult(false, listOf(mini.deserialize("<red>$message")))
    }

    fun isValidKey(raw: String): Boolean = runCatching { Key.key(raw) }.isSuccess

    fun validate(action: EditAction, context: EditContext, player: Player): EditResult? {
        val meta = context.item.itemMeta
        when (action) {
            is EditAction.SetDamage -> {
                if (action.value < 0) return fail(player, "damage не может быть отрицательным")
                val damageable = meta as? Damageable
                val maxDamage = if (damageable?.hasMaxDamage() == true) damageable.maxDamage else null
                if (maxDamage != null && action.value > maxDamage) return fail(player, "damage не может быть больше max_damage ($maxDamage)")
            }

            is EditAction.SetMaxDamage -> {
                val value = action.value ?: return null
                if (value <= 0) return fail(player, "max_damage должен быть > 0")
                val stack = if (meta?.hasMaxStackSize() == true) meta.maxStackSize else null
                if (stack != null && stack > 1) {
                    return fail(player, "max_damage конфликтует с max_stack_size > 1")
                }
            }

            is EditAction.SetMaxStackSize -> {
                val value = action.value ?: return null
                if (value <= 0 || value > 99) return fail(player, "max_stack_size должен быть в диапазоне 1..99")
                val damageable = meta as? Damageable
                val maxDamage = if (damageable?.hasMaxDamage() == true) damageable.maxDamage else null
                if (value > 1 && maxDamage != null) return fail(player, "max_stack_size > 1 конфликтует с max_damage")
            }

            is EditAction.EnchantAdd -> {
                if (action.level <= 0) return fail(player, "уровень чар должен быть > 0")
                if (action.level > 255) return fail(player, "уровень чар больше hard cap API (255)")
            }

            is EditAction.PotionEffectAdd -> {
                if (!context.snapshot.isPotion) return fail(player, "Эта ветка только для potion/splash/lingering/tipped_arrow")
                if (action.duration < 0 || action.amplifier < 0) return fail(player, "duration/amplifier не могут быть отрицательными")
                if (action.duration > Int.MAX_VALUE / 2) return fail(player, "duration слишком большой")
                if (action.amplifier > 255) return fail(player, "amplifier > 255 не допускается")
            }

            is EditAction.PotionColor -> {
                if (!context.snapshot.isPotion) return fail(player, "Цвет зелий доступен только для potion предметов")
            }
            is EditAction.ConsumableConsumeSeconds -> {
                if (!action.value.isFinite()) return fail(player, "consume_seconds должен быть конечным числом")
                if (action.value <= 0f) return fail(player, "consume_seconds должен быть > 0")
                if (action.value > 60f) return fail(player, "consume_seconds слишком большой")
            }
            is EditAction.FoodNutrition -> {
                if (action.value < 0) return fail(player, "nutrition не может быть отрицательным")
                if (action.value > 1000) return fail(player, "nutrition слишком большой")
            }
            is EditAction.FoodSaturation -> {
                if (!action.value.isFinite()) return fail(player, "saturation должен быть конечным числом")
                if (action.value < 0f) return fail(player, "saturation не может быть отрицательным")
                if (action.value > 1000f) return fail(player, "saturation слишком большой")
            }
            is EditAction.ToolSetDefaultMiningSpeed -> {
                if (!action.value.isFinite()) return fail(player, "tool speed должен быть конечным числом")
                if (action.value < 0f) return fail(player, "tool speed не может быть отрицательным")
            }
            is EditAction.ToolSetDamagePerBlock -> {
                if (action.value < 0) return fail(player, "tool damage_per_block не может быть отрицательным")
            }
            is EditAction.SetUseCooldown -> {
                if (!action.seconds.isFinite()) return fail(player, "use_cooldown seconds должен быть конечным числом")
                if (action.seconds <= 0f) return fail(player, "use_cooldown seconds должен быть > 0")
                val group = action.cooldownGroup
                if (group != null && !isValidKey(group.asString())) return fail(player, "Некорректный namespaced key для use_cooldown group")
            }
            is EditAction.ConsumableEffectAdd -> {
                val message = EditEffectActionsSupport.validateSpec(action.spec, this)
                if (message != null) return fail(player, message)
            }
            is EditAction.DeathProtectionEffectAdd -> {
                val message = EditEffectActionsSupport.validateSpec(action.spec, this)
                if (message != null) return fail(player, message)
            }
            is EditAction.ConsumableEffectRemove -> {
                val consumable = context.item.getData(io.papermc.paper.datacomponent.DataComponentTypes.CONSUMABLE)
                    ?: return fail(player, "У предмета нет компонента consumable")
                if (action.index !in consumable.consumeEffects().indices) return fail(player, "Некорректный индекс effect_remove")
            }
            is EditAction.DeathProtectionEffectRemove -> {
                val dp = context.item.getData(io.papermc.paper.datacomponent.DataComponentTypes.DEATH_PROTECTION)
                    ?: return fail(player, "У предмета нет компонента death_protection")
                if (action.index !in dp.deathEffects().indices) return fail(player, "Некорректный индекс effect_remove")
            }

            is EditAction.HeadSetFromTexture -> {
                if (!context.snapshot.isHead) return fail(player, "Эта ветка только для player_head")
                if (action.base64.isBlank()) return fail(player, "texture base64 пустая")
            }

            EditAction.HeadClear -> {
                if (!context.snapshot.isHead) return fail(player, "Эта ветка только для player_head")
            }

            is EditAction.HeadSetFromName -> {
                if (!context.snapshot.isHead) return fail(player, "Эта ветка только для player_head")
                if (!action.name.matches(Regex("^[A-Za-z0-9_]{3,16}$"))) return fail(player, "Некорректный licensed_name (ожидается 3..16: A-Z a-z 0-9 _)")
            }

            is EditAction.HeadSetFromOnline -> {
                if (!context.snapshot.isHead) return fail(player, "Эта ветка только для player_head")
                if (action.name.isBlank()) return fail(player, "Укажите ник онлайн-игрока")
            }

            EditAction.TrimClear -> {
                if (!context.snapshot.isArmor) return fail(player, "Эта ветка только для armor items")
                if (meta !is org.bukkit.inventory.meta.ArmorMeta) return fail(player, "Item meta не поддерживает ArmorMeta")
            }
            is EditAction.TrimSet -> {
                if (!context.snapshot.isArmor) return fail(player, "Эта ветка только для armor items")
                if (meta !is org.bukkit.inventory.meta.ArmorMeta) return fail(player, "Item meta не поддерживает ArmorMeta")
            }
            EditAction.PotClear,
            is EditAction.PotSet,
            is EditAction.PotSetSide -> {
                if (context.item.type != Material.DECORATED_POT) return fail(player, "Эта ветка только для minecraft:decorated_pot")
                val materials = when (action) {
                    is EditAction.PotSet -> listOf(action.back, action.left, action.right, action.front)
                    is EditAction.PotSetSide -> listOf(action.material)
                    else -> emptyList()
                }
                val unsupported = materials.firstOrNull { !EditTrimPotSupport.potDecorationMaterialIds.contains(it.key.asString()) }
                if (unsupported != null) return fail(player, "Недопустимый pot item id: ${unsupported.key.asString()}")
            }
            is EditAction.AttributeAdd, is EditAction.AttributeClear, is EditAction.AttributeRemove -> {
                if (!context.snapshot.isArmor) {
                    return fail(player, "attribute modifiers в этой команде доступны только для armor items")
                }
            }
            is EditAction.EquippableSetEquipSound -> {
                val key = action.keyOrDefault
                if (key != null && !isValidKey(key.asString())) return fail(player, "Некорректный namespaced key для equip_sound")
                if (key == null && context.item.type.getDefaultData(DataComponentTypes.EQUIPPABLE) == null) {
                    return fail(player, "Для этого material нельзя восстановить default equip sound")
                }
            }
            is EditAction.EquippableSetCameraOverlay -> {
                val key = action.keyOrNull
                if (key != null && !isValidKey(key.asString())) return fail(player, "Некорректный namespaced key для camera_overlay")
                if (context.item.getData(DataComponentTypes.EQUIPPABLE) == null && context.item.type.getDefaultData(DataComponentTypes.EQUIPPABLE) == null) {
                    return fail(player, "Сначала /edit equippable slot ...")
                }
            }
            is EditAction.EquippableSetAssetId -> {
                val key = action.keyOrNull
                if (key != null && !isValidKey(key.asString())) return fail(player, "Некорректный namespaced key для asset_id")
                if (context.item.getData(DataComponentTypes.EQUIPPABLE) == null && context.item.type.getDefaultData(DataComponentTypes.EQUIPPABLE) == null) {
                    return fail(player, "Сначала /edit equippable slot ...")
                }
            }
            is EditAction.EquippableSetDispensable,
            is EditAction.EquippableSetSwappable,
            is EditAction.EquippableSetDamageOnHurt -> {
                if (context.item.getData(DataComponentTypes.EQUIPPABLE) == null && context.item.type.getDefaultData(DataComponentTypes.EQUIPPABLE) == null) {
                    return fail(player, "Сначала /edit equippable slot ...")
                }
            }
            EditAction.RemainderSetFromOffhand -> {
                val offhand = player.inventory.itemInOffHand
                if (offhand.type == Material.AIR || offhand.amount <= 0) {
                    return fail(player, "Для remainder set держите предмет во второй руке.")
                }
            }
            EditAction.LockSetFromOffhand -> {
                if (!context.snapshot.isShulker) return fail(player, "Эта ветка только для shulker box item")
                val offhand = player.inventory.itemInOffHand
                if (offhand.type == Material.AIR || offhand.amount <= 0) {
                    return fail(player, "Для lock set держите предмет-ключ во второй руке.")
                }
                val blockStateMeta = meta as? BlockStateMeta ?: return fail(player, "Item meta не поддерживает block state (BlockStateMeta)")
                val state = blockStateMeta.blockState
                if (state !is Lockable) return fail(player, "Block state этого shulker не поддерживает lock API")
            }
            EditAction.LockClear -> {
                if (!context.snapshot.isShulker) return fail(player, "Эта ветка только для shulker box item")
                val blockStateMeta = meta as? BlockStateMeta ?: return fail(player, "Item meta не поддерживает block state (BlockStateMeta)")
                val state = blockStateMeta.blockState
                if (state !is Lockable) return fail(player, "Block state этого shulker не поддерживает lock API")
            }
            is EditAction.ContainerSetSlotFromOffhand -> {
                val capacity = EditContainerSupport.containerCapacity(context.item.type)
                    ?: return fail(player, "Этот предмет не поддерживает /edit container")
                if (action.index < 0 || action.index >= capacity) {
                    return fail(player, "Для ${context.item.type.key.asString()} доступно $capacity слотов: 0..${capacity - 1}")
                }
                val offhand = player.inventory.itemInOffHand
                if (EditContainerSupport.isEmpty(offhand)) {
                    return fail(player, "Во второй руке должен быть предмет (не AIR) для установки в container slot")
                }
            }

            else -> Unit
        }
        return null
    }
}
