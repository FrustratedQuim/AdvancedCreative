package com.ratger.acreative.utils

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object PlayerDisplayNameResolver {

    fun resolve(player: Player): Component {
        val team = sequenceOf(
            Bukkit.getScoreboardManager().mainScoreboard,
            player.scoreboard
        )
            .mapNotNull { it.getEntryTeam(player.name) }
            .firstOrNull()

        return team?.let {
            Component.join(JoinConfiguration.noSeparators(), it.prefix(), Component.text(player.name), it.suffix())
        } ?: Component.text(player.name)
    }
}
