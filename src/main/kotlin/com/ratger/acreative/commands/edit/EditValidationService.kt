package com.ratger.acreative.commands.edit

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.block.Lockable
import org.bukkit.Material
import org.bukkit.Registry
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.BlockStateMeta
import org.bukkit.inventory.meta.Damageable

class EditValidationService {
    private val mini = MiniMessage.miniMessage()

    fun fail(message: String): EditResult {
        return EditResult(false, listOf(mini.deserialize("<red>$message")))
    }

    fun isValidKey(raw: String): Boolean = runCatching { Key.key(raw) }.isSuccess

    fun validate(action: EditAction, context: EditContext, player: Player): EditResult? {
        val meta = context.item.itemMeta
        when (action) {
            is EditAction.SetDamage -> {
                if (action.value < 0) return fail("damage не может быть отрицательным")
                val damageable = meta as? Damageable
                val maxDamage = if (damageable?.hasMaxDamage() == true) damageable.maxDamage else null
                if (maxDamage != null && action.value > maxDamage) return fail("damage не может быть больше max_damage ($maxDamage)")
            }

            is EditAction.SetMaxDamage -> {
                val value = action.value ?: return null
                if (value <= 0) return fail("max_damage должен быть > 0")
                val stack = if (meta?.hasMaxStackSize() == true) meta.maxStackSize else null
                if (stack != null && stack > 1) {
                    return fail("max_damage конфликтует с max_stack_size > 1")
                }
            }

            is EditAction.SetMaxStackSize -> {
                val value = action.value ?: return null
                if (value <= 0 || value > 99) return fail("max_stack_size должен быть в диапазоне 1..99")
                val damageable = meta as? Damageable
                val maxDamage = if (damageable?.hasMaxDamage() == true) damageable.maxDamage else null
                if (value > 1 && maxDamage != null) return fail("max_stack_size > 1 конфликтует с max_damage")
            }

            is EditAction.EnchantAdd -> {
                if (action.level <= 0) return fail("уровень чар должен быть > 0")
                if (action.level > 255) return fail("уровень чар больше hard cap API (255)")
            }

            is EditAction.PotionEffectAdd -> {
                if (!context.snapshot.isPotion) return fail("Эта ветка только для potion/splash/lingering/tipped_arrow")
                if (action.duration < 0 || action.amplifier < 0) return fail("duration/amplifier не могут быть отрицательными")
                if (action.duration > Int.MAX_VALUE / 2) return fail("duration слишком большой")
                if (action.amplifier > 255) return fail("amplifier > 255 не допускается")
            }

            is EditAction.PotionColor -> {
                if (!context.snapshot.isPotion) return fail("Цвет зелий доступен только для potion предметов")
            }
            is EditAction.ConsumableConsumeSeconds -> {
                if (!action.value.isFinite()) return fail("consume_seconds должен быть конечным числом")
                if (action.value <= 0f) return fail("consume_seconds должен быть > 0")
                if (action.value > 60f) return fail("consume_seconds слишком большой")
            }
            is EditAction.FoodNutrition -> {
                if (action.value < 0) return fail("nutrition не может быть отрицательным")
                if (action.value > 1000) return fail("nutrition слишком большой")
            }
            is EditAction.FoodSaturation -> {
                if (!action.value.isFinite()) return fail("saturation должен быть конечным числом")
                if (action.value < 0f) return fail("saturation не может быть отрицательным")
                if (action.value > 1000f) return fail("saturation слишком большой")
            }
            is EditAction.ToolSetDefaultMiningSpeed -> {
                if (!action.value.isFinite()) return fail("tool speed должен быть конечным числом")
                if (action.value < 0f) return fail("tool speed не может быть отрицательным")
            }
            is EditAction.ToolSetDamagePerBlock -> {
                if (action.value < 0) return fail("tool damage_per_block не может быть отрицательным")
            }
            is EditAction.SetUseCooldown -> {
                if (!action.seconds.isFinite()) return fail("use_cooldown seconds должен быть конечным числом")
                if (action.seconds <= 0f) return fail("use_cooldown seconds должен быть > 0")
                val group = action.cooldownGroup
                if (group != null && !isValidKey(group.asString())) return fail("Некорректный namespaced key для use_cooldown group")
            }
            is EditAction.ConsumableEffectAdd -> {
                val message = EditEffectActionsSupport.validateSpec(action.spec, this)
                if (message != null) return fail(message)
            }
            is EditAction.DeathProtectionEffectAdd -> {
                val message = EditEffectActionsSupport.validateSpec(action.spec, this)
                if (message != null) return fail(message)
            }
            is EditAction.ConsumableEffectRemove -> {
                val consumable = context.item.getData(io.papermc.paper.datacomponent.DataComponentTypes.CONSUMABLE)
                    ?: return fail("У предмета нет компонента consumable")
                if (action.index !in consumable.consumeEffects().indices) return fail("Некорректный индекс effect_remove")
            }
            is EditAction.DeathProtectionEffectRemove -> {
                val dp = context.item.getData(io.papermc.paper.datacomponent.DataComponentTypes.DEATH_PROTECTION)
                    ?: return fail("У предмета нет компонента death_protection")
                if (action.index !in dp.deathEffects().indices) return fail("Некорректный индекс effect_remove")
            }

            is EditAction.HeadSetFromTexture -> {
                if (!context.snapshot.isHead) return fail("Эта ветка только для player_head")
                if (action.base64.isBlank()) return fail("texture base64 пустая")
            }

            EditAction.HeadClear -> {
                if (!context.snapshot.isHead) return fail("Эта ветка только для player_head")
            }

            is EditAction.HeadSetFromName -> {
                if (!context.snapshot.isHead) return fail("Эта ветка только для player_head")
                if (!action.name.matches(Regex("^[A-Za-z0-9_]{3,16}$"))) return fail("Некорректный licensed_name (ожидается 3..16: A-Z a-z 0-9 _)")
            }

            is EditAction.HeadSetFromOnline -> {
                if (!context.snapshot.isHead) return fail("Эта ветка только для player_head")
                if (action.name.isBlank()) return fail("Укажите ник онлайн-игрока")
            }

            EditAction.TrimClear -> {
                if (!context.snapshot.isArmor) return fail("Эта ветка только для armor items")
                if (meta !is org.bukkit.inventory.meta.ArmorMeta) return fail("Item meta не поддерживает ArmorMeta")
            }
            is EditAction.TrimSet -> {
                if (!context.snapshot.isArmor) return fail("Эта ветка только для armor items")
                if (meta !is org.bukkit.inventory.meta.ArmorMeta) return fail("Item meta не поддерживает ArmorMeta")
            }
            EditAction.PotClear,
            is EditAction.PotSet,
            is EditAction.PotSetSide -> {
                if (context.item.type != Material.DECORATED_POT) return fail("Эта ветка только для minecraft:decorated_pot")
                val materials = when (action) {
                    is EditAction.PotSet -> listOf(action.back, action.left, action.right, action.front)
                    is EditAction.PotSetSide -> listOf(action.material)
                    else -> emptyList()
                }
                val unsupported = materials.firstOrNull { !EditTrimPotSupport.potDecorationMaterialIds.contains(it.key.asString()) }
                if (unsupported != null) return fail("Недопустимый pot item id: ${unsupported.key.asString()}")
            }
            is EditAction.AttributeAdd, is EditAction.AttributeClear, is EditAction.AttributeRemove -> {
                if (!context.snapshot.isArmor) {
                    return fail("attribute modifiers в этой команде доступны только для armor items")
                }
            }
            is EditAction.EquippableSetEquipSound -> {
                val key = action.keyOrDefault
                if (key != null) {
                    if (!isValidKey(key.asString())) return fail("Некорректный namespaced key для equip_sound")
                    val namespaced = NamespacedKey.fromString(key.asString())
                    if (namespaced == null || Registry.SOUNDS.get(namespaced) == null) {
                        return fail("Неизвестный sound key для equip_sound")
                    }
                }
                if (key == null && EditEquippableSupport.prototypeSnapshot(context.item) == null) {
                    return fail("Для этого material нельзя восстановить default equip sound")
                }
            }
            is EditAction.EquippableSetCameraOverlay -> {
                val key = action.keyOrNull
                if (key != null && !isValidKey(key.asString())) return fail("Некорректный namespaced key для camera_overlay")
                if (!EditEquippableSupport.hasExistingOrPrototype(context.item)) {
                    return fail("Сначала /edit equippable slot ...")
                }
            }
            is EditAction.EquippableSetAssetId -> {
                val key = action.keyOrNull
                if (key != null && !isValidKey(key.asString())) return fail("Некорректный namespaced key для asset_id")
                if (!EditEquippableSupport.hasExistingOrPrototype(context.item)) {
                    return fail("Сначала /edit equippable slot ...")
                }
            }
            is EditAction.EquippableSetDispensable,
            is EditAction.EquippableSetSwappable,
            is EditAction.EquippableSetDamageOnHurt -> {
                if (!EditEquippableSupport.hasExistingOrPrototype(context.item)) {
                    return fail("Сначала /edit equippable slot ...")
                }
            }
            EditAction.RemainderSetFromOffhand -> {
                val offhand = player.inventory.itemInOffHand
                if (offhand.type == Material.AIR || offhand.amount <= 0) {
                    return fail("Для remainder set держите предмет во второй руке.")
                }
            }
            EditAction.LockSetFromOffhand -> {
                if (!context.snapshot.isShulker) return fail("Эта ветка только для shulker box item")
                val offhand = player.inventory.itemInOffHand
                if (offhand.type == Material.AIR || offhand.amount <= 0) {
                    return fail("Для lock set держите предмет-ключ во второй руке.")
                }
                val blockStateMeta = meta as? BlockStateMeta ?: return fail("Item meta не поддерживает block state (BlockStateMeta)")
                val state = blockStateMeta.blockState
                if (state !is Lockable) return fail("Block state этого shulker не поддерживает lock API")
            }
            EditAction.LockClear -> {
                if (!context.snapshot.isShulker) return fail("Эта ветка только для shulker box item")
                val blockStateMeta = meta as? BlockStateMeta ?: return fail("Item meta не поддерживает block state (BlockStateMeta)")
                val state = blockStateMeta.blockState
                if (state !is Lockable) return fail("Block state этого shulker не поддерживает lock API")
            }
            is EditAction.ContainerSetSlotFromOffhand -> {
                val capacity = EditContainerSupport.containerCapacity(context.item.type)
                    ?: return fail("Этот предмет не поддерживает /edit container")
                if (action.index < 0 || action.index >= capacity) {
                    return fail("Для ${context.item.type.key.asString()} доступно $capacity слотов: 0..${capacity - 1}")
                }
                if (!EditContainerSupport.supportsStableContainerEditing(context.item)) {
                    return fail("Для ${context.item.type.key.asString()} стабильный BlockState путь контейнера недоступен")
                }
                val offhand = player.inventory.itemInOffHand
                if (EditContainerSupport.isEmpty(offhand)) {
                    return fail("Во второй руке должен быть предмет (не AIR) для установки в container slot")
                }
            }

            else -> Unit
        }
        return null
    }
}
