package com.ratger.acreative.commands.sit

import org.bukkit.block.Block
import org.bukkit.entity.Player

class SitSessionRegistry {
    private val sessions: MutableMap<Player, SitSession> = mutableMapOf()

    fun set(player: Player, session: SitSession) {
        sessions[player] = session
    }

    fun get(player: Player): SitSession? = sessions[player]

    fun remove(player: Player): SitSession? = sessions.remove(player)

    fun isSitting(player: Player): Boolean = sessions.containsKey(player)

    fun players(): Set<Player> = sessions.keys

    fun entries(): Set<Map.Entry<Player, SitSession>> = sessions.entries

    fun values(): Collection<SitSession> = sessions.values

    fun byBlock(block: Block): Map<Player, SitSession> = sessions.filterValues { it.block == block }
}
