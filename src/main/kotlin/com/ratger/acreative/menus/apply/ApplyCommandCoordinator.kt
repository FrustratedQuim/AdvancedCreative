package com.ratger.acreative.menus.apply

import org.bukkit.entity.Player

interface ApplyCommandTarget {
    fun isWaiting(player: Player): Boolean
    fun handle(player: Player, args: Array<out String>): Boolean
    fun tabComplete(player: Player, args: Array<out String>): List<String>
    fun cancel(player: Player)
}

class ApplyCommandCoordinator(
    private val targets: MutableList<ApplyCommandTarget> = mutableListOf()
) {
    fun registerTarget(target: ApplyCommandTarget) {
        targets += target
    }

    fun isWaiting(player: Player): Boolean = targets.any { it.isWaiting(player) }

    fun activeTarget(player: Player): ApplyCommandTarget? = targets.firstOrNull { it.isWaiting(player) }

    fun handle(player: Player, args: Array<out String>): Boolean {
        val active = activeTarget(player) ?: return false
        return active.handle(player, args)
    }

    fun tabComplete(player: Player, args: Array<out String>): List<String> {
        val active = activeTarget(player) ?: return emptyList()
        return active.tabComplete(player, args)
    }

    fun cancel(player: Player) {
        targets.forEach { it.cancel(player) }
    }
}
