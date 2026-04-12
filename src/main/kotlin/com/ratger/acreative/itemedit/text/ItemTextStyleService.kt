package com.ratger.acreative.itemedit.text

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.inventory.ItemStack
import java.util.Locale

class ItemTextStyleService(
    private val vanillaNameLocalizationService: VanillaNameLocalizationService
) {
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

    enum class TextInputMode {
        LITERAL_ESCAPED,
        FORMATTED_MINIMESSAGE
    }

    fun parseInputText(input: String, mode: TextInputMode, vararg tagResolvers: TagResolver): Component {
        val normalizedInput = when (mode) {
            TextInputMode.LITERAL_ESCAPED -> escapeMiniMessageForPreview(input, *tagResolvers)
            TextInputMode.FORMATTED_MINIMESSAGE -> input
        }
        return mini.deserialize(normalizedInput, *tagResolvers)
    }

    fun parseRawMiniMessage(input: String, vararg tagResolvers: TagResolver): Component =
        mini.deserialize(input, *tagResolvers)

    fun escapeForMiniMessage(input: String, vararg tagResolvers: TagResolver): String =
        escapeMiniMessageForPreview(input, *tagResolvers)

    fun escapeMiniMessageForPreview(input: String, vararg tagResolvers: TagResolver): String =
        mini.escapeTags(input, *tagResolvers)

    fun serializeMiniMessage(component: Component): String = mini.serialize(component)

    fun serializeLoreToRawMiniMessageLines(lore: List<Component>): List<String> =
        lore.map { line ->
            val serialized = mini.serialize(line)
            if (plain.serialize(line).isBlank() && serialized.isBlank()) "" else serialized
        }

    fun setCustomNameRawMiniMessage(item: ItemStack, rawMiniMessage: String): Component {
        val parsed = parseRawMiniMessage(rawMiniMessage).decoration(TextDecoration.ITALIC, false)
        setCustomName(item, parsed)
        return parsed
    }

    fun materializeLoreFromVirtualLines(virtualLines: List<String>): List<Component> {
        val lastNonBlankIndex = virtualLines.indexOfLast { it.isNotBlank() }
        if (lastNonBlankIndex < 0) return emptyList()

        return (0..lastNonBlankIndex).map { index ->
            val raw = virtualLines[index]
            if (raw.isBlank()) {
                Component.empty().decoration(TextDecoration.ITALIC, false)
            } else {
                parseRawMiniMessage(raw).decoration(TextDecoration.ITALIC, false)
            }
        }
    }

    fun ensureVirtualLoreLines(lines: List<String>, minimumSize: Int = 3): List<String> {
        val safeMinimumSize = minimumSize.coerceAtLeast(1)
        val mutable = lines.toMutableList()
        val lastNonBlankIndex = mutable.indexOfLast { it.isNotBlank() }

        val targetSize = if (lastNonBlankIndex < 0) {
            safeMinimumSize
        } else {
            maxOf(safeMinimumSize, lastNonBlankIndex + 2)
        }

        while (mutable.size < targetSize) {
            mutable.add("")
        }
        while (mutable.size > targetSize) {
            mutable.removeLast()
        }
        return mutable
    }

    fun updateVirtualLoreLine(lines: List<String>, index: Int, rawValue: String, minimumSize: Int = 3): List<String> {
        val safeIndex = index.coerceAtLeast(0)
        val mutable = ensureVirtualLoreLines(lines, minimumSize).toMutableList()
        while (mutable.size <= safeIndex) {
            mutable.add("")
        }
        mutable[safeIndex] = rawValue
        return ensureVirtualLoreLines(mutable, minimumSize)
    }

    fun clearVirtualLoreLine(lines: List<String>, index: Int, minimumSize: Int = 3): List<String> =
        updateVirtualLoreLine(lines, index, "", minimumSize)

    fun setLoreFromVirtualRawLines(item: ItemStack, virtualLines: List<String>) {
        val materialized = materializeLoreFromVirtualLines(virtualLines)
        setLore(item, materialized.ifEmpty { null })
    }

    fun stripOnlyColor(component: Component): Component = mutate(component) { style -> style.color(null) }

    fun stripOnlyShadow(component: Component): Component = mutate(component) { style -> style.shadowColor(null) }

    fun applyOrderedColors(component: Component, orderedColors: List<String>, locale: Locale? = null): Component {
        val normalized = orderedColors.distinct()
        val stripped = stripOnlyColor(component).decoration(TextDecoration.ITALIC, false)
        if (normalized.isEmpty()) return stripped

        return if (normalized.size == 1) {
            val color = resolveNamedColor(normalized.first())
            if (color == null) {
                val payload = serializeMiniMessage(stripped)
                mini.deserialize("<${normalized.first()}>$payload</${normalized.first()}>")
            } else {
                mutate(stripped) { style -> style.color(color) }
            }
        } else {
            val text = gradientTextPayload(stripped, locale)
            applyGradient(text, normalized)
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

    private fun gradientTextPayload(component: Component, locale: Locale?): String {
        val plainText = if (locale != null && containsTranslatable(component)) {
            vanillaNameLocalizationService.localizeToPlainText(component, locale)
        } else {
            preview(component, prettyMaterialNameFallback(component))
        }
        return plainText.ifBlank { prettyMaterialNameFallback(component) }
    }

    private fun applyGradient(text: String, colors: List<String>): Component {
        if (text.isEmpty()) return Component.empty().decoration(TextDecoration.ITALIC, false)

        val palette = colors.map { key -> resolveNamedColor(key) ?: NamedTextColor.WHITE }
        if (palette.size < 2) {
            return Component.text(text).color(palette.first()).decoration(TextDecoration.ITALIC, false)
        }

        val children = text.mapIndexed { index, symbol ->
            val progress = if (text.length <= 1) 0.0 else index.toDouble() / (text.length - 1).toDouble()
            val scaled = progress * (palette.size - 1)
            val leftIndex = scaled.toInt().coerceIn(0, palette.lastIndex)
            val rightIndex = (leftIndex + 1).coerceAtMost(palette.lastIndex)
            val blend = (scaled - leftIndex).toFloat()
            val color = if (leftIndex == rightIndex) {
                palette[leftIndex]
            } else {
                TextColor.lerp(blend, palette[leftIndex], palette[rightIndex])
            }
            Component.text(symbol.toString()).color(color)
        }

        return Component.empty().children(children).decoration(TextDecoration.ITALIC, false)
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
