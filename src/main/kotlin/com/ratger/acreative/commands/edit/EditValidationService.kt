package com.ratger.acreative.commands.edit

import io.papermc.paper.datacomponent.DataComponentTypes
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.entity.Player
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

            is EditAction.HeadTextureSet -> {
                if (!context.snapshot.isHead) return fail(player, "Эта ветка только для player_head")
                if (action.base64.isBlank()) return fail(player, "texture base64 пустая")
            }

            is EditAction.HeadTextureClear -> {
                if (!context.snapshot.isHead) return fail(player, "Эта ветка только для player_head")
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

            else -> Unit
        }
        return null
    }
}
