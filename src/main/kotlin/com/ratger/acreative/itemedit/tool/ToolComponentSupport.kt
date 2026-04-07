@file:Suppress("UnstableApiUsage") // Experimental Tool

package com.ratger.acreative.itemedit.tool

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.Tool
import org.bukkit.inventory.ItemStack

object ToolComponentSupport {

    fun explicitSnapshot(item: ItemStack): Tool? = item.getData(DataComponentTypes.TOOL)

    fun prototypeSnapshot(item: ItemStack): Tool? = item.type.getDefaultData(DataComponentTypes.TOOL)

    fun resolvedSnapshot(item: ItemStack): Tool? = explicitSnapshot(item) ?: prototypeSnapshot(item)

    fun supportsToolEditing(item: ItemStack): Boolean = resolvedSnapshot(item) != null

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
        val base = explicitSnapshot(item) ?: prototypeSnapshot(item) ?: return false
        val hasRules = base.rules().isNotEmpty()
        val rebuiltRules = if (hasRules) {
            base.rules().map { rule ->
                Tool.rule(rule.blocks(), value, rule.correctForDrops())
            }
        } else {
            base.rules()
        }
        val rebuilt = Tool.tool()
            .defaultMiningSpeed(if (hasRules) base.defaultMiningSpeed() else value)
            .damagePerBlock(base.damagePerBlock())
            .addRules(rebuiltRules)
            .build()
        item.setData(DataComponentTypes.TOOL, rebuilt)
        normalizeAfterMutation(item)
        return true
    }

    fun setDamagePerBlock(item: ItemStack, value: Int): Boolean {
        val base = explicitSnapshot(item) ?: prototypeSnapshot(item) ?: return false
        val rebuilt = Tool.tool()
            .defaultMiningSpeed(base.defaultMiningSpeed())
            .damagePerBlock(value)
            .addRules(base.rules())
            .build()
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
}
