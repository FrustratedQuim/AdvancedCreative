package com.ratger.acreative.menus.edit.text

import java.util.Locale

object VanillaRuLocalization {
    private val ruLocale = Locale.forLanguageTag("ru-RU")

    @Volatile
    private var resolver: VanillaTranslationResolver? = null

    fun initialize(resolver: VanillaTranslationResolver) {
        this.resolver = resolver
    }

    fun enchantmentName(path: String): String =
        resolve("enchantment.minecraft.$path") ?: humanize(path)

    fun potionEffectName(path: String): String =
        resolve("effect.minecraft.$path") ?: humanize(path)

    fun blockName(path: String): String =
        resolve("block.minecraft.$path") ?: humanize(path)

    private fun resolve(key: String): String? = resolver?.resolve(key, ruLocale)

    private fun humanize(path: String): String {
        return path.split('_')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.replaceFirstChar { ch ->
                    if (ch.isLowerCase()) ch.titlecase() else ch.toString()
                }
            }
    }
}
