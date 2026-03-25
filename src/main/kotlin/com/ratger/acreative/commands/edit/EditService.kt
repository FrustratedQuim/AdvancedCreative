package com.ratger.acreative.commands.edit

import com.destroystokyo.paper.profile.ProfileProperty
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
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
        val meta = item.itemMeta ?: return EditResult(false, listOf(mini.deserialize("<red>У предмета нет редактируемой meta")))

        val result = apply(action, item, meta, context)
        if (!result.ok) return result

        targetResolver.markPluginState(item)
        targetResolver.save(player, item)
        return result
    }

    private fun apply(action: EditAction, item: ItemStack, meta: ItemMeta, context: EditContext): EditResult {
        when (action) {
            is EditAction.Reset -> {
                return when (action.scope) {
                    "all" -> {
                        resetAll(item)
                        EditResult(true, listOf(mini.deserialize("<green>Состояние предмета очищено (reset all).")))
                    }

                    "plugin" -> {
                        targetResolver.clearPluginState(item)
                        EditResult(true, listOf(mini.deserialize("<green>Служебное состояние плагина очищено.")))
                    }

                    else -> EditResult(false, listOf(mini.deserialize("<red>Использование: /edit reset <all|plugin>")))
                }
            }

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
            EditAction.Show -> Unit
        }

        item.itemMeta = meta
        return EditResult(true, listOf(mini.deserialize("<green>Изменение применено.")))
    }

    private fun resetAll(item: ItemStack) {
        val clean = ItemStack(item.type, item.amount)
        if (item.type == Material.AIR) return
        item.itemMeta = clean.itemMeta
        targetResolver.clearPluginState(item)
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
}
