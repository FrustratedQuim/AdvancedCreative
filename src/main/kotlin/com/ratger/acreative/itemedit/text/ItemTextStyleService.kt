package com.ratger.acreative.itemedit.text

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.inventory.ItemStack

class ItemTextStyleService() {
    private val mini = MiniMessage.miniMessage()
    private val plain = PlainTextComponentSerializer.plainText()

    fun hasCustomName(item: ItemStack): Boolean = item.itemMeta?.hasCustomName() == true

    fun customName(item: ItemStack): Component? = item.itemMeta?.customName()

    fun setCustomName(item: ItemStack, component: Component?) {
        val meta = item.itemMeta ?: return
        meta.customName(component)
        item.itemMeta = meta
    }

    fun hasLore(item: ItemStack): Boolean = item.itemMeta?.hasLore() == true && !item.itemMeta?.lore().isNullOrEmpty()

    fun lore(item: ItemStack): List<Component> = item.itemMeta?.lore().orEmpty()

    fun setLore(item: ItemStack, lore: List<Component>?) {
        val meta = item.itemMeta ?: return
        meta.lore(lore)
        item.itemMeta = meta
    }

    fun parseMiniMessage(input: String): Component = mini.deserialize(input)

    fun serializeMiniMessage(component: Component): String = mini.serialize(component)

    fun stripOnlyColor(component: Component): Component = mutate(component) { style -> style.color(null) }

    fun stripOnlyShadow(component: Component): Component = mutate(component) { style -> style.shadowColor(null) }

    fun applyOrderedColors(component: Component, orderedColors: List<String>): Component {
        val normalized = orderedColors.distinct()
        val stripped = stripOnlyColor(component).decoration(TextDecoration.ITALIC, false)
        if (normalized.isEmpty()) return stripped

        val payload = serializeMiniMessage(normalizeForGradient(stripped))
        return if (normalized.size == 1) {
            val color = resolveNamedColor(normalized.first())
            if (color == null) {
                mini.deserialize("<${normalized.first()}>$payload</${normalized.first()}>")
            } else {
                mutate(stripped) { style -> style.color(color) }
            }
        } else {
            mini.deserialize("<gradient:${normalized.joinToString(":")}>$payload</gradient>")
        }
    }

    fun applyShadow(component: Component, shadowColor: String?): Component {
        val stripped = stripOnlyShadow(component).decoration(TextDecoration.ITALIC, false)
        if (shadowColor == null) return stripped
        val payload = serializeMiniMessage(stripped)
        return mini.deserialize("<shadow:$shadowColor:1>$payload</shadow>")
    }

    fun resolveShadowColor(shadowStateKey: String): String? {
        return shadowStateKey
            .takeUnless { it == TextStylePalette.ORDINARY_SHADOW_KEY }
            ?.lowercase()
    }

    fun preview(component: Component?, fallback: String): String {
        if (component == null) return fallback
        val raw = plain.serialize(component).replace('\n', ' ').trim()
        return raw.ifBlank { fallback }
    }

    fun detectOrderedColors(component: Component): List<String> {
        val serialized = serializeMiniMessage(component)
        val gradient = Regex("<gradient:([^>]+)>", RegexOption.IGNORE_CASE).find(serialized)
        if (gradient != null) {
            return gradient.groupValues[1].split(':').map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        }
        val single = Regex("<(white|gray|dark_gray|black|yellow|gold|red|dark_red|green|dark_green|aqua|dark_aqua|blue|dark_blue|light_purple|dark_purple)>", RegexOption.IGNORE_CASE)
            .find(serialized)
            ?.groupValues
            ?.getOrNull(1)
            ?.lowercase()
        return if (single == null) emptyList() else listOf(single)
    }

    fun detectShadowColor(component: Component): String? {
        val serialized = serializeMiniMessage(component)
        return Regex("<shadow:([a-z_]+):1>", RegexOption.IGNORE_CASE)
            .find(serialized)
            ?.groupValues
            ?.getOrNull(1)
            ?.lowercase()
    }

    private fun mutate(component: Component, styleMutation: (net.kyori.adventure.text.format.Style) -> net.kyori.adventure.text.format.Style): Component {
        val updatedStyle = styleMutation(component.style())
        val updatedChildren = component.children().map { child -> mutate(child, styleMutation) }
        return component.style(updatedStyle).children(updatedChildren)
    }

    private fun normalizeForGradient(component: Component): Component {
        if (!containsTranslatable(component)) {
            return component
        }
        val plainText = preview(component, prettyMaterialNameFallback(component))
        return Component.text(plainText).decoration(TextDecoration.ITALIC, false)
    }

    private fun containsTranslatable(component: Component): Boolean {
        if (component is TranslatableComponent) return true
        return component.children().any(::containsTranslatable)
    }

    private fun prettyMaterialNameFallback(component: Component): String {
        return plain.serialize(component).ifBlank { "Item" }
    }

    private fun resolveNamedColor(colorKey: String): TextColor? {
        return NamedTextColor.NAMES.value(colorKey.lowercase())
    }
}
