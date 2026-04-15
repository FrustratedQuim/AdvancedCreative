package com.ratger.acreative.menus.edit.text

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import java.util.Locale

class VanillaNameLocalizationService(
    private val translationResolver: VanillaTranslationResolver
) {
    private val plain = PlainTextComponentSerializer.plainText()

    fun localizeToPlainText(component: Component, locale: Locale): String {
        val localized = buildString {
            append(localizedSelf(component, locale))
            for (child in component.children()) {
                append(localizeToPlainText(child, locale))
            }
        }.trim()

        return localized.ifBlank {
            plain.serialize(component).trim()
        }
    }

    private fun localizedSelf(component: Component, locale: Locale): String {
        return when (component) {
            is TextComponent -> component.content()
            is TranslatableComponent -> {
                translationResolver.resolve(component.key(), locale)
                    ?: component.fallback()
                    ?: component.key()
            }
            else -> ""
        }
    }
}
