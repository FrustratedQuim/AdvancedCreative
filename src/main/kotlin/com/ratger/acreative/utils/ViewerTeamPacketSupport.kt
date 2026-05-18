package com.ratger.acreative.utils

import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket
import net.minecraft.world.scores.PlayerTeam
import net.minecraft.world.scores.Scoreboard
import net.minecraft.world.scores.Team
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player

object ViewerTeamPacketSupport {

    data class Definition(
        val teamName: String,
        val entry: String,
        val prefix: net.minecraft.network.chat.Component = net.minecraft.network.chat.Component.empty(),
        val suffix: net.minecraft.network.chat.Component = net.minecraft.network.chat.Component.empty(),
        val nameTagVisibility: Team.Visibility = Team.Visibility.NEVER,
        val collisionRule: Team.CollisionRule = Team.CollisionRule.NEVER
    ) {
        companion object {
            fun hidden(teamName: String, entry: String): Definition = Definition(
                teamName = teamName,
                entry = entry
            )
        }
    }

    fun sendCreate(viewer: Player, definition: Definition) {
        val scoreboard = Scoreboard()
        val team = PlayerTeam(scoreboard, definition.teamName)
        team.playerPrefix = definition.prefix
        team.playerSuffix = definition.suffix
        team.nameTagVisibility = definition.nameTagVisibility
        team.collisionRule = definition.collisionRule
        team.players.add(definition.entry)

        val packet = ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, true)
        (viewer as CraftPlayer).handle.connection.send(packet)
    }

    fun sendRemove(viewer: Player, teamName: String) {
        val scoreboard = Scoreboard()
        val team = PlayerTeam(scoreboard, teamName)
        val packet = ClientboundSetPlayerTeamPacket.createRemovePacket(team)
        (viewer as CraftPlayer).handle.connection.send(packet)
    }
}
