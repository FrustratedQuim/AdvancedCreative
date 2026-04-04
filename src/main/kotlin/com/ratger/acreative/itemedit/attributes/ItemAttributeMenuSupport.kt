@file:Suppress("UnstableApiUsage")

package com.ratger.acreative.itemedit.attributes

import com.google.common.collect.LinkedHashMultimap
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers
import org.bukkit.Material
import org.bukkit.Registry
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.EquipmentSlotGroup
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

    private val slotDisplayNames = mapOf(
        EquipmentSlotGroup.ANY to "Любой слот",
        EquipmentSlotGroup.HAND to "Любая рука",
        EquipmentSlotGroup.MAINHAND to "Главная рука",
        EquipmentSlotGroup.OFFHAND to "Вторая рука",
        EquipmentSlotGroup.ARMOR to "Броня",
        EquipmentSlotGroup.HEAD to "Шлем",
        EquipmentSlotGroup.CHEST to "Нагрудник",
        EquipmentSlotGroup.LEGS to "Поножи",
        EquipmentSlotGroup.FEET to "Ботинки",
        EquipmentSlotGroup.BODY to "Тело"
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

    fun displaySlot(slotGroup: EquipmentSlotGroup): String {
        return slotDisplayNames[slotGroup] ?: toReadableName(slotGroup.toString().lowercase(Locale.ROOT))
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

    fun clampAmount(attribute: Attribute, parsed: BigDecimal): Double {
        val (min, max) = attributeRange(attribute)
        return parsed.coerceIn(min, max).toDouble()
    }

    private fun attributeRange(attribute: Attribute): Pair<BigDecimal, BigDecimal> {
        return when (attribute) {
            Attribute.MAX_HEALTH -> "1" bd "1024"
            Attribute.FOLLOW_RANGE -> "0" bd "2048"
            Attribute.KNOCKBACK_RESISTANCE -> "0" bd "1"
            Attribute.MOVEMENT_SPEED -> "0" bd "1024"
            Attribute.FLYING_SPEED -> "0" bd "1024"
            Attribute.ATTACK_DAMAGE -> "0" bd "2048"
            Attribute.ATTACK_KNOCKBACK -> "0" bd "5"
            Attribute.ATTACK_SPEED -> "0" bd "1024"
            Attribute.ARMOR -> "0" bd "30"
            Attribute.ARMOR_TOUGHNESS -> "0" bd "20"
            Attribute.LUCK -> "-1024" bd "1024"
            Attribute.MAX_ABSORPTION -> "0" bd "2048"
            Attribute.SAFE_FALL_DISTANCE -> "-1024" bd "1024"
            Attribute.SCALE -> "0.0625" bd "16"
            Attribute.STEP_HEIGHT -> "0" bd "10"
            Attribute.GRAVITY -> "-1" bd "1"
            Attribute.JUMP_STRENGTH -> "0" bd "32"
            Attribute.BURNING_TIME -> "0" bd "1024"
            Attribute.EXPLOSION_KNOCKBACK_RESISTANCE -> "0" bd "1"
            Attribute.MOVEMENT_EFFICIENCY -> "0" bd "1"
            Attribute.OXYGEN_BONUS -> "0" bd "1024"
            Attribute.WATER_MOVEMENT_EFFICIENCY -> "0" bd "1"
            Attribute.TEMPT_RANGE -> "0" bd "2048"
            Attribute.BLOCK_INTERACTION_RANGE -> "0" bd "64"
            Attribute.ENTITY_INTERACTION_RANGE -> "0" bd "64"
            Attribute.BLOCK_BREAK_SPEED -> "0" bd "1024"
            Attribute.MINING_EFFICIENCY -> "0" bd "1024"
            Attribute.SUBMERGED_MINING_SPEED -> "0" bd "20"
            Attribute.SNEAKING_SPEED -> "0" bd "1"
            Attribute.SWEEPING_DAMAGE_RATIO -> "0" bd "1"
            Attribute.SPAWN_REINFORCEMENTS -> "0" bd "1"
            else -> "-1024" bd "1024"
        }
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
