package com.ratger.acreative.menus.edit.potion

import com.ratger.acreative.menus.edit.meta.ItemStackReplacementSupport
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.Registry
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.potion.PotionType

object PotionItemSupport {
    data class PotionForm(
        val material: Material,
        val titlePrefix: String,
        val label: String
    )

    data class PotionEffectEntry(
        val index: Int,
        val effect: PotionEffect,
        val displayName: String,
        val durationLabel: String,
        val displayLevel: Int,
        val showParticles: Boolean,
        val showIcon: Boolean
    )

    private val localizedEffectNames = mapOf(
        "speed" to "Скорость",
        "slowness" to "Замедление",
        "haste" to "Спешка",
        "mining_fatigue" to "Усталость",
        "strength" to "Сила",
        "instant_health" to "Мгновенное исцеление",
        "instant_damage" to "Мгновенный урон",
        "jump_boost" to "Прыгучесть",
        "nausea" to "Тошнота",
        "regeneration" to "Регенерация",
        "resistance" to "Сопротивление",
        "fire_resistance" to "Огнестойкость",
        "water_breathing" to "Подводное дыхание",
        "invisibility" to "Невидимость",
        "blindness" to "Слепота",
        "night_vision" to "Ночное зрение",
        "hunger" to "Голод",
        "weakness" to "Слабость",
        "poison" to "Отравление",
        "wither" to "Иссушение",
        "health_boost" to "Прилив здоровья",
        "absorption" to "Поглощение",
        "saturation" to "Насыщение",
        "glowing" to "Свечение",
        "levitation" to "Левитация",
        "luck" to "Удача",
        "unluck" to "Невезение",
        "slow_falling" to "Плавное падение",
        "conduit_power" to "Сила источника",
        "dolphins_grace" to "Грация дельфина",
        "bad_omen" to "Дурной знак",
        "hero_of_the_village" to "Герой деревни",
        "darkness" to "Темнота",
        "trial_omen" to "Знак испытаний",
        "raid_omen" to "Знак рейда",
        "wind_charged" to "Ветровой заряд",
        "weaving" to "Плетение",
        "oozing" to "Слизистость",
        "infested" to "Заражение"
    )

    private val previewPotionTypeByEffect = mapOf(
        "speed" to PotionType.SWIFTNESS,
        "slowness" to PotionType.SLOWNESS,
        "strength" to PotionType.STRENGTH,
        "jump_boost" to PotionType.LEAPING,
        "regeneration" to PotionType.REGENERATION,
        "poison" to PotionType.POISON,
        "healing" to PotionType.HEALING,
        "instant_health" to PotionType.HEALING,
        "harming" to PotionType.HARMING,
        "instant_damage" to PotionType.HARMING,
        "night_vision" to PotionType.NIGHT_VISION,
        "invisibility" to PotionType.INVISIBILITY,
        "fire_resistance" to PotionType.FIRE_RESISTANCE,
        "water_breathing" to PotionType.WATER_BREATHING,
        "weakness" to PotionType.WEAKNESS,
        "slow_falling" to PotionType.SLOW_FALLING,
        "turtle_master" to PotionType.TURTLE_MASTER,
        "luck" to PotionType.LUCK,
        "wind_charged" to PotionType.WIND_CHARGED,
        "weaving" to PotionType.WEAVING,
        "oozing" to PotionType.OOZING,
        "infested" to PotionType.INFESTED
    )

    val forms: List<PotionForm> = listOf(
        PotionForm(Material.POTION, "①", "Обычное"),
        PotionForm(Material.SPLASH_POTION, "②", "Взрывное"),
        PotionForm(Material.LINGERING_POTION, "③", "Туманное"),
        PotionForm(Material.TIPPED_ARROW, "④", "Стрела")
    )

    fun color(item: ItemStack): Color? = potionMeta(item)?.color

    fun setColor(item: ItemStack, color: Color?) {
        val meta = potionMeta(item) ?: return
        meta.color = color
        item.itemMeta = meta
    }

    fun effects(item: ItemStack): List<PotionEffect> {
        val meta = potionMeta(item) ?: return emptyList()
        val customEffects = meta.customEffects
        val baseEffects = basePotionEffects(meta)
        if (customEffects.isEmpty()) return baseEffects
        val customKeys = customEffects.map { keyPath(it.type) }.toSet()
        return buildList {
            baseEffects.forEach { presetEffect ->
                if (!customKeys.contains(keyPath(presetEffect.type))) {
                    add(presetEffect)
                }
            }
            addAll(customEffects)
        }
    }

    fun effectEntries(item: ItemStack): List<PotionEffectEntry> {
        return effects(item).mapIndexed { index, effect ->
            val normalizedDuration = normalizeDurationForItemForm(item.type, effect.type, effect.duration)
            PotionEffectEntry(
                index = index,
                effect = effect,
                displayName = displayName(effect.type),
                durationLabel = durationLabel(normalizedDuration),
                displayLevel = displayLevel(effect.amplifier),
                showParticles = effect.hasParticles(),
                showIcon = effect.hasIcon()
            )
        }
    }

    fun addEffect(item: ItemStack, effect: PotionEffect) {
        val meta = potionMeta(item) ?: return
        meta.addCustomEffect(effect, true)
        item.itemMeta = meta
    }

    fun removeEffect(item: ItemStack, type: PotionEffectType) {
        val meta = potionMeta(item) ?: return
        val hadCustom = meta.removeCustomEffect(type)
        if (!hadCustom && basePotionEffects(meta).any { it.type == type }) {
            meta.basePotionType = PotionType.WATER
        }
        item.itemMeta = meta
    }

    fun clearEffects(item: ItemStack) {
        val meta = potionMeta(item) ?: return
        meta.customEffects.map { it.type }.forEach(meta::removeCustomEffect)
        meta.basePotionType = PotionType.WATER
        item.itemMeta = meta
    }

    fun currentFormIndex(item: ItemStack): Int = forms.indexOfFirst { it.material == item.type }.takeIf { it >= 0 } ?: 0

    fun setForm(item: ItemStack, form: PotionForm): ItemStack {
        return ItemStackReplacementSupport.replaceItemId(item, form.material)
    }

    fun displayName(type: PotionEffectType): String {
        val path = keyPath(type)
        return localizedEffectNames[path] ?: humanize(path)
    }

    fun keyPath(type: PotionEffectType): String {
        return Registry.MOB_EFFECT.getKey(type)?.key ?: "unknown"
    }

    fun effectSuggestions(prefix: String): List<String> {
        return Registry.MOB_EFFECT.iterator().asSequence()
            .map { keyPath(it) }
            .filter { it.startsWith(prefix, ignoreCase = true) }
            .sorted()
            .toList()
    }

    fun displayLevel(amplifier: Int): Int = amplifier + 1

    fun seconds(ticks: Int): Int = (ticks / 20).coerceAtLeast(1)

    fun durationLabel(ticks: Int): String {
        if (ticks == PotionEffect.INFINITE_DURATION) return "∞"
        return seconds(ticks).toString()
    }

    fun previewPotionType(type: PotionEffectType): PotionType? {
        val path = keyPath(type)
        return previewPotionTypeByEffect[path]
    }

    fun denormalizeDurationForItemForm(itemType: Material, effectType: PotionEffectType, visibleDuration: Int): Int {
        if (visibleDuration == PotionEffect.INFINITE_DURATION) return visibleDuration
        if (effectType.isInstant) return visibleDuration

        val multiplier = durationStorageMultiplier(itemType)
        if (multiplier == 1) return visibleDuration
        return (visibleDuration.toLong() * multiplier.toLong())
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
    }

    private fun potionMeta(item: ItemStack): PotionMeta? = item.itemMeta as? PotionMeta
    private fun basePotionEffects(meta: PotionMeta): List<PotionEffect> = meta.basePotionType?.potionEffects ?: emptyList()

    private fun normalizeDurationForItemForm(itemType: Material, effectType: PotionEffectType, duration: Int): Int {
        if (duration == PotionEffect.INFINITE_DURATION) return duration
        if (effectType.isInstant) return duration

        val multiplier = durationStorageMultiplier(itemType)
        if (multiplier == 1) return duration
        return (duration / multiplier).coerceAtLeast(1)
    }

    private fun durationStorageMultiplier(itemType: Material): Int {
        return when (itemType) {
            Material.LINGERING_POTION -> 4
            Material.TIPPED_ARROW -> 8
            else -> 1
        }
    }

    private fun humanize(path: String): String {
        return path.split('_').joinToString(" ") {
            it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() }
        }
    }
}
