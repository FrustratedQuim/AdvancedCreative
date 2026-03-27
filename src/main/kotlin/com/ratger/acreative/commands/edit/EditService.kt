package com.ratger.acreative.commands.edit

import com.destroystokyo.paper.profile.ProfileProperty
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.Consumable
import io.papermc.paper.datacomponent.item.DeathProtection
import io.papermc.paper.datacomponent.item.FoodProperties
import io.papermc.paper.datacomponent.item.Tool
import io.papermc.paper.datacomponent.item.UseCooldown
import io.papermc.paper.datacomponent.item.UseRemainder
import io.papermc.paper.datacomponent.item.consumable.ConsumeEffect
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.block.Lockable
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockStateMeta
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.potion.PotionEffect
import java.util.UUID

class EditService(
    private val targetResolver: EditTargetResolver,
    private val validationService: EditValidationService,
    private val showService: EditShowService,
    private val parser: EditParsers,
    private val miniMessage: EditMiniMessage
) {
    private val mini = MiniMessage.miniMessage()

    fun execute(player: Player, action: EditAction): EditResult {
        val context = targetResolver.resolve(player) ?: return EditResult(false, emptyList())
        if (action is EditAction.Show) {
            return EditResult(true, showService.render(player, context))
        }

        if (action is EditAction.Reset && action.scope.startsWith("unsupported:")) {
            val key = action.scope.removePrefix("unsupported:")
            return EditResult(false, listOf(mini.deserialize("<yellow>Ветка <white>$key<yellow> пока unsupported на стабильном API сборки.")), warning = true)
        }

        validationService.validate(action, context, player)?.let { return it }

        val item = context.item.clone()
        val result = apply(player, action, item)
        if (!result.ok) return result

        targetResolver.save(player, item)
        return result
    }

    private fun apply(player: Player, action: EditAction, item: ItemStack): EditResult {
        if (action is EditAction.Reset) {
            return when (action.scope) {
                "all" -> {
                    resetAll(item)
                    EditResult(true, listOf(mini.deserialize("<green>Состояние предмета очищено (reset all).")))
                }

                else -> EditResult(false, listOf(mini.deserialize("<red>Использование: /edit reset <all>")))
            }
        }

        if (action is EditAction.SetItemId) {
            item.type = action.material
            return EditResult(true, listOf(mini.deserialize("<green>ID предмета изменён на <white>${action.material.key.asString()}</white>.")))
        }

        if (isDataComponentAction(action)) {
            return applyDataComponentAction(player, action, item)
        }

        if (action is EditAction.LockSetFromOffhand || action is EditAction.LockClear) {
            return applyLockAction(player, action, item)
        }

        val meta = item.itemMeta ?: return EditResult(false, listOf(mini.deserialize("<red>У предмета нет редактируемой meta")))
        val metaResult = applyMetaAction(action, meta)
        if (!metaResult.ok) return metaResult

        item.itemMeta = meta
        return EditResult(true, listOf(mini.deserialize("<green>Изменение применено.")))
    }

    private fun isDataComponentAction(action: EditAction): Boolean {
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
            action is EditAction.ClearUseCooldown
    }

    private fun applyDataComponentAction(player: Player, action: EditAction, item: ItemStack): EditResult {
        when (action) {
            is EditAction.ConsumableToggle -> {
                if (action.enabled) {
                    val current = item.getData(DataComponentTypes.CONSUMABLE)
                    if (current == null) {
                        item.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable().build())
                    }
                } else {
                    item.unsetData(DataComponentTypes.CONSUMABLE)
                    item.unsetData(DataComponentTypes.FOOD)
                }
            }

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
                val effects = consumable.consumeEffects().toMutableList()
                effects += EditEffectActionsSupport.toConsumeEffect(action.spec)
                item.setData(DataComponentTypes.CONSUMABLE, rebuildConsumable(consumable, effects))
            }
            is EditAction.ConsumableEffectRemove -> {
                val consumable = item.getData(DataComponentTypes.CONSUMABLE) ?: return EditResult(false, listOf(mini.deserialize("<red>У предмета нет компонента consumable")))
                val next = EditEffectActionsSupport.removeByIndex(consumable.consumeEffects(), action.index)
                    ?: return EditResult(false, listOf(mini.deserialize("<red>Некорректный индекс effect_remove")))
                item.setData(DataComponentTypes.CONSUMABLE, rebuildConsumable(consumable, next))
            }
            EditAction.ConsumableEffectClear -> {
                val consumable = item.getData(DataComponentTypes.CONSUMABLE) ?: return EditResult(false, listOf(mini.deserialize("<red>У предмета нет компонента consumable")))
                item.setData(DataComponentTypes.CONSUMABLE, rebuildConsumable(consumable, EditEffectActionsSupport.clear()))
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
                val effects = current.deathEffects().toMutableList()
                effects += EditEffectActionsSupport.toConsumeEffect(action.spec)
                item.setData(DataComponentTypes.DEATH_PROTECTION, deathProtectionOf(effects))
            }
            is EditAction.DeathProtectionEffectRemove -> {
                val current = item.getData(DataComponentTypes.DEATH_PROTECTION)
                    ?: return EditResult(false, listOf(mini.deserialize("<red>У предмета нет компонента death_protection")))
                val next = EditEffectActionsSupport.removeByIndex(current.deathEffects(), action.index)
                    ?: return EditResult(false, listOf(mini.deserialize("<red>Некорректный индекс effect_remove")))
                item.setData(DataComponentTypes.DEATH_PROTECTION, deathProtectionOf(next))
            }
            EditAction.DeathProtectionEffectClear -> {
                val current = item.getData(DataComponentTypes.DEATH_PROTECTION)
                    ?: return EditResult(false, listOf(mini.deserialize("<red>У предмета нет компонента death_protection")))
                item.setData(DataComponentTypes.DEATH_PROTECTION, deathProtectionOf(EditEffectActionsSupport.clear()))
            }

            is EditAction.FoodNutrition -> {
                ensureConsumableData(item)
                val builder = foodBuilder(item)
                builder.nutrition(action.value)
                item.setData(DataComponentTypes.FOOD, builder.build())
            }

            is EditAction.FoodSaturation -> {
                ensureConsumableData(item)
                val builder = foodBuilder(item)
                builder.saturation(action.value)
                item.setData(DataComponentTypes.FOOD, builder.build())
            }

            is EditAction.FoodCanAlwaysEat -> {
                ensureConsumableData(item)
                val builder = foodBuilder(item)
                builder.canAlwaysEat(action.value)
                item.setData(DataComponentTypes.FOOD, builder.build())
            }
            EditAction.RemainderSetFromOffhand -> {
                val offhandClone = player.inventory.itemInOffHand.clone()
                item.setData(DataComponentTypes.USE_REMAINDER, UseRemainder.useRemainder(offhandClone))
            }
            EditAction.RemainderClear -> {
                item.unsetData(DataComponentTypes.USE_REMAINDER)
            }
            is EditAction.EquippableSetSlot -> {
                val current = EditEquippableSupport.existingEquippable(item)
                val next = if (current == null) {
                    io.papermc.paper.datacomponent.item.Equippable.equippable(action.slot).build()
                } else {
                    EditEquippableSupport.rebuildEquippableWithSlot(current, action.slot)
                }
                item.setData(DataComponentTypes.EQUIPPABLE, next)
            }
            EditAction.EquippableClear -> {
                item.unsetData(DataComponentTypes.EQUIPPABLE)
            }
            is EditAction.EquippableSetDispensable -> {
                val builder = EditEquippableSupport.equippableBuilderOrPrototype(item)
                    ?: return EditResult(false, listOf(mini.deserialize("<red>Сначала установите slot через /edit equippable slot ...")))
                builder.dispensable(action.value)
                EditEquippableSupport.apply(item, builder)
            }
            is EditAction.EquippableSetSwappable -> {
                val builder = EditEquippableSupport.equippableBuilderOrPrototype(item)
                    ?: return EditResult(false, listOf(mini.deserialize("<red>Сначала установите slot через /edit equippable slot ...")))
                builder.swappable(action.value)
                EditEquippableSupport.apply(item, builder)
            }
            is EditAction.EquippableSetDamageOnHurt -> {
                val builder = EditEquippableSupport.equippableBuilderOrPrototype(item)
                    ?: return EditResult(false, listOf(mini.deserialize("<red>Сначала установите slot через /edit equippable slot ...")))
                builder.damageOnHurt(action.value)
                EditEquippableSupport.apply(item, builder)
            }
            is EditAction.EquippableSetEquipSound -> {
                val builder = EditEquippableSupport.equippableBuilderOrPrototype(item)
                    ?: return EditResult(false, listOf(mini.deserialize("<red>Сначала установите slot через /edit equippable slot ...")))
                if (action.keyOrDefault == null) {
                    val defaultEquippable = item.type.getDefaultData(DataComponentTypes.EQUIPPABLE)
                    if (defaultEquippable != null) {
                        builder.equipSound(defaultEquippable.equipSound())
                    } else {
                        return EditResult(
                            false,
                            listOf(mini.deserialize("<yellow>Для этого предмета нельзя восстановить default equip sound, потому что у material нет prototype equippable.")),
                            warning = true
                        )
                    }
                } else {
                    builder.equipSound(action.keyOrDefault)
                }
                EditEquippableSupport.apply(item, builder)
            }
            is EditAction.EquippableSetCameraOverlay -> {
                val builder = EditEquippableSupport.equippableBuilderOrPrototype(item)
                    ?: return EditResult(false, listOf(mini.deserialize("<red>Сначала установите slot через /edit equippable slot ...")))
                builder.cameraOverlay(action.keyOrNull)
                EditEquippableSupport.apply(item, builder)
            }
            is EditAction.EquippableSetAssetId -> {
                val builder = EditEquippableSupport.equippableBuilderOrPrototype(item)
                    ?: return EditResult(false, listOf(mini.deserialize("<red>Сначала установите slot через /edit equippable slot ...")))
                builder.assetId(action.keyOrNull)
                EditEquippableSupport.apply(item, builder)
            }
            is EditAction.ToolSetDefaultMiningSpeed -> {
                applyToolSpeed(item, action.value, action.scope)
            }
            is EditAction.ToolSetDamagePerBlock -> {
                val builder = toolBuilder(item)
                builder.damagePerBlock(action.value)
                item.setData(DataComponentTypes.TOOL, builder.build())
            }
            EditAction.ToolClear -> {
                item.unsetData(DataComponentTypes.TOOL)
            }
            is EditAction.SetUseCooldown -> {
                val builder = UseCooldown.useCooldown(action.seconds)
                builder.cooldownGroup(action.cooldownGroup)
                item.setData(DataComponentTypes.USE_COOLDOWN, builder.build())
            }
            EditAction.ClearUseCooldown -> {
                item.unsetData(DataComponentTypes.USE_COOLDOWN)
            }

            else -> return EditResult(false, listOf(mini.deserialize("<red>Ветка не поддерживается для data components")))
        }

        return EditResult(true, listOf(mini.deserialize("<green>Изменение применено.")))
    }

    private fun applyMetaAction(action: EditAction, meta: ItemMeta): EditResult {
        when (action) {
            is EditAction.NameSet -> meta.displayName(withoutItalic(miniMessage.parse(action.miniMessage)))
            EditAction.NameClear -> meta.displayName(null)
            is EditAction.LoreAdd -> {
                val lore = (meta.lore() ?: mutableListOf()).toMutableList()
                lore += withoutItalic(miniMessage.parse(action.miniMessage))
                meta.lore(lore)
            }

            is EditAction.LoreSet -> {
                val lore = (meta.lore() ?: mutableListOf()).toMutableList()
                if (action.index !in lore.indices) return EditResult(false, listOf(mini.deserialize("<red>Некорректный индекс lore")))
                lore[action.index] = withoutItalic(miniMessage.parse(action.miniMessage))
                meta.lore(lore)
            }

            is EditAction.LoreRemove -> {
                val lore = (meta.lore() ?: mutableListOf()).toMutableList()
                if (action.index !in lore.indices) return EditResult(false, listOf(mini.deserialize("<red>Некорректный индекс lore")))
                lore.removeAt(action.index)
                meta.lore(lore.takeIf { it.isNotEmpty() })
            }

            EditAction.LoreClear -> meta.lore(null)
            is EditAction.SetItemModel -> meta.itemModel = action.key
            is EditAction.SetUnbreakable -> meta.isUnbreakable = action.value
            is EditAction.SetGlider -> meta.setGlider(action.value)
            is EditAction.SetMaxDamage -> (meta as? Damageable)?.setMaxDamage(action.value)
                ?: return EditResult(false, listOf(mini.deserialize("<red>Предмет не поддерживает max_damage")))

            is EditAction.SetDamage -> {
                val dmg = meta as? Damageable ?: return EditResult(false, listOf(mini.deserialize("<red>Предмет не поддерживает damage")))
                dmg.damage = action.value
            }

            is EditAction.SetMaxStackSize -> {
                val value = action.value ?: return EditResult(false, listOf(mini.deserialize("<red>Укажите max_stack_size числом")))
                meta.setMaxStackSize(value)
            }

            is EditAction.SetRarity -> {
                val value = action.value ?: return EditResult(false, listOf(mini.deserialize("<red>Укажите rarity: common|uncommon|rare|epic")))
                meta.setRarity(value)
            }

            is EditAction.SetTooltipStyle -> meta.tooltipStyle = action.value
            is EditAction.SetHideTooltip -> meta.isHideTooltip = action.value
            is EditAction.SetHideAdditionalTooltip -> meta.isHideTooltip = action.value
            is EditAction.EnchantAdd -> meta.addEnchant(action.enchantment, action.level, true)
            is EditAction.EnchantRemove -> {
                if (!meta.hasEnchant(action.enchantment)) {
                    return EditResult(false, listOf(mini.deserialize("<yellow>На предмете нет зачарования <white>${action.enchantment.key.key}</white>.")), warning = true)
                }
                meta.removeEnchant(action.enchantment)
            }

            EditAction.EnchantClear -> meta.enchants.keys.toList().forEach(meta::removeEnchant)
            is EditAction.SetEnchantmentGlint -> meta.setEnchantmentGlintOverride(action.value)
            is EditAction.TooltipToggle -> toggleFlag(meta, action)
            is EditAction.SetCanPlaceOn -> meta.setPlaceableKeys(action.keys)
            is EditAction.SetCanBreak -> meta.setDestroyableKeys(action.keys)
            is EditAction.PotionColor -> {
                val potionMeta = meta as? PotionMeta ?: return EditResult(false, listOf(mini.deserialize("<red>Не potion item")))
                potionMeta.color = action.rgb?.let(Color::fromRGB)
            }

            is EditAction.PotionEffectAdd -> {
                val potionMeta = meta as? PotionMeta ?: return EditResult(false, listOf(mini.deserialize("<red>Не potion item")))
                potionMeta.addCustomEffect(PotionEffect(action.type, action.duration, action.amplifier, action.ambient, action.particles, action.icon), true)
            }

            is EditAction.PotionEffectRemove -> {
                val potionMeta = meta as? PotionMeta ?: return EditResult(false, listOf(mini.deserialize("<red>Не potion item")))
                potionMeta.removeCustomEffect(action.type)
            }

            EditAction.PotionEffectClear -> {
                val potionMeta = meta as? PotionMeta ?: return EditResult(false, listOf(mini.deserialize("<red>Не potion item")))
                potionMeta.customEffects.map { it.type }.forEach(potionMeta::removeCustomEffect)
            }

            is EditAction.HeadTextureSet -> {
                val skull = meta as? SkullMeta ?: return EditResult(false, listOf(mini.deserialize("<red>Не player head")))
                val profile = Bukkit.createProfile(UUID.randomUUID())
                profile.setProperty(ProfileProperty("textures", action.base64))
                skull.playerProfile = profile
            }

            EditAction.HeadTextureClear -> {
                val skull = meta as? SkullMeta ?: return EditResult(false, listOf(mini.deserialize("<red>Не player head")))
                skull.playerProfile = null
            }

            is EditAction.AttributeAdd -> {
                val slotGroup = action.slotGroup?.let(parser::slotGroup)
                val modifier = if (slotGroup == null) {
                    AttributeModifier(UUID.randomUUID(), "acreative_attr", action.amount, action.operation)
                } else {
                    AttributeModifier(UUID.randomUUID(), "acreative_attr", action.amount, action.operation, slotGroup)
                }
                meta.addAttributeModifier(action.attribute, modifier)
            }

            is EditAction.AttributeRemove -> {
                val mods = meta.attributeModifiers?.entries()?.toList().orEmpty()
                if (action.index !in mods.indices) return EditResult(false, listOf(mini.deserialize("<red>Нет такого индекса attribute modifier")))
                val pair = mods[action.index]
                meta.removeAttributeModifier(pair.key, pair.value)
            }

            EditAction.AttributeClear -> meta.attributeModifiers?.entries()?.toList()?.forEach { (attr, mod) -> meta.removeAttributeModifier(attr, mod) }
            EditAction.Show,
            is EditAction.Reset,
            is EditAction.SetItemId,
            is EditAction.ConsumableToggle,
            is EditAction.ConsumableAnimation,
            is EditAction.ConsumableHasParticles,
            is EditAction.ConsumableConsumeSeconds,
            is EditAction.ConsumableSound,
            is EditAction.FoodNutrition,
            is EditAction.FoodSaturation,
            is EditAction.FoodCanAlwaysEat,
            EditAction.RemainderSetFromOffhand,
            EditAction.RemainderClear,
            is EditAction.ConsumableEffectAdd,
            is EditAction.ConsumableEffectRemove,
            EditAction.ConsumableEffectClear,
            is EditAction.DeathProtectionToggle,
            is EditAction.DeathProtectionEffectAdd,
            is EditAction.DeathProtectionEffectRemove,
            EditAction.DeathProtectionEffectClear,
            is EditAction.EquippableSetSlot,
            EditAction.EquippableClear,
            is EditAction.EquippableSetDispensable,
            is EditAction.EquippableSetSwappable,
            is EditAction.EquippableSetDamageOnHurt,
            is EditAction.EquippableSetEquipSound,
            is EditAction.EquippableSetCameraOverlay,
            is EditAction.EquippableSetAssetId,
            is EditAction.ToolSetDefaultMiningSpeed,
            is EditAction.ToolSetDamagePerBlock,
            EditAction.ToolClear,
            is EditAction.SetUseCooldown,
            EditAction.ClearUseCooldown,
            EditAction.LockSetFromOffhand,
            EditAction.LockClear -> return EditResult(false, listOf(mini.deserialize("<red>Ветка не поддерживается для item meta")))
        }

        return EditResult(true, listOf(mini.deserialize("<green>Изменение применено.")))
    }

    private fun applyLockAction(player: Player, action: EditAction, item: ItemStack): EditResult {
        val meta = item.itemMeta as? BlockStateMeta
            ?: return EditResult(false, listOf(mini.deserialize("<red>Item meta не поддерживает block state (BlockStateMeta)")))
        val state = meta.blockState
        val lockable = state as? Lockable
            ?: return EditResult(false, listOf(mini.deserialize("<red>Block state предмета не поддерживает lock API")))

        when (action) {
            EditAction.LockSetFromOffhand -> {
                val offhandClone = player.inventory.itemInOffHand.clone()
                lockable.setLockItem(offhandClone)
            }
            EditAction.LockClear -> {
                lockable.setLockItem(null)
            }
            else -> return EditResult(false, listOf(mini.deserialize("<red>Некорректное lock действие")))
        }

        meta.setBlockState(state)
        item.itemMeta = meta
        return EditResult(true, listOf(mini.deserialize("<green>Изменение применено.")))
    }

    private fun resetAll(item: ItemStack) {
        val clean = ItemStack(item.type, item.amount)
        if (item.type == Material.AIR) return
        item.itemMeta = clean.itemMeta
    }

    private fun toggleFlag(meta: ItemMeta, action: EditAction.TooltipToggle) {
        if (action.key.equals("hide_tooltip", ignoreCase = true)) {
            meta.isHideTooltip = action.hide
            return
        }
        if (action.key.equals("hide_additional_tooltip", ignoreCase = true)) {
            meta.isHideTooltip = action.hide
            return
        }
        val flag = when (action.key.lowercase()) {
            "enchantments" -> ItemFlag.HIDE_ENCHANTS
            "attribute_modifiers", "attributes" -> ItemFlag.HIDE_ATTRIBUTES
            "unbreakable" -> ItemFlag.HIDE_UNBREAKABLE
            "dyed_color" -> ItemFlag.HIDE_DYE
            "can_break" -> ItemFlag.HIDE_DESTROYS
            "can_place_on" -> ItemFlag.HIDE_PLACED_ON
            "trim" -> ItemFlag.HIDE_ARMOR_TRIM
            else -> null
        } ?: return

        if (action.hide) meta.addItemFlags(flag) else meta.removeItemFlags(flag)
    }

    private fun withoutItalic(component: net.kyori.adventure.text.Component): net.kyori.adventure.text.Component {
        return component.decoration(TextDecoration.ITALIC, false)
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

    private fun rebuildConsumable(current: Consumable, effects: List<ConsumeEffect>): Consumable {
        val builder = Consumable.consumable()
            .consumeSeconds(current.consumeSeconds())
            .animation(current.animation())
            .hasConsumeParticles(current.hasConsumeParticles())
            .sound(current.sound())
            .addEffects(effects)
        return builder.build()
    }

    private fun deathProtectionOf(effects: List<ConsumeEffect>): DeathProtection {
        return DeathProtection.deathProtection().addEffects(effects).build()
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
        if (!hasSpeedRules) {
            val rebuilt = Tool.tool()
                .defaultMiningSpeed(speed)
                .damagePerBlock(current.damagePerBlock())
                .addRules(current.rules())
                .build()
            item.setData(DataComponentTypes.TOOL, rebuilt)
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

        val rebuilt = Tool.tool()
            .defaultMiningSpeed(
                when (scope) {
                    ToolSpeedScope.ALL_BLOCKS -> speed
                    ToolSpeedScope.EFFECTIVE_ONLY -> current.defaultMiningSpeed()
                    ToolSpeedScope.INEFFECTIVE_ONLY -> speed
                }
            )
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
