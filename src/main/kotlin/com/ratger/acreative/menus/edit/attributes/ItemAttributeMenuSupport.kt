@file:Suppress("UnstableApiUsage") // Experimental AttributeModifer

package com.ratger.acreative.menus.edit.attributes

import com.google.common.collect.LinkedHashMultimap
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers
import org.bukkit.Material
import org.bukkit.Registry
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import java.math.BigDecimal
import java.util.*

object ItemAttributeMenuSupport {
    data class AttributeEntry(
        val attribute: Attribute,
        val modifier: AttributeModifier
    )

    private data class AttributeIdentity(
        val attribute: Attribute,
        val key: String
    )

    private val attributeDisplayNames = mapOf(
        "max_health" to "Максимальное здоровье",
        "max_absorption" to "Максимальное поглощение",
        "follow_range" to "Дальность обнаружения",
        "knockback_resistance" to "Сопротивление отбрасыванию",
        "movement_speed" to "Скорость",
        "flying_speed" to "Скорость полёта",
        "attack_damage" to "Урон",
        "attack_knockback" to "Отдача",
        "attack_speed" to "Скорость атаки",
        "armor" to "Броня",
        "armor_toughness" to "Твёрдость брони",
        "luck" to "Удача",
        "spawn_reinforcements" to "Шанс подкрепления",
        "jump_strength" to "Сила прыжка",
        "burning_time" to "Время горения",
        "explosion_knockback_resistance" to "Сопротивление отбрасыванию от взрыва",
        "fall_damage_multiplier" to "Множитель урона от падения",
        "gravity" to "Гравитация",
        "safe_fall_distance" to "Безопасная высота падения",
        "scale" to "Размер",
        "step_height" to "Высота шага",
        "block_interaction_range" to "Дальность взаимодействия с блоками",
        "entity_interaction_range" to "Дальность взаимодействия с сущностями",
        "block_break_speed" to "Скорость ломания блоков",
        "mining_efficiency" to "Эффективность копания",
        "submerged_mining_speed" to "Скорость копания под водой",
        "sneaking_speed" to "Скорость подкрадывания",
        "movement_efficiency" to "Эффективность движения",
        "oxygen_bonus" to "Запас воздуха",
        "water_movement_efficiency" to "Скорость движения в воде",
        "tempt_range" to "Дальность приманки",
        "sweeping_damage_ratio" to "Коэффициент разящего удара"
    )

    private val attributeSuggestedValues = mapOf(
        Attribute.MAX_HEALTH to listOf("4", "20", "40", "100", "600", "1024"),
        Attribute.MAX_ABSORPTION to listOf("4", "20", "40", "100", "600", "2048"),
        Attribute.FOLLOW_RANGE to listOf("8", "16", "32", "64", "128", "2048"),
        Attribute.KNOCKBACK_RESISTANCE to listOf("0.1", "0.25", "0.5", "0.75", "1"),
        Attribute.MOVEMENT_SPEED to listOf("0.05", "0.1", "0.2", "0.3", "0.5", "1"),
        Attribute.FLYING_SPEED to listOf("0.05", "0.1", "0.2", "0.4", "0.8", "1"),
        Attribute.ATTACK_DAMAGE to listOf("1", "4", "8", "12", "20", "100"),
        Attribute.ATTACK_KNOCKBACK to listOf("0.5", "1", "2", "3", "5"),
        Attribute.ATTACK_SPEED to listOf("1", "2", "4", "8", "16"),
        Attribute.ARMOR to listOf("1", "2", "4", "8", "16", "30"),
        Attribute.ARMOR_TOUGHNESS to listOf("1", "2", "4", "8", "12", "20"),
        Attribute.LUCK to listOf("-10", "1", "5", "10", "100"),
        Attribute.SAFE_FALL_DISTANCE to listOf("3", "8", "16", "32", "128"),
        Attribute.SCALE to listOf("0.25", "0.5", "1", "2", "4", "16"),
        Attribute.STEP_HEIGHT to listOf("0.5", "1", "2", "4", "10"),
        Attribute.GRAVITY to listOf("-0.08", "0", "0.04", "0.08", "0.5", "1"),
        Attribute.JUMP_STRENGTH to listOf("0.5", "1", "2", "5", "10", "32"),
        Attribute.BURNING_TIME to listOf("20", "100", "200", "600", "1024"),
        Attribute.EXPLOSION_KNOCKBACK_RESISTANCE to listOf("0.1", "0.25", "0.5", "0.75", "1"),
        Attribute.FALL_DAMAGE_MULTIPLIER to listOf("0", "0.25", "0.5", "1", "2", "10"),
        Attribute.MOVEMENT_EFFICIENCY to listOf("0.1", "0.25", "0.5", "0.75", "1"),
        Attribute.OXYGEN_BONUS to listOf("10", "50", "100", "300", "1024"),
        Attribute.WATER_MOVEMENT_EFFICIENCY to listOf("0.1", "0.25", "0.5", "0.75", "1"),
        Attribute.TEMPT_RANGE to listOf("4", "10", "20", "40", "80", "2048"),
        Attribute.BLOCK_INTERACTION_RANGE to listOf("2", "4", "6", "8", "16", "64"),
        Attribute.ENTITY_INTERACTION_RANGE to listOf("2", "3", "4", "6", "8", "64"),
        Attribute.BLOCK_BREAK_SPEED to listOf("1", "5", "10", "50", "100", "1024"),
        Attribute.MINING_EFFICIENCY to listOf("1", "5", "10", "50", "100", "1024"),
        Attribute.SUBMERGED_MINING_SPEED to listOf("0.5", "1", "5", "10", "20"),
        Attribute.SNEAKING_SPEED to listOf("0.1", "0.25", "0.5", "0.75", "1"),
        Attribute.SWEEPING_DAMAGE_RATIO to listOf("0.1", "0.25", "0.5", "0.75", "1"),
        Attribute.SPAWN_REINFORCEMENTS to listOf("0.05", "0.1", "0.25", "0.5", "1")
    )

    fun listEffectiveEntries(item: ItemStack): List<AttributeEntry> {
        val effective = currentEffectiveAttributes(item)
        return effective.entries().map { AttributeEntry(it.key, it.value) }
    }

    fun currentEffectiveAttributes(item: ItemStack): LinkedHashMultimap<Attribute, AttributeModifier> {
        if (hasExplicitAttributeOverride(item)) {
            val explicit = LinkedHashMultimap.create<Attribute, AttributeModifier>()
            val component = item.getData(DataComponentTypes.ATTRIBUTE_MODIFIERS)
            component?.modifiers()?.forEach { entry ->
                explicit.put(entry.attribute(), entry.modifier())
            }
            return explicit
        }
        return defaultEffectiveAttributes(item.type)
    }

    fun hasExplicitAttributeOverride(item: ItemStack): Boolean {
        return item.hasData(DataComponentTypes.ATTRIBUTE_MODIFIERS)
    }

    fun writeExplicitAttributes(item: ItemStack, attributes: LinkedHashMultimap<Attribute, AttributeModifier>) {
        val currentComponent = if (hasExplicitAttributeOverride(item)) {
            item.getData(DataComponentTypes.ATTRIBUTE_MODIFIERS)
        } else {
            null
        }
        val showInTooltip = currentComponent?.showInTooltip() ?: true
        val builder = ItemAttributeModifiers.itemAttributes().showInTooltip(showInTooltip)
        attributes.entries().forEach { (attribute, modifier) ->
            builder.addModifier(attribute, modifier)
        }
        item.setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, builder.build())
    }

    fun defaultEffectiveAttributes(material: Material?): LinkedHashMultimap<Attribute, AttributeModifier> {
        val result = LinkedHashMultimap.create<Attribute, AttributeModifier>()
        val type = material ?: return result
        if (!type.isItem) return result

        val seen = linkedSetOf<AttributeIdentity>()
        for (slot in EquipmentSlot.entries) {
            val bySlot = type.getDefaultAttributeModifiers(slot)
            for ((attribute, modifier) in bySlot.entries()) {
                val identity = AttributeIdentity(attribute, modifier.key.asString())
                if (seen.add(identity)) {
                    result.put(attribute, modifier)
                }
            }
        }
        return result
    }

    fun displayAttributeName(attribute: Attribute): String {
        val key = attribute.key.key
        return attributeDisplayNames[key] ?: toReadableName(key)
    }

    fun displayOperation(operation: AttributeModifier.Operation): String {
        return when (operation) {
            AttributeModifier.Operation.ADD_NUMBER -> "Число"
            AttributeModifier.Operation.ADD_SCALAR -> "Процент"
            AttributeModifier.Operation.MULTIPLY_SCALAR_1 -> "Процент (Общий)"
        }
    }

    fun formatAmount(modifier: AttributeModifier): String {
        val value = when (modifier.operation) {
            AttributeModifier.Operation.ADD_NUMBER -> BigDecimal.valueOf(modifier.amount)
            AttributeModifier.Operation.ADD_SCALAR,
            AttributeModifier.Operation.MULTIPLY_SCALAR_1 -> BigDecimal.valueOf(modifier.amount).multiply(BigDecimal.valueOf(100))
        }
        val plain = value.stripTrailingZeros().toPlainString()
        val sign = if (value.signum() >= 0) "+" else ""
        val suffix = if (modifier.operation == AttributeModifier.Operation.ADD_NUMBER) "" else "%"
        return "$sign$plain$suffix"
    }

    fun attributeTokenMap(): Map<String, Attribute> {
        return Registry.ATTRIBUTE.iterator().asSequence()
            .associateBy(
                keySelector = { it.key.key.lowercase(Locale.ROOT) },
                valueTransform = { it }
            )
            .toSortedMap()
    }

    fun clampAmount(parsed: BigDecimal): Double {
        val (min, max) = attributeRange()
        return parsed.coerceIn(min, max).toDouble()
    }

    fun suggestedValues(attribute: Attribute): List<String> {
        return attributeSuggestedValues[attribute] ?: fallbackSuggestedValues()
    }

    private fun attributeRange(): Pair<BigDecimal, BigDecimal> {
        return "-2048" bd "2048"
    }

    private fun fallbackSuggestedValues(): List<String> {
        val (min, max) = attributeRange()
        val midpoint = min.add(max).divide(BigDecimal("2"))
        val quarter = min.add(max.subtract(min).divide(BigDecimal("4")))
        return linkedSetOf(
            min.toPlainString(),
            quarter.stripTrailingZeros().toPlainString(),
            midpoint.stripTrailingZeros().toPlainString(),
            max.stripTrailingZeros().toPlainString()
        ).toList()
    }

    private fun toReadableName(value: String): String {
        return value
            .substringAfter(':')
            .replace('_', ' ')
            .split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.lowercase(Locale.ROOT).replaceFirstChar { it.titlecase(Locale.ROOT) }
            }
    }

    private infix fun String.bd(max: String): Pair<BigDecimal, BigDecimal> {
        return BigDecimal(this) to BigDecimal(max)
    }
}
