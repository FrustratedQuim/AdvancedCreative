@file:Suppress("UnstableApiUsage") // Experimental Jukebox

package com.ratger.acreative.itemedit.meta

import com.destroystokyo.paper.profile.ProfileProperty
import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.Multimap
import com.ratger.acreative.commands.edit.EditParsers
import com.ratger.acreative.itemedit.api.ItemAction
import com.ratger.acreative.itemedit.api.ItemResult
import com.ratger.acreative.itemedit.attributes.AttributeModifierFactory
import com.ratger.acreative.itemedit.attributes.ItemAttributeMenuSupport
import com.ratger.acreative.itemedit.head.PlayerProfileCopyHelper
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.*
import org.bukkit.inventory.meta.components.JukeboxPlayableComponent
import org.bukkit.inventory.meta.trim.ArmorTrim
import org.bukkit.potion.PotionEffect
import java.util.*

class MetaActionsApplier(
    private val parser: EditParsers,
    private val miniMessage: MiniMessageParser
) {
    companion object {
        fun isTooltipHidden(meta: ItemMeta, key: String): Boolean {
            return when (key.lowercase()) {
                "hide_tooltip" -> meta.isHideTooltip
                "jukebox_playable", "music" -> meta.hasJukeboxPlayable() && !meta.jukeboxPlayable.isShowInTooltip
                else -> keyToFlag(key)?.let { meta.itemFlags.contains(it) } ?: false
            }
        }

        fun canEnableTooltipHide(meta: ItemMeta, key: String, itemType: Material? = null): Boolean {
            return when (key.lowercase()) {
                "hide_tooltip" -> true
                "enchantments" -> meta.hasEnchants()
                "attribute_modifiers", "attributes" -> true
                "unbreakable" -> meta.isUnbreakable
                "dyed_color" -> (meta as? LeatherArmorMeta)?.isDyed == true
                "can_break" -> LegacyMetaKeySupport.hasDestroyable(meta)
                "can_place_on" -> LegacyMetaKeySupport.hasPlaceable(meta)
                "trim" -> (meta as? ArmorMeta)?.trim != null
                "jukebox_playable", "music" -> meta.hasJukeboxPlayable() || resolveDefaultDiscSongKey(itemType) != null
                else -> keyToFlag(key) != null
            }
        }

        fun setTooltipHidden(meta: ItemMeta, key: String, hide: Boolean, itemType: Material? = null): Boolean {
            if (key.equals("hide_tooltip", ignoreCase = true)) {
                if (meta.isHideTooltip == hide) return false
                meta.isHideTooltip = hide
                return true
            }
            if (key.equals("jukebox_playable", ignoreCase = true) || key.equals("music", ignoreCase = true)) {
                return setJukeboxTooltipHidden(meta, hide, itemType)
            }
            if (key.equals("attribute_modifiers", ignoreCase = true) || key.equals("attributes", ignoreCase = true)) {
                return if (hide) enableAttributeTooltipHide(meta, itemType) else disableAttributeTooltipHide(meta)
            }
            if (hide && !canEnableTooltipHide(meta, key, itemType)) {
                return false
            }
            val flag = keyToFlag(key) ?: return false
            val wasHidden = meta.itemFlags.contains(flag)
            if (wasHidden == hide) return false
            if (hide) meta.addItemFlags(flag) else meta.removeItemFlags(flag)
            return true
        }

        private fun enableAttributeTooltipHide(meta: ItemMeta, itemType: Material?): Boolean {
            if (meta.itemFlags.contains(ItemFlag.HIDE_ATTRIBUTES)) {
                return false
            }
            if (!meta.hasAttributeModifiers()) {
                materializeDefaultAttributeModifiers(meta, itemType)
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            return true
        }

        private fun disableAttributeTooltipHide(meta: ItemMeta): Boolean {
            if (!meta.itemFlags.contains(ItemFlag.HIDE_ATTRIBUTES)) return false
            meta.removeItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            return true
        }

        fun materializeDefaultAttributeModifiers(meta: ItemMeta, itemType: Material?): Boolean {
            if (meta.hasAttributeModifiers()) return false
            val defaults = resolveDefaultAttributeModifiers(itemType)
            val explicitList = LinkedHashMultimap.create<Attribute, AttributeModifier>()
            explicitList.putAll(defaults)
            meta.attributeModifiers = explicitList
            return true
        }

        private fun resolveDefaultAttributeModifiers(itemType: Material?): Multimap<Attribute, AttributeModifier> {
            return ItemAttributeMenuSupport.defaultEffectiveAttributes(itemType)
        }

        fun clearExplicitAttributeModifiers(meta: ItemMeta): Boolean {
            if (!meta.hasAttributeModifiers()) return false
            meta.attributeModifiers = LinkedHashMultimap.create<Attribute, AttributeModifier>()
            return true
        }

        private fun setJukeboxTooltipHidden(meta: ItemMeta, hide: Boolean, itemType: Material?): Boolean {
            if (hide) {
                val component = if (meta.hasJukeboxPlayable()) {
                    meta.jukeboxPlayable
                } else {
                    val songKey = resolveDefaultDiscSongKey(itemType) ?: return false
                    val generated = createJukeboxComponentFromDefaults(itemType) ?: return false
                    generated.songKey = songKey
                    generated
                }
                if (!component.isShowInTooltip) return false
                component.isShowInTooltip = false
                meta.setJukeboxPlayable(component)
                return true
            }

            if (!meta.hasJukeboxPlayable()) return false
            val component = meta.jukeboxPlayable
            if (component.isShowInTooltip) return false
            component.isShowInTooltip = true
            meta.setJukeboxPlayable(component)
            return true
        }

        fun clearVanillaDiscExplicitJukeboxComponent(meta: ItemMeta, itemType: Material?): Boolean {
            if (!meta.hasJukeboxPlayable()) return false
            val defaultSong = resolveDefaultDiscSongKey(itemType) ?: return false
            val component = meta.jukeboxPlayable
            if (component.songKey != defaultSong) return false
            meta.setJukeboxPlayable(null)
            return true
        }

        private fun createJukeboxComponentFromDefaults(itemType: Material?): JukeboxPlayableComponent? {
            val candidates = buildList {
                if (itemType != null) {
                    add(runCatching { Bukkit.getItemFactory().getItemMeta(itemType) }.getOrNull())
                    add(runCatching { ItemStack.of(itemType).itemMeta }.getOrNull())
                }
            }
            return candidates.firstNotNullOfOrNull { candidate ->
                candidate?.jukeboxPlayable
            }
        }

        private fun resolveDefaultDiscSongKey(itemType: Material?): NamespacedKey? {
            val type = itemType ?: return null
            val typeKey = type.key.key
            if (!typeKey.startsWith("music_disc_")) return null
            return NamespacedKey.minecraft(typeKey.removePrefix("music_disc_"))
        }

        private fun keyToFlag(key: String): ItemFlag? = when (key.lowercase()) {
            "enchantments" -> ItemFlag.HIDE_ENCHANTS
            "attribute_modifiers", "attributes" -> ItemFlag.HIDE_ATTRIBUTES
            "unbreakable" -> ItemFlag.HIDE_UNBREAKABLE
            "dyed_color" -> ItemFlag.HIDE_DYE
            "can_break" -> ItemFlag.HIDE_DESTROYS
            "can_place_on" -> ItemFlag.HIDE_PLACED_ON
            "trim" -> ItemFlag.HIDE_ARMOR_TRIM
            "hide_additional_tooltip", "additional", "misc" -> ItemFlag.HIDE_ADDITIONAL_TOOLTIP
            else -> null
        }
    }

    private val mini = MiniMessage.miniMessage()

    fun apply(action: ItemAction, item: ItemStack): ItemResult? {
        when (action) {
            is ItemAction.AttributeAdd,
            is ItemAction.AttributeRemove,
            ItemAction.AttributeClear -> return applyAttributeAction(action, item)
            else -> Unit
        }
        val meta = item.itemMeta ?: return ItemResult(false, listOf(mini.deserialize("<red>У предмета нет редактируемой meta")))
        val result = applyToMeta(action, meta, item)
        if (!result.ok) return result
        item.itemMeta = meta
        return result
    }

    private fun applyAttributeAction(action: ItemAction, item: ItemStack): ItemResult {
        when (action) {
            is ItemAction.AttributeAdd -> {
                val slotGroupSpec = action.slotGroup?.let(parser::slotGroup)
                val key = NamespacedKey.minecraft(UUID.randomUUID().toString())
                val modifier = AttributeModifierFactory.create(key, action.amount, action.operation, slotGroupSpec)
                val explicit = ItemAttributeMenuSupport.currentEffectiveAttributes(item)
                explicit.put(action.attribute, modifier)
                ItemAttributeMenuSupport.writeExplicitAttributes(item, explicit)
            }
            is ItemAction.AttributeRemove -> {
                val explicit = ItemAttributeMenuSupport.currentEffectiveAttributes(item)
                val mods = explicit.entries().toList()
                if (action.index !in mods.indices) return ItemResult(false, listOf(mini.deserialize("<red>Нет такого индекса attribute modifier")))
                val pair = mods[action.index]
                explicit.remove(pair.key, pair.value)
                ItemAttributeMenuSupport.writeExplicitAttributes(item, explicit)
            }
            ItemAction.AttributeClear -> {
                ItemAttributeMenuSupport.writeExplicitAttributes(
                    item,
                    LinkedHashMultimap.create<Attribute, AttributeModifier>()
                )
            }
            else -> return ItemResult(false, listOf(mini.deserialize("<red>Ветка не поддерживается для item meta")))
        }
        return ItemResult(true, listOf(mini.deserialize("<green>Изменение применено.")))
    }

    private fun applyToMeta(action: ItemAction, meta: ItemMeta, item: ItemStack): ItemResult {
        val itemType = item.type
        when (action) {
            is ItemAction.NameSet -> meta.customName(withoutItalic(miniMessage.parse(action.miniMessage)))
            ItemAction.NameClear -> meta.customName(null)
            is ItemAction.LoreAdd -> {
                val lore = (meta.lore() ?: mutableListOf()).toMutableList()
                lore += withoutItalic(miniMessage.parse(action.miniMessage))
                meta.lore(lore)
            }
            is ItemAction.LoreSet -> {
                val lore = (meta.lore() ?: mutableListOf()).toMutableList()
                if (action.index !in lore.indices) return ItemResult(false, listOf(mini.deserialize("<red>Некорректный индекс lore")))
                lore[action.index] = withoutItalic(miniMessage.parse(action.miniMessage))
                meta.lore(lore)
            }
            is ItemAction.LoreRemove -> {
                val lore = (meta.lore() ?: mutableListOf()).toMutableList()
                if (action.index !in lore.indices) return ItemResult(false, listOf(mini.deserialize("<red>Некорректный индекс lore")))
                lore.removeAt(action.index)
                meta.lore(lore.takeIf { it.isNotEmpty() })
            }
            ItemAction.LoreClear -> meta.lore(null)
            is ItemAction.SetItemModel -> meta.itemModel = action.key
            is ItemAction.SetUnbreakable -> meta.isUnbreakable = action.value
            is ItemAction.SetGlider -> meta.isGlider = action.value
            is ItemAction.SetMaxDamage -> (meta as? Damageable)?.setMaxDamage(action.value)
                ?: return ItemResult(false, listOf(mini.deserialize("<red>Предмет не поддерживает max_damage")))
            is ItemAction.SetDamage -> {
                val damageable = meta as? Damageable ?: return ItemResult(false, listOf(mini.deserialize("<red>Предмет не поддерживает damage")))
                damageable.damage = action.value
            }
            is ItemAction.SetMaxStackSize -> {
                val value = action.value ?: return ItemResult(false, listOf(mini.deserialize("<red>Укажите max_stack_size числом")))
                meta.setMaxStackSize(value)
            }
            is ItemAction.SetRarity -> {
                val value = action.value ?: return ItemResult(false, listOf(mini.deserialize("<red>Укажите rarity: common|uncommon|rare|epic")))
                meta.setRarity(value)
            }
            is ItemAction.SetTooltipStyle -> meta.tooltipStyle = action.value
            is ItemAction.SetHideTooltip -> meta.isHideTooltip = action.value
            is ItemAction.SetHideAdditionalTooltip -> setTooltipHidden(meta, "hide_additional_tooltip", action.value, itemType)
            is ItemAction.EnchantAdd -> meta.addEnchant(action.enchantment, action.level, true)
            is ItemAction.EnchantRemove -> {
                if (!meta.hasEnchant(action.enchantment)) {
                    return ItemResult(false, listOf(mini.deserialize("<yellow>На предмете нет зачарования <white>${action.enchantment.key.key}</white>.")), warning = true)
                }
                meta.removeEnchant(action.enchantment)
            }
            ItemAction.EnchantClear -> meta.enchants.keys.toList().forEach(meta::removeEnchant)
            is ItemAction.SetEnchantmentGlint -> meta.setEnchantmentGlintOverride(action.value)
            is ItemAction.TooltipToggle -> toggleFlag(meta, action, itemType)
            is ItemAction.SetCanPlaceOn -> LegacyMetaKeySupport.setCanPlaceOn(meta, action.keys)
            is ItemAction.SetCanBreak -> LegacyMetaKeySupport.setCanBreak(meta, action.keys)
            is ItemAction.PotionColor -> {
                val potionMeta = meta as? PotionMeta ?: return ItemResult(false, listOf(mini.deserialize("<red>Не potion item")))
                potionMeta.color = action.rgb?.let(Color::fromRGB)
            }
            is ItemAction.PotionEffectAdd -> {
                val potionMeta = meta as? PotionMeta ?: return ItemResult(false, listOf(mini.deserialize("<red>Не potion item")))
                potionMeta.addCustomEffect(PotionEffect(action.type, action.duration, action.amplifier, action.ambient, action.particles, action.icon), true)
            }
            is ItemAction.PotionEffectRemove -> {
                val potionMeta = meta as? PotionMeta ?: return ItemResult(false, listOf(mini.deserialize("<red>Не potion item")))
                potionMeta.removeCustomEffect(action.type)
            }
            ItemAction.PotionEffectClear -> {
                val potionMeta = meta as? PotionMeta ?: return ItemResult(false, listOf(mini.deserialize("<red>Не potion item")))
                potionMeta.customEffects.map { it.type }.forEach(potionMeta::removeCustomEffect)
            }
            is ItemAction.HeadSetFromTexture -> {
                val skull = meta as? SkullMeta ?: return ItemResult(false, listOf(mini.deserialize("<red>Не player head")))
                val profile = Bukkit.createProfile(UUID.randomUUID())
                profile.setProperty(ProfileProperty("textures", action.base64))
                skull.playerProfile = profile
            }
            is ItemAction.HeadSetFromOnline -> {
                val skull = meta as? SkullMeta ?: return ItemResult(false, listOf(mini.deserialize("<red>Не player head")))
                val source = Bukkit.getPlayerExact(action.name)
                    ?: return ItemResult(false, listOf(mini.deserialize("<red>Онлайн-игрок <white>${action.name}</white> не найден.")))
                skull.playerProfile = PlayerProfileCopyHelper.copyProfile(source.playerProfile)
            }
            ItemAction.HeadClear -> {
                val skull = meta as? SkullMeta ?: return ItemResult(false, listOf(mini.deserialize("<red>Не player head")))
                skull.playerProfile = null
            }
            is ItemAction.TrimSet -> {
                val armorMeta = meta as? ArmorMeta ?: return ItemResult(false, listOf(mini.deserialize("<red>Item meta не поддерживает ArmorMeta")))
                armorMeta.trim = ArmorTrim(action.material, action.pattern)
            }
            ItemAction.TrimClear -> {
                val armorMeta = meta as? ArmorMeta ?: return ItemResult(false, listOf(mini.deserialize("<red>Item meta не поддерживает ArmorMeta")))
                armorMeta.trim = null
            }
            else -> return ItemResult(false, listOf(mini.deserialize("<red>Ветка не поддерживается для item meta")))
        }
        return ItemResult(true, listOf(mini.deserialize("<green>Изменение применено.")))
    }

    private fun toggleFlag(meta: ItemMeta, action: ItemAction.TooltipToggle, itemType: Material) {
        setTooltipHidden(meta, action.key, action.hide, itemType)
    }

    private fun withoutItalic(component: net.kyori.adventure.text.Component): net.kyori.adventure.text.Component {
        return component.decoration(TextDecoration.ITALIC, false)
    }
}
