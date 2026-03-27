@file:Suppress("UnstableApiUsage")

package com.ratger.acreative.commands.edit

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.Consumable
import io.papermc.paper.datacomponent.item.DeathProtection
import io.papermc.paper.datacomponent.item.FoodProperties
import io.papermc.paper.datacomponent.item.Tool
import io.papermc.paper.datacomponent.item.UseCooldown
import io.papermc.paper.datacomponent.item.UseRemainder
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class EditExperimentalComponentsService {
    private val mini = MiniMessage.miniMessage()

    fun supports(action: EditAction): Boolean {
        return action is EditAction.ConsumableToggle ||
            action is EditAction.ConsumableAnimation ||
            action is EditAction.ConsumableHasParticles ||
            action is EditAction.ConsumableConsumeSeconds ||
            action is EditAction.ConsumableSound ||
            action is EditAction.ConsumableEffectAdd ||
            action is EditAction.ConsumableEffectRemove ||
            action is EditAction.ConsumableEffectClear ||
            action is EditAction.DeathProtectionToggle ||
            action is EditAction.DeathProtectionEffectAdd ||
            action is EditAction.DeathProtectionEffectRemove ||
            action is EditAction.DeathProtectionEffectClear ||
            action is EditAction.FoodNutrition ||
            action is EditAction.FoodSaturation ||
            action is EditAction.FoodCanAlwaysEat ||
            action is EditAction.RemainderSetFromOffhand ||
            action is EditAction.RemainderClear ||
            action is EditAction.EquippableSetSlot ||
            action is EditAction.EquippableClear ||
            action is EditAction.EquippableSetDispensable ||
            action is EditAction.EquippableSetSwappable ||
            action is EditAction.EquippableSetDamageOnHurt ||
            action is EditAction.EquippableSetEquipSound ||
            action is EditAction.EquippableSetCameraOverlay ||
            action is EditAction.EquippableSetAssetId ||
            action is EditAction.ToolSetDefaultMiningSpeed ||
            action is EditAction.ToolSetDamagePerBlock ||
            action is EditAction.ToolClear ||
            action is EditAction.SetUseCooldown ||
            action is EditAction.ClearUseCooldown ||
            action is EditAction.ContainerSetSlotFromOffhand ||
            action is EditAction.PotClear ||
            action is EditAction.PotSet ||
            action is EditAction.PotSetSide
    }

    fun apply(player: Player, action: EditAction, item: ItemStack): EditResult {
        when (action) {
            is EditAction.ConsumableToggle -> applyConsumableToggle(item, action.enabled)
            is EditAction.ConsumableAnimation -> {
                val builder = consumableBuilder(item)
                builder.animation(action.animation)
                item.setData(DataComponentTypes.CONSUMABLE, builder.build())
            }
            is EditAction.ConsumableHasParticles -> {
                val builder = consumableBuilder(item)
                builder.hasConsumeParticles(action.value)
                item.setData(DataComponentTypes.CONSUMABLE, builder.build())
            }
            is EditAction.ConsumableConsumeSeconds -> {
                val builder = consumableBuilder(item)
                builder.consumeSeconds(action.value)
                item.setData(DataComponentTypes.CONSUMABLE, builder.build())
            }
            is EditAction.ConsumableSound -> {
                val builder = consumableBuilder(item)
                if (action.key == null) {
                    val defaultConsumable = item.type.getDefaultData(DataComponentTypes.CONSUMABLE)
                    if (defaultConsumable != null) {
                        builder.sound(defaultConsumable.sound())
                    } else {
                        return EditResult(
                            false,
                            listOf(mini.deserialize("<yellow>Для этого предмета нельзя восстановить default consumable sound, потому что у material нет prototype consumable.")),
                            warning = true
                        )
                    }
                } else {
                    builder.sound(action.key)
                }
                item.setData(DataComponentTypes.CONSUMABLE, builder.build())
            }
            is EditAction.ConsumableEffectAdd -> {
                val consumable = item.getData(DataComponentTypes.CONSUMABLE) ?: Consumable.consumable().build()
                item.setData(DataComponentTypes.CONSUMABLE, EditConsumeEffectsAdapter.addConsumableEffect(consumable, action.spec))
            }
            is EditAction.ConsumableEffectRemove -> {
                val consumable = item.getData(DataComponentTypes.CONSUMABLE)
                    ?: return EditResult(false, listOf(mini.deserialize("<red>У предмета нет компонента consumable")))
                val next = EditConsumeEffectsAdapter.removeConsumableEffect(consumable, action.index)
                    ?: return EditResult(false, listOf(mini.deserialize("<red>Некорректный индекс effect_remove")))
                item.setData(DataComponentTypes.CONSUMABLE, next)
            }
            EditAction.ConsumableEffectClear -> {
                val consumable = item.getData(DataComponentTypes.CONSUMABLE)
                    ?: return EditResult(false, listOf(mini.deserialize("<red>У предмета нет компонента consumable")))
                item.setData(DataComponentTypes.CONSUMABLE, EditConsumeEffectsAdapter.clearConsumableEffects(consumable))
            }
            is EditAction.DeathProtectionToggle -> {
                if (action.enabled) {
                    val current = item.getData(DataComponentTypes.DEATH_PROTECTION)
                    if (current == null) {
                        item.setData(DataComponentTypes.DEATH_PROTECTION, DeathProtection.deathProtection().build())
                    }
                } else {
                    item.unsetData(DataComponentTypes.DEATH_PROTECTION)
                }
            }
            is EditAction.DeathProtectionEffectAdd -> {
                val current = item.getData(DataComponentTypes.DEATH_PROTECTION) ?: DeathProtection.deathProtection().build()
                item.setData(DataComponentTypes.DEATH_PROTECTION, EditConsumeEffectsAdapter.addDeathProtectionEffect(current, action.spec))
            }
            is EditAction.DeathProtectionEffectRemove -> {
                val current = item.getData(DataComponentTypes.DEATH_PROTECTION)
                    ?: return EditResult(false, listOf(mini.deserialize("<red>У предмета нет компонента death_protection")))
                val next = EditConsumeEffectsAdapter.removeDeathProtectionEffect(current, action.index)
                    ?: return EditResult(false, listOf(mini.deserialize("<red>Некорректный индекс effect_remove")))
                item.setData(DataComponentTypes.DEATH_PROTECTION, next)
            }
            EditAction.DeathProtectionEffectClear -> {
                if (item.getData(DataComponentTypes.DEATH_PROTECTION) == null) {
                    return EditResult(false, listOf(mini.deserialize("<red>У предмета нет компонента death_protection")))
                }
                item.setData(DataComponentTypes.DEATH_PROTECTION, EditConsumeEffectsAdapter.clearDeathProtectionEffects())
            }
            is EditAction.FoodNutrition -> applyFoodNutrition(item, action.value)
            is EditAction.FoodSaturation -> applyFoodSaturation(item, action.value)
            is EditAction.FoodCanAlwaysEat -> applyFoodCanAlwaysEat(item, action.value)
            EditAction.RemainderSetFromOffhand -> item.setData(DataComponentTypes.USE_REMAINDER, UseRemainder.useRemainder(player.inventory.itemInOffHand.clone()))
            EditAction.RemainderClear -> item.unsetData(DataComponentTypes.USE_REMAINDER)
            is EditAction.EquippableSetSlot -> {
                if (!EditEquippableSupport.setSlot(item, action.slot)) {
                    return EditResult(false, listOf(mini.deserialize("<red>Не удалось создать equippable snapshot для этого предмета")))
                }
            }
            EditAction.EquippableClear -> EditEquippableSupport.clear(item)
            is EditAction.EquippableSetDispensable -> {
                if (!EditEquippableSupport.setDispensable(item, action.value)) {
                    return EditResult(false, listOf(mini.deserialize("<red>Сначала установите slot через /edit equippable slot ...")))
                }
            }
            is EditAction.EquippableSetSwappable -> {
                if (!EditEquippableSupport.setSwappable(item, action.value)) {
                    return EditResult(false, listOf(mini.deserialize("<red>Сначала установите slot через /edit equippable slot ...")))
                }
            }
            is EditAction.EquippableSetDamageOnHurt -> {
                if (!EditEquippableSupport.setDamageOnHurt(item, action.value)) {
                    return EditResult(false, listOf(mini.deserialize("<red>Сначала установите slot через /edit equippable slot ...")))
                }
            }
            is EditAction.EquippableSetEquipSound -> {
                if (action.keyOrDefault == null) {
                    if (!EditEquippableSupport.restoreDefaultEquipSound(item)) {
                        return EditResult(
                            false,
                            listOf(mini.deserialize("<yellow>Для этого предмета нельзя восстановить default equip sound, потому что у material нет prototype equippable.")),
                            warning = true
                        )
                    }
                } else if (!EditEquippableSupport.setEquipSound(item, action.keyOrDefault)) {
                    return EditResult(false, listOf(mini.deserialize("<red>Сначала установите slot через /edit equippable slot ...")))
                }
            }
            is EditAction.EquippableSetCameraOverlay -> {
                if (!EditEquippableSupport.setCameraOverlay(item, action.keyOrNull)) {
                    return EditResult(false, listOf(mini.deserialize("<red>Сначала установите slot через /edit equippable slot ...")))
                }
            }
            is EditAction.EquippableSetAssetId -> {
                if (!EditEquippableSupport.setAssetId(item, action.keyOrNull)) {
                    return EditResult(false, listOf(mini.deserialize("<red>Сначала установите slot через /edit equippable slot ...")))
                }
            }
            is EditAction.ToolSetDefaultMiningSpeed -> applyToolSpeed(item, action.value, action.scope)
            is EditAction.ToolSetDamagePerBlock -> {
                val builder = toolBuilder(item)
                builder.damagePerBlock(action.value)
                item.setData(DataComponentTypes.TOOL, builder.build())
            }
            EditAction.ToolClear -> item.unsetData(DataComponentTypes.TOOL)
            is EditAction.SetUseCooldown -> applyUseCooldown(item, action.seconds, action.cooldownGroup)
            EditAction.ClearUseCooldown -> item.unsetData(DataComponentTypes.USE_COOLDOWN)
            is EditAction.ContainerSetSlotFromOffhand -> {
                val snapshot = EditContainerSupport.readContainerContents(item)
                    ?: return EditResult(false, listOf(mini.deserialize("<red>Этот предмет не поддерживает /edit container на стабильном BlockState API")))
                snapshot.contents[action.index] = player.inventory.itemInOffHand.clone()
                if (!EditContainerSupport.applyContainerContents(item, snapshot.contents)) {
                    return EditResult(false, listOf(mini.deserialize("<red>Не удалось применить container через стабильный BlockState API")))
                }
            }
            EditAction.PotClear -> {
                if (!EditTrimPotSupport.applyDecorations(item, null, null, null, null)) {
                    return EditResult(false, listOf(mini.deserialize("<red>Не удалось применить pot-декорации: требуется decorated pot item с BlockStateMeta")))
                }
            }
            is EditAction.PotSet -> {
                if (!EditTrimPotSupport.applyDecorations(item, action.back, action.left, action.right, action.front)) {
                    return EditResult(false, listOf(mini.deserialize("<red>Не удалось применить pot-декорации: требуется decorated pot item с BlockStateMeta")))
                }
            }
            is EditAction.PotSetSide -> {
                if (!EditTrimPotSupport.applySide(item, action.side, action.material)) {
                    return EditResult(false, listOf(mini.deserialize("<red>Не удалось применить pot-декорации: требуется decorated pot item с BlockStateMeta")))
                }
            }
            else -> return EditResult(false, listOf(mini.deserialize("<red>Ветка не поддерживается для data components")))
        }

        return EditResult(true, listOf(mini.deserialize("<green>Изменение применено.")))
    }

    fun applyConsumableToggle(item: ItemStack, enabled: Boolean) {
        if (enabled) {
            if (item.getData(DataComponentTypes.CONSUMABLE) == null) {
                item.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable().build())
            }
            return
        }
        item.unsetData(DataComponentTypes.CONSUMABLE)
        item.unsetData(DataComponentTypes.FOOD)
    }

    fun applyFoodNutrition(item: ItemStack, value: Int) {
        ensureConsumableData(item)
        val builder = foodBuilder(item)
        builder.nutrition(value)
        item.setData(DataComponentTypes.FOOD, builder.build())
    }

    fun applyFoodSaturation(item: ItemStack, value: Float) {
        ensureConsumableData(item)
        val builder = foodBuilder(item)
        builder.saturation(value)
        item.setData(DataComponentTypes.FOOD, builder.build())
    }

    fun applyFoodCanAlwaysEat(item: ItemStack, value: Boolean) {
        ensureConsumableData(item)
        val builder = foodBuilder(item)
        builder.canAlwaysEat(value)
        item.setData(DataComponentTypes.FOOD, builder.build())
    }

    fun applyUseCooldown(item: ItemStack, seconds: Float, cooldownGroup: net.kyori.adventure.key.Key?) {
        val builder = UseCooldown.useCooldown(seconds)
        builder.cooldownGroup(cooldownGroup)
        item.setData(DataComponentTypes.USE_COOLDOWN, builder.build())
    }

    private fun consumableBuilder(item: ItemStack): Consumable.Builder {
        val current = item.getData(DataComponentTypes.CONSUMABLE)
        return current?.toBuilder() ?: Consumable.consumable()
    }

    private fun ensureConsumableData(item: ItemStack) {
        if (item.getData(DataComponentTypes.CONSUMABLE) == null) {
            item.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable().build())
        }
    }

    private fun foodBuilder(item: ItemStack): FoodProperties.Builder {
        val current = item.getData(DataComponentTypes.FOOD)
        return current?.toBuilder() ?: FoodProperties.food()
    }

    private fun toolBuilder(item: ItemStack): Tool.Builder {
        val current = item.getData(DataComponentTypes.TOOL)
        val builder = Tool.tool()
        if (current != null) {
            builder.defaultMiningSpeed(current.defaultMiningSpeed())
            builder.damagePerBlock(current.damagePerBlock())
            builder.addRules(current.rules())
        }
        return builder
    }

    private fun applyToolSpeed(item: ItemStack, speed: Float, scope: ToolSpeedScope) {
        val current = item.getData(DataComponentTypes.TOOL)
        if (current == null) {
            item.setData(DataComponentTypes.TOOL, Tool.tool().defaultMiningSpeed(speed).build())
            return
        }

        val hasSpeedRules = hasSpeedBearingRules(current)
        if (!hasSpeedRules || scope == ToolSpeedScope.INEFFECTIVE_ONLY) {
            val rebuilt = Tool.tool()
                .defaultMiningSpeed(speed)
                .damagePerBlock(current.damagePerBlock())
                .addRules(current.rules())
                .build()
            item.setData(DataComponentTypes.TOOL, rebuilt)
            return
        }

        val rebuilt = Tool.tool()
            .defaultMiningSpeed(if (scope == ToolSpeedScope.ALL_BLOCKS) speed else current.defaultMiningSpeed())
            .damagePerBlock(current.damagePerBlock())

        current.rules().forEach { rule ->
            val updatedSpeed = when (scope) {
                ToolSpeedScope.ALL_BLOCKS, ToolSpeedScope.EFFECTIVE_ONLY -> rule.speed()?.let { speed }
                ToolSpeedScope.INEFFECTIVE_ONLY -> rule.speed()
            }
            rebuilt.addRule(Tool.rule(rule.blocks(), updatedSpeed, rule.correctForDrops()))
        }

        item.setData(DataComponentTypes.TOOL, rebuilt.build())
    }

    private fun hasSpeedBearingRules(tool: Tool): Boolean = tool.rules().any { it.speed() != null }
}
