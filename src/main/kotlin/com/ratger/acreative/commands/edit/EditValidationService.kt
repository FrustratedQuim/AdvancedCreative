package com.ratger.acreative.commands.edit

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.Damageable

class EditValidationService {
    private val mini = MiniMessage.miniMessage()

    fun fail(player: Player, message: String): EditResult {
        return EditResult(false, listOf(mini.deserialize("<red>$message")))
    }

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

            else -> Unit
        }
        return null
    }
}
