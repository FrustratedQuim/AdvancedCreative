@file:Suppress("UnstableApiUsage") // Experimental Consumable/Tool/etc

package com.ratger.acreative.itemedit.experimental

import com.ratger.acreative.itemedit.api.ItemAction
import com.ratger.acreative.itemedit.api.ItemResult
import com.ratger.acreative.itemedit.api.ToolSpeedScope
import com.ratger.acreative.itemedit.container.ContainerSupport
import com.ratger.acreative.itemedit.effects.ConsumeEffectsAdapter
import com.ratger.acreative.itemedit.equippable.EquippableSupport
import com.ratger.acreative.itemedit.remainder.UseRemainderSupport
import com.ratger.acreative.itemedit.trim.TrimPotSupport
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.Consumable
import io.papermc.paper.datacomponent.item.DeathProtection
import io.papermc.paper.datacomponent.item.FoodProperties
import io.papermc.paper.datacomponent.item.Tool
import io.papermc.paper.datacomponent.item.UseCooldown
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class ComponentsService {
    private val mini = MiniMessage.miniMessage()

    fun supports(action: ItemAction): Boolean {
        return action is ItemAction.ConsumableToggle ||
            action is ItemAction.ConsumableAnimation ||
            action is ItemAction.ConsumableHasParticles ||
            action is ItemAction.ConsumableConsumeSeconds ||
            action is ItemAction.ConsumableSound ||
            action is ItemAction.ConsumableEffectAdd ||
            action is ItemAction.ConsumableEffectRemove ||
            action is ItemAction.ConsumableEffectClear ||
            action is ItemAction.DeathProtectionToggle ||
            action is ItemAction.DeathProtectionEffectAdd ||
            action is ItemAction.DeathProtectionEffectRemove ||
            action is ItemAction.DeathProtectionEffectClear ||
            action is ItemAction.FoodNutrition ||
            action is ItemAction.FoodSaturation ||
            action is ItemAction.FoodCanAlwaysEat ||
            action is ItemAction.RemainderSetFromOffhand ||
            action is ItemAction.RemainderClear ||
            action is ItemAction.EquippableSetSlot ||
            action is ItemAction.EquippableClear ||
            action is ItemAction.EquippableSetDispensable ||
            action is ItemAction.EquippableSetSwappable ||
            action is ItemAction.EquippableSetDamageOnHurt ||
            action is ItemAction.EquippableSetEquipSound ||
            action is ItemAction.EquippableSetCameraOverlay ||
            action is ItemAction.EquippableSetAssetId ||
            action is ItemAction.ToolSetDefaultMiningSpeed ||
            action is ItemAction.ToolSetDamagePerBlock ||
            action is ItemAction.ToolClear ||
            action is ItemAction.SetUseCooldown ||
            action is ItemAction.ClearUseCooldown ||
            action is ItemAction.ContainerSetSlotFromOffhand ||
            action is ItemAction.PotClear ||
            action is ItemAction.PotSet ||
            action is ItemAction.PotSetSide
    }

    fun apply(player: Player, action: ItemAction, item: ItemStack): ItemResult {
        when (action) {
            is ItemAction.ConsumableToggle -> applyConsumableToggle(item, action.enabled)
            is ItemAction.ConsumableAnimation -> {
                val builder = consumableBuilder(item)
                builder.animation(action.animation)
                item.setData(DataComponentTypes.CONSUMABLE, builder.build())
            }
            is ItemAction.ConsumableHasParticles -> {
                val builder = consumableBuilder(item)
                builder.hasConsumeParticles(action.value)
                item.setData(DataComponentTypes.CONSUMABLE, builder.build())
            }
            is ItemAction.ConsumableConsumeSeconds -> {
                val builder = consumableBuilder(item)
                builder.consumeSeconds(action.value)
                item.setData(DataComponentTypes.CONSUMABLE, builder.build())
            }
            is ItemAction.ConsumableSound -> {
                val builder = consumableBuilder(item)
                if (action.key == null) {
                    val defaultConsumable = item.type.getDefaultData(DataComponentTypes.CONSUMABLE)
                    if (defaultConsumable != null) {
                        builder.sound(defaultConsumable.sound())
                    } else {
                        return ItemResult(
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
            is ItemAction.ConsumableEffectAdd -> {
                val consumable = item.getData(DataComponentTypes.CONSUMABLE) ?: Consumable.consumable().build()
                item.setData(DataComponentTypes.CONSUMABLE, ConsumeEffectsAdapter.addConsumableEffect(consumable, action.spec))
            }
            is ItemAction.ConsumableEffectRemove -> {
                val consumable = item.getData(DataComponentTypes.CONSUMABLE)
                    ?: return ItemResult(false, listOf(mini.deserialize("<red>У предмета нет компонента consumable")))
                val next = ConsumeEffectsAdapter.removeConsumableEffect(consumable, action.index)
                    ?: return ItemResult(false, listOf(mini.deserialize("<red>Некорректный индекс effect_remove")))
                item.setData(DataComponentTypes.CONSUMABLE, next)
            }
            ItemAction.ConsumableEffectClear -> {
                val consumable = item.getData(DataComponentTypes.CONSUMABLE)
                    ?: return ItemResult(false, listOf(mini.deserialize("<red>У предмета нет компонента consumable")))
                item.setData(DataComponentTypes.CONSUMABLE, ConsumeEffectsAdapter.clearConsumableEffects(consumable))
            }
            is ItemAction.DeathProtectionToggle -> {
                if (action.enabled) {
                    val current = item.getData(DataComponentTypes.DEATH_PROTECTION)
                    if (current == null) {
                        item.setData(DataComponentTypes.DEATH_PROTECTION, DeathProtection.deathProtection().build())
                    }
                } else {
                    item.unsetData(DataComponentTypes.DEATH_PROTECTION)
                }
            }
            is ItemAction.DeathProtectionEffectAdd -> {
                val current = item.getData(DataComponentTypes.DEATH_PROTECTION) ?: DeathProtection.deathProtection().build()
                item.setData(DataComponentTypes.DEATH_PROTECTION, ConsumeEffectsAdapter.addDeathProtectionEffect(current, action.spec))
            }
            is ItemAction.DeathProtectionEffectRemove -> {
                val current = item.getData(DataComponentTypes.DEATH_PROTECTION)
                    ?: return ItemResult(false, listOf(mini.deserialize("<red>У предмета нет компонента death_protection")))
                val next = ConsumeEffectsAdapter.removeDeathProtectionEffect(current, action.index)
                    ?: return ItemResult(false, listOf(mini.deserialize("<red>Некорректный индекс effect_remove")))
                item.setData(DataComponentTypes.DEATH_PROTECTION, next)
            }
            ItemAction.DeathProtectionEffectClear -> {
                if (item.getData(DataComponentTypes.DEATH_PROTECTION) == null) {
                    return ItemResult(false, listOf(mini.deserialize("<red>У предмета нет компонента death_protection")))
                }
                item.setData(DataComponentTypes.DEATH_PROTECTION, ConsumeEffectsAdapter.clearDeathProtectionEffects())
            }
            is ItemAction.FoodNutrition -> applyFoodNutrition(item, action.value)
            is ItemAction.FoodSaturation -> applyFoodSaturation(item, action.value)
            is ItemAction.FoodCanAlwaysEat -> applyFoodCanAlwaysEat(item, action.value)
            ItemAction.RemainderSetFromOffhand -> UseRemainderSupport.setOrClear(item, player.inventory.itemInOffHand)
            ItemAction.RemainderClear -> UseRemainderSupport.clear(item)
            is ItemAction.EquippableSetSlot -> {
                if (!EquippableSupport.setSlot(item, action.slot)) {
                    return ItemResult(false, listOf(mini.deserialize("<red>Не удалось создать equippable snapshot для этого предмета")))
                }
            }
            ItemAction.EquippableClear -> EquippableSupport.clear(item)
            is ItemAction.EquippableSetDispensable -> {
                if (!EquippableSupport.setDispensable(item, action.value)) {
                    return ItemResult(false, listOf(mini.deserialize("<red>Сначала установите slot через /dedit equippable slot ...")))
                }
            }
            is ItemAction.EquippableSetSwappable -> {
                if (!EquippableSupport.setSwappable(item, action.value)) {
                    return ItemResult(false, listOf(mini.deserialize("<red>Сначала установите slot через /dedit equippable slot ...")))
                }
            }
            is ItemAction.EquippableSetDamageOnHurt -> {
                if (!EquippableSupport.setDamageOnHurt(item, action.value)) {
                    return ItemResult(false, listOf(mini.deserialize("<red>Сначала установите slot через /dedit equippable slot ...")))
                }
            }
            is ItemAction.EquippableSetEquipSound -> {
                if (action.keyOrDefault == null) {
                    if (!EquippableSupport.restoreDefaultEquipSound(item)) {
                        return ItemResult(
                            false,
                            listOf(mini.deserialize("<yellow>Для этого предмета нельзя восстановить default equip sound, потому что у material нет prototype equippable.")),
                            warning = true
                        )
                    }
                } else if (!EquippableSupport.setEquipSound(item, action.keyOrDefault)) {
                    return ItemResult(false, listOf(mini.deserialize("<red>Сначала установите slot через /dedit equippable slot ...")))
                }
            }
            is ItemAction.EquippableSetCameraOverlay -> {
                if (!EquippableSupport.setCameraOverlay(item, action.keyOrNull)) {
                    return ItemResult(false, listOf(mini.deserialize("<red>Сначала установите slot через /dedit equippable slot ...")))
                }
            }
            is ItemAction.EquippableSetAssetId -> {
                if (!EquippableSupport.setAssetId(item, action.keyOrNull)) {
                    return ItemResult(false, listOf(mini.deserialize("<red>Сначала установите slot через /dedit equippable slot ...")))
                }
            }
            is ItemAction.ToolSetDefaultMiningSpeed -> applyToolSpeed(item, action.value, action.scope)
            is ItemAction.ToolSetDamagePerBlock -> {
                val builder = toolBuilder(item)
                builder.damagePerBlock(action.value)
                item.setData(DataComponentTypes.TOOL, builder.build())
            }
            ItemAction.ToolClear -> item.unsetData(DataComponentTypes.TOOL)
            is ItemAction.SetUseCooldown -> applyUseCooldown(item, action.seconds, action.cooldownGroup)
            ItemAction.ClearUseCooldown -> item.unsetData(DataComponentTypes.USE_COOLDOWN)
            is ItemAction.ContainerSetSlotFromOffhand -> {
                val snapshot = ContainerSupport.readContainerContents(item)
                    ?: return ItemResult(false, listOf(mini.deserialize("<red>Этот предмет не поддерживает /dedit container на стабильном BlockState API")))
                snapshot.contents[action.index] = player.inventory.itemInOffHand.clone()
                if (!ContainerSupport.applyContainerContents(item, snapshot.contents)) {
                    return ItemResult(false, listOf(mini.deserialize("<red>Не удалось применить container через стабильный BlockState API")))
                }
            }
            ItemAction.PotClear -> {
                if (!TrimPotSupport.applyDecorations(item, null, null, null, null)) {
                    return ItemResult(false, listOf(mini.deserialize("<red>Не удалось применить pot-декорации: требуется decorated pot item с BlockStateMeta")))
                }
            }
            is ItemAction.PotSet -> {
                if (!TrimPotSupport.applyDecorations(item, action.back, action.left, action.right, action.front)) {
                    return ItemResult(false, listOf(mini.deserialize("<red>Не удалось применить pot-декорации: требуется decorated pot item с BlockStateMeta")))
                }
            }
            is ItemAction.PotSetSide -> {
                if (!TrimPotSupport.applySide(item, action.side, action.material)) {
                    return ItemResult(false, listOf(mini.deserialize("<red>Не удалось применить pot-декорации: требуется decorated pot item с BlockStateMeta")))
                }
            }
            else -> return ItemResult(false, listOf(mini.deserialize("<red>Ветка не поддерживается для data components")))
        }

        return ItemResult(true, listOf(mini.deserialize("<green>Изменение применено.")))
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

        if (scope == ToolSpeedScope.INEFFECTIVE_ONLY) {
            val rebuilt = Tool.tool()
                .defaultMiningSpeed(speed)
                .damagePerBlock(current.damagePerBlock())
                .addRules(current.rules())
                .build()
            item.setData(DataComponentTypes.TOOL, rebuilt)
            return
        }

        val hasSpeedRules = hasSpeedBearingRules(current)
        if (!hasSpeedRules) {
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
            val updatedSpeed = rule.speed()?.let { speed }
            rebuilt.addRule(Tool.rule(rule.blocks(), updatedSpeed, rule.correctForDrops()))
        }

        item.setData(DataComponentTypes.TOOL, rebuilt.build())
    }

    private fun hasSpeedBearingRules(tool: Tool): Boolean = tool.rules().any { it.speed() != null }
}
