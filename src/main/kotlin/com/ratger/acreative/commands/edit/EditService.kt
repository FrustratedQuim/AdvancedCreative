package com.ratger.acreative.commands.edit

import com.destroystokyo.paper.profile.ProfileProperty
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.Consumable
import io.papermc.paper.datacomponent.item.DeathProtection
import io.papermc.paper.datacomponent.item.FoodProperties
import io.papermc.paper.datacomponent.item.Tool
import io.papermc.paper.datacomponent.item.UseCooldown
import io.papermc.paper.datacomponent.item.UseRemainder
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
import org.bukkit.inventory.meta.ArmorMeta
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.inventory.meta.trim.ArmorTrim
import org.bukkit.potion.PotionEffect
import java.util.UUID
import org.bukkit.plugin.java.JavaPlugin
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture

class EditService(
    private val plugin: JavaPlugin,
    private val targetResolver: EditTargetResolver,
    private val validationService: EditValidationService,
    private val showService: EditShowService,
    private val parser: EditParsers,
    private val miniMessage: EditMiniMessage
) {
    private val mini = MiniMessage.miniMessage()
    private val httpClient: HttpClient = HttpClient.newBuilder().build()

    fun execute(player: Player, action: EditAction): EditResult {
        val context = targetResolver.resolve(player) ?: return EditResult(false, emptyList())
        if (action is EditAction.Show) {
            return EditResult(true, showService.render(context))
        }

        if (action is EditAction.Reset && action.scope.startsWith("unsupported:")) {
            val key = action.scope.removePrefix("unsupported:")
            return EditResult(false, listOf(mini.deserialize("<yellow>Ветка <white>$key<yellow> пока unsupported на стабильном API сборки.")), warning = true)
        }

        validationService.validate(action, context, player)?.let { return it }

        if (action is EditAction.HeadSetFromName) {
            return handleHeadSetFromNameAsync(player, action.name)
        }

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
            action is EditAction.ClearUseCooldown ||
            action is EditAction.ContainerSetSlotFromOffhand ||
            action is EditAction.PotClear ||
            action is EditAction.PotSet ||
            action is EditAction.PotSetSide
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
                item.setData(DataComponentTypes.CONSUMABLE, EditConsumeEffectsAdapter.addConsumableEffect(consumable, action.spec))
            }
            is EditAction.ConsumableEffectRemove -> {
                val consumable = item.getData(DataComponentTypes.CONSUMABLE) ?: return EditResult(false, listOf(mini.deserialize("<red>У предмета нет компонента consumable")))
                val next = EditConsumeEffectsAdapter.removeConsumableEffect(consumable, action.index)
                    ?: return EditResult(false, listOf(mini.deserialize("<red>Некорректный индекс effect_remove")))
                item.setData(DataComponentTypes.CONSUMABLE, next)
            }
            EditAction.ConsumableEffectClear -> {
                val consumable = item.getData(DataComponentTypes.CONSUMABLE) ?: return EditResult(false, listOf(mini.deserialize("<red>У предмета нет компонента consumable")))
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
                if (!EditEquippableSupport.setSlot(item, action.slot)) {
                    return EditResult(false, listOf(mini.deserialize("<red>Не удалось создать equippable snapshot для этого предмета")))
                }
            }
            EditAction.EquippableClear -> {
                EditEquippableSupport.clear(item)
            }
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
                } else {
                    if (!EditEquippableSupport.setEquipSound(item, action.keyOrDefault)) {
                        return EditResult(false, listOf(mini.deserialize("<red>Сначала установите slot через /edit equippable slot ...")))
                    }
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

            is EditAction.HeadSetFromTexture -> {
                val skull = meta as? SkullMeta ?: return EditResult(false, listOf(mini.deserialize("<red>Не player head")))
                val profile = Bukkit.createProfile(UUID.randomUUID())
                profile.setProperty(ProfileProperty("textures", action.base64))
                skull.setPlayerProfile(profile)
            }

            is EditAction.HeadSetFromOnline -> {
                val skull = meta as? SkullMeta ?: return EditResult(false, listOf(mini.deserialize("<red>Не player head")))
                val source = Bukkit.getPlayerExact(action.name)
                    ?: return EditResult(false, listOf(mini.deserialize("<red>Онлайн-игрок <white>${action.name}</white> не найден.")))
                skull.setPlayerProfile(copyProfile(source.playerProfile))
            }

            EditAction.HeadClear -> {
                val skull = meta as? SkullMeta ?: return EditResult(false, listOf(mini.deserialize("<red>Не player head")))
                skull.setPlayerProfile(null)
            }

            is EditAction.AttributeAdd -> {
                val slotGroupSpec = action.slotGroup?.let(parser::slotGroup)
                val modifier = if (slotGroupSpec == null) {
                    AttributeModifier(UUID.randomUUID(), "acreative_attr", action.amount, action.operation)
                } else {
                    val slotGroup = EditSlotGroupAdapter.toPaperGroup(slotGroupSpec)
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
            is EditAction.TrimSet -> {
                val armorMeta = meta as? ArmorMeta ?: return EditResult(false, listOf(mini.deserialize("<red>Item meta не поддерживает ArmorMeta")))
                armorMeta.setTrim(ArmorTrim(action.material, action.pattern))
            }
            EditAction.TrimClear -> {
                val armorMeta = meta as? ArmorMeta ?: return EditResult(false, listOf(mini.deserialize("<red>Item meta не поддерживает ArmorMeta")))
                armorMeta.setTrim(null)
            }
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
            is EditAction.ContainerSetSlotFromOffhand,
            EditAction.PotClear,
            is EditAction.PotSet,
            is EditAction.PotSetSide,
            EditAction.LockSetFromOffhand,
            EditAction.LockClear,
            is EditAction.HeadSetFromName -> return EditResult(false, listOf(mini.deserialize("<red>Ветка не поддерживается для item meta")))
        }

        return EditResult(true, listOf(mini.deserialize("<green>Изменение применено.")))
    }

    private fun handleHeadSetFromNameAsync(player: Player, name: String): EditResult {
        CompletableFuture.supplyAsync { lookupLicensedProfile(name) }.whenComplete { payload, error ->
            Bukkit.getScheduler().runTask(plugin, Runnable {
                val target = Bukkit.getPlayer(player.uniqueId) ?: return@Runnable
                if (error != null) {
                    target.sendMessage(mini.deserialize("<red>Ошибка licensed-profile lookup для <white>$name</white>: <gray>${error.message ?: "unknown error"}"))
                    return@Runnable
                }
                if (payload == null) {
                    target.sendMessage(mini.deserialize("<red>Не удалось получить профиль по имени <white>$name</white>."))
                    return@Runnable
                }
                if (payload.textureValue.isBlank()) {
                    target.sendMessage(mini.deserialize("<red>Официальный профиль <white>${payload.canonicalName}</white> не содержит textures property."))
                    return@Runnable
                }

                val context = targetResolver.resolve(target) ?: return@Runnable
                if (!context.snapshot.isHead) {
                    target.sendMessage(mini.deserialize("<red>Держите minecraft:player_head в основной руке перед применением результата."))
                    return@Runnable
                }
                val item = context.item.clone()
                val meta = item.itemMeta as? SkullMeta
                if (meta == null) {
                    target.sendMessage(mini.deserialize("<red>Текущий предмет не является player head."))
                    return@Runnable
                }
                val officialProfile = Bukkit.createProfile(payload.uuid, payload.canonicalName)
                officialProfile.setProperty(ProfileProperty("textures", payload.textureValue, payload.textureSignature))
                meta.setPlayerProfile(officialProfile)
                item.itemMeta = meta
                targetResolver.save(target, item)
                target.sendMessage(mini.deserialize("<green>Текстура головы установлена из официального licensed profile <white>${payload.canonicalName}</white>."))
            })
        }
        return EditResult(true, listOf(mini.deserialize("<yellow>Запрошен профиль <white>$name</white>. Применю текстуру после асинхронного обновления.")))
    }

    private fun lookupLicensedProfile(name: String): LicensedProfilePayload {
        val encoded = URLEncoder.encode(name, StandardCharsets.UTF_8)
        val nameLookup = lookupNameViaMinecraftServices(encoded) ?: lookupNameViaMojang(encoded)
            ?: throw IllegalStateException("Профиль по имени не найден.")
        val session = lookupSessionProfile(nameLookup.uuid)
        val textures = session.texturesValue ?: throw IllegalStateException("Session profile не содержит textures.")
        return LicensedProfilePayload(
            uuid = session.uuid,
            canonicalName = session.name ?: nameLookup.canonicalName,
            textureValue = textures,
            textureSignature = session.texturesSignature
        )
    }

    private fun lookupNameViaMinecraftServices(encodedName: String): NameLookupPayload? {
        val response = sendJsonGet("https://api.minecraftservices.com/minecraft/profile/lookup/name/$encodedName")
        if (response.statusCode() == 404) return null
        if (response.statusCode() !in 200..299) {
            throw IllegalStateException("minecraftservices lookup HTTP ${response.statusCode()}")
        }
        return parseNameLookupPayload(response.body())
    }

    private fun lookupNameViaMojang(encodedName: String): NameLookupPayload? {
        val response = sendJsonGet("https://api.mojang.com/users/profiles/minecraft/$encodedName")
        if (response.statusCode() == 204 || response.statusCode() == 404) return null
        if (response.statusCode() !in 200..299) {
            throw IllegalStateException("mojang lookup HTTP ${response.statusCode()}")
        }
        return parseNameLookupPayload(response.body())
    }

    private fun lookupSessionProfile(uuid: UUID): SessionProfilePayload {
        val undashed = uuid.toString().replace("-", "")
        val response = sendJsonGet("https://sessionserver.mojang.com/session/minecraft/profile/$undashed")
        if (response.statusCode() == 404) throw IllegalStateException("Session profile не найден.")
        if (response.statusCode() !in 200..299) throw IllegalStateException("sessionserver HTTP ${response.statusCode()}")
        return parseSessionProfilePayload(response.body())
    }

    private fun sendJsonGet(url: String): HttpResponse<String> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept", "application/json")
            .GET()
            .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
    }

    private fun parseNameLookupPayload(json: String): NameLookupPayload? {
        val idRaw = jsonString(json, "id") ?: return null
        val canonicalName = jsonString(json, "name") ?: return null
        val uuid = parseUuid(idRaw) ?: return null
        return NameLookupPayload(uuid, canonicalName)
    }

    private fun parseSessionProfilePayload(json: String): SessionProfilePayload {
        val idRaw = jsonString(json, "id") ?: throw IllegalStateException("Session profile id отсутствует.")
        val uuid = parseUuid(idRaw) ?: throw IllegalStateException("Session profile id невалидный.")
        val canonicalName = jsonString(json, "name")
        val texturesObject = Regex("\\{[^{}]*\"name\"\\s*:\\s*\"textures\"[^{}]*}", setOf(RegexOption.DOT_MATCHES_ALL))
            .find(json)
            ?.value
        val textureValue = texturesObject?.let { jsonString(it, "value") }
        val textureSignature = texturesObject?.let { jsonString(it, "signature") }
        return SessionProfilePayload(uuid, canonicalName, textureValue, textureSignature)
    }


    private fun parseUuid(raw: String): UUID? {
        val normalized = raw.trim()
        return runCatching {
            if (normalized.contains('-')) UUID.fromString(normalized)
            else UUID.fromString(normalized.replaceFirst(
                Regex("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)$"),
                "$1-$2-$3-$4-$5"
            ))
        }.getOrNull()
    }

    private fun jsonString(source: String, field: String): String? {
        val escapedField = Regex.escape(field)
        val regex = Regex("\"$escapedField\"\\s*:\\s*\"([^\"]*)\"")
        return regex.find(source)?.groupValues?.getOrNull(1)
    }

    private data class NameLookupPayload(
        val uuid: UUID,
        val canonicalName: String
    )

    private data class SessionProfilePayload(
        val uuid: UUID,
        val name: String?,
        val texturesValue: String?,
        val texturesSignature: String?
    )

    private data class LicensedProfilePayload(
        val uuid: UUID,
        val canonicalName: String,
        val textureValue: String,
        val textureSignature: String?
    )

    private fun copyProfile(source: com.destroystokyo.paper.profile.PlayerProfile): com.destroystokyo.paper.profile.PlayerProfile {
        val clone = runCatching { Bukkit.createProfile(source.uniqueId, source.name) }
            .getOrElse { Bukkit.createProfile(source.uniqueId ?: UUID.randomUUID()) }
        source.properties.forEach { clone.setProperty(ProfileProperty(it.name, it.value, it.signature)) }
        return clone
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
