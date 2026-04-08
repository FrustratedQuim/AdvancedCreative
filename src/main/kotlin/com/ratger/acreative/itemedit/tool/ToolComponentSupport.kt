@file:Suppress("UnstableApiUsage") // Experimental Tool

package com.ratger.acreative.itemedit.tool

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.Tool
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

object ToolComponentSupport {
    private val toolTypeBySuffix = linkedMapOf(
        "_PICKAXE" to Material.WOODEN_PICKAXE,
        "_AXE" to Material.WOODEN_AXE,
        "_HOE" to Material.WOODEN_HOE,
        "_SHOVEL" to Material.WOODEN_SHOVEL,
        "_SWORD" to Material.WOODEN_SWORD
    )

    fun explicitSnapshot(item: ItemStack): Tool? = item.getData(DataComponentTypes.TOOL)

    fun prototypeSnapshot(item: ItemStack): Tool? = item.type.getDefaultData(DataComponentTypes.TOOL)

    fun resolvedSnapshot(item: ItemStack): Tool? = explicitSnapshot(item) ?: prototypeSnapshot(item)

    fun effectiveMiningSpeed(item: ItemStack): Float? {
        val tool = resolvedSnapshot(item) ?: return null
        val speedRule = tool.rules().firstOrNull { it.speed() != null }?.speed()
        return speedRule ?: tool.defaultMiningSpeed()
    }

    fun effectiveDamagePerBlock(item: ItemStack): Int? = resolvedSnapshot(item)?.damagePerBlock()

    fun isMiningSpeedOrdinary(item: ItemStack): Boolean {
        val explicit = explicitSnapshot(item) ?: return true
        val prototype = prototypeSnapshot(item) ?: return false
        return miningSpeedSignature(explicit) == miningSpeedSignature(prototype)
    }

    fun isDamagePerBlockOrdinary(item: ItemStack): Boolean {
        val explicit = explicitSnapshot(item) ?: return true
        val prototype = prototypeSnapshot(item) ?: return false
        return explicit.damagePerBlock() == prototype.damagePerBlock()
    }

    fun setMiningSpeed(item: ItemStack, value: Float): Boolean {
        val explicit = explicitSnapshot(item)
        val base = explicit ?: prototypeSnapshot(item)
        val rebuilt = if (base != null) {
            val toolPrototype = matchingToolPrototype(item)
            if (toolPrototype != null) {
                val speedRule = toolPrototype.rules().firstOrNull { it.speed() != null }
                if (speedRule != null) {
                    Tool.tool()
                        .defaultMiningSpeed(base.defaultMiningSpeed())
                        .damagePerBlock(base.damagePerBlock())
                        .addRule(Tool.rule(speedRule.blocks(), value, speedRule.correctForDrops()))
                        .build()
                } else {
                    Tool.tool()
                        .defaultMiningSpeed(value)
                        .damagePerBlock(base.damagePerBlock())
                        .addRules(base.rules())
                        .build()
                }
            } else {
                val combinedRules = speedRuleTemplates()
                Tool.tool()
                    .defaultMiningSpeed(base.defaultMiningSpeed())
                    .damagePerBlock(base.damagePerBlock())
                    .addRules(combinedRules.map { Tool.rule(it.blocks(), value, it.correctForDrops()) })
                    .build()
            }
        } else {
            val combinedRules = speedRuleTemplates()
            if (combinedRules.isNotEmpty()) {
                Tool.tool()
                    .defaultMiningSpeed(1f)
                    .damagePerBlock(1)
                    .addRules(combinedRules.map { Tool.rule(it.blocks(), value, it.correctForDrops()) })
                    .build()
            } else {
                Tool.tool().defaultMiningSpeed(value).damagePerBlock(1).build()
            }
        }
        item.setData(DataComponentTypes.TOOL, rebuilt)
        normalizeAfterMutation(item)
        return true
    }

    fun setDamagePerBlock(item: ItemStack, value: Int): Boolean {
        val base = explicitSnapshot(item) ?: prototypeSnapshot(item)
        val rebuilt = if (base != null) {
            Tool.tool()
                .defaultMiningSpeed(base.defaultMiningSpeed())
                .damagePerBlock(value)
                .addRules(base.rules())
                .build()
        } else {
            val shovelRule = toolPrototype(Material.WOODEN_SHOVEL)?.rules()?.firstOrNull { it.speed() != null }
            val builder = Tool.tool()
                .defaultMiningSpeed(1f)
                .damagePerBlock(value)
            if (shovelRule != null) {
                builder.addRule(Tool.rule(shovelRule.blocks(), 1f, shovelRule.correctForDrops()))
            }
            builder.build()
        }
        item.setData(DataComponentTypes.TOOL, rebuilt)
        normalizeAfterMutation(item)
        return true
    }

    fun resetMiningSpeed(item: ItemStack) {
        val explicit = explicitSnapshot(item) ?: return
        val prototype = prototypeSnapshot(item) ?: run {
            item.unsetData(DataComponentTypes.TOOL)
            return
        }
        val hasSpeedRules = explicit.rules().any { it.speed() != null } || prototype.rules().any { it.speed() != null }
        val rebuiltRules = if (hasSpeedRules) {
            explicit.rules().mapIndexed { index, rule ->
                val prototypeSpeed = prototype.rules().getOrNull(index)?.speed()
                Tool.rule(rule.blocks(), rule.speed()?.let { prototypeSpeed }, rule.correctForDrops())
            }
        } else {
            explicit.rules()
        }
        val rebuilt = Tool.tool()
            .defaultMiningSpeed(if (hasSpeedRules) explicit.defaultMiningSpeed() else prototype.defaultMiningSpeed())
            .damagePerBlock(explicit.damagePerBlock())
            .addRules(rebuiltRules)
            .build()
        item.setData(DataComponentTypes.TOOL, rebuilt)
        normalizeAfterMutation(item)
    }

    fun resetDamagePerBlock(item: ItemStack) {
        val explicit = explicitSnapshot(item) ?: return
        val prototype = prototypeSnapshot(item) ?: return
        val rebuilt = Tool.tool()
            .defaultMiningSpeed(explicit.defaultMiningSpeed())
            .damagePerBlock(prototype.damagePerBlock())
            .addRules(explicit.rules())
            .build()
        item.setData(DataComponentTypes.TOOL, rebuilt)
        normalizeAfterMutation(item)
    }

    fun normalizeAfterMutation(item: ItemStack) {
        val explicit = explicitSnapshot(item) ?: return
        val prototype = prototypeSnapshot(item) ?: return
        if (componentsMatch(explicit, prototype)) {
            item.unsetData(DataComponentTypes.TOOL)
        }
    }

    private fun componentsMatch(a: Tool, b: Tool): Boolean {
        return a.defaultMiningSpeed() == b.defaultMiningSpeed() &&
            a.damagePerBlock() == b.damagePerBlock() &&
            a.rules() == b.rules()
    }

    private fun miningSpeedSignature(tool: Tool): String {
        val speedRules = tool.rules().mapNotNull { it.speed() }
        if (speedRules.isNotEmpty()) {
            return speedRules.joinToString(separator = "|")
        }
        return "default:${tool.defaultMiningSpeed()}"
    }

    private fun matchingToolPrototype(item: ItemStack): Tool? {
        val materialName = item.type.name
        val templateMaterial = toolTypeBySuffix.entries.firstOrNull { materialName.endsWith(it.key) }?.value
        return templateMaterial?.let { toolPrototype(it) }
    }

    private fun speedRuleTemplates(): List<Tool.Rule> {
        val templates = listOf(
            Material.WOODEN_AXE,
            Material.WOODEN_HOE,
            Material.WOODEN_PICKAXE,
            Material.WOODEN_SHOVEL,
            Material.WOODEN_SWORD
        )
        return templates.mapNotNull { material ->
            toolPrototype(material)?.rules()?.firstOrNull { it.speed() != null }
        }
    }

    private fun toolPrototype(material: Material): Tool? =
        material.getDefaultData(DataComponentTypes.TOOL)
}
