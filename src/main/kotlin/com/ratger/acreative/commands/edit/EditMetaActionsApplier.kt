package com.ratger.acreative.commands.edit

import com.destroystokyo.paper.profile.ProfileProperty
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.NamespacedKey
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ArmorMeta
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.inventory.meta.trim.ArmorTrim
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import java.util.UUID

class EditMetaActionsApplier(
    private val plugin: JavaPlugin,
    private val parser: EditParsers,
    private val miniMessage: EditMiniMessage
) {
    private val mini = MiniMessage.miniMessage()

    fun apply(action: EditAction, player: Player, item: ItemStack): EditResult? {
        val meta = item.itemMeta ?: return EditResult(false, listOf(mini.deserialize("<red>У предмета нет редактируемой meta")))
        val result = applyToMeta(action, player, meta)
        if (!result.ok) return result
        item.itemMeta = meta
        return result
    }

    private fun applyToMeta(action: EditAction, player: Player, meta: ItemMeta): EditResult {
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
            is EditAction.SetGlider -> meta.isGlider = action.value
            is EditAction.SetMaxDamage -> (meta as? Damageable)?.setMaxDamage(action.value)
                ?: return EditResult(false, listOf(mini.deserialize("<red>Предмет не поддерживает max_damage")))
            is EditAction.SetDamage -> {
                val damageable = meta as? Damageable ?: return EditResult(false, listOf(mini.deserialize("<red>Предмет не поддерживает damage")))
                damageable.damage = action.value
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
            is EditAction.SetCanPlaceOn -> EditLegacyMetaKeySupport.setCanPlaceOn(meta, action.keys)
            is EditAction.SetCanBreak -> EditLegacyMetaKeySupport.setCanBreak(meta, action.keys)
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
                skull.playerProfile = profile
            }
            is EditAction.HeadSetFromOnline -> {
                val skull = meta as? SkullMeta ?: return EditResult(false, listOf(mini.deserialize("<red>Не player head")))
                val source = Bukkit.getPlayerExact(action.name)
                    ?: return EditResult(false, listOf(mini.deserialize("<red>Онлайн-игрок <white>${action.name}</white> не найден.")))
                skull.playerProfile = copyProfile(source.playerProfile)
            }
            EditAction.HeadClear -> {
                val skull = meta as? SkullMeta ?: return EditResult(false, listOf(mini.deserialize("<red>Не player head")))
                skull.playerProfile = null
            }
            is EditAction.AttributeAdd -> {
                val slotGroupSpec = action.slotGroup?.let(parser::slotGroup)
                val key = NamespacedKey(plugin, "acreative_attr_${UUID.randomUUID()}")
                val modifier = if (slotGroupSpec == null) {
                    AttributeModifier(key, action.amount, action.operation)
                } else {
                    AttributeModifier(key, action.amount, action.operation, EditSlotGroupAdapter.toPaperGroup(slotGroupSpec))
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
                armorMeta.trim = ArmorTrim(action.material, action.pattern)
            }
            EditAction.TrimClear -> {
                val armorMeta = meta as? ArmorMeta ?: return EditResult(false, listOf(mini.deserialize("<red>Item meta не поддерживает ArmorMeta")))
                armorMeta.trim = null
            }
            else -> return EditResult(false, listOf(mini.deserialize("<red>Ветка не поддерживается для item meta")))
        }
        return EditResult(true, listOf(mini.deserialize("<green>Изменение применено.")))
    }

    private fun copyProfile(source: com.destroystokyo.paper.profile.PlayerProfile): com.destroystokyo.paper.profile.PlayerProfile {
        val clone = runCatching { Bukkit.createProfile(source.uniqueId, source.name) }
            .getOrElse { Bukkit.createProfile(source.uniqueId ?: UUID.randomUUID()) }
        source.properties.forEach { clone.setProperty(ProfileProperty(it.name, it.value, it.signature)) }
        return clone
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
