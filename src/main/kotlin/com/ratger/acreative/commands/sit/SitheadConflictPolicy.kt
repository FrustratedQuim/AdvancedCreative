package com.ratger.acreative.commands.sit

import com.ratger.acreative.core.FunctionHooker
import org.bukkit.entity.Player

internal class SitheadConflictPolicy(hooker: FunctionHooker) {

    private val rules: List<SitheadConflictRule> = listOf(
        JarredTargetConflictRule(hooker)
    )

    fun hasConflict(rider: Player, target: Player): Boolean {
        return rules.any { it.hasConflict(rider, target) }
    }
}

private fun interface SitheadConflictRule {
    fun hasConflict(rider: Player, target: Player): Boolean
}

private class JarredTargetConflictRule(private val hooker: FunctionHooker) : SitheadConflictRule {
    override fun hasConflict(rider: Player, target: Player): Boolean {
        return hooker.jarManagerOrNull().let { it != null && it.isJarred(target) }
    }
}
