package com.ratger.acreative.commands.glow

import com.ratger.acreative.core.FunctionHooker
import org.bukkit.entity.Player

class GlowManager(private val hooker: FunctionHooker) {

    val glowingPlayers = mutableSetOf<Player>()

    fun glowPlayer(player: Player) {
        if (glowingPlayers.contains(player)) {
            removeGlow(player)
        } else {
            applyGlow(player)
        }
    }

    fun applyGlow(player: Player) {
        glowingPlayers.add(player)

        if (hooker.utils.isLaying(player)) {
            hooker.layManager.updateNpcGlowing(player, isGlowing = true)
        } else if (hooker.utils.isDisguised(player)) {
            hooker.disguiseManager.updateEntityGlowing(player, isGlowing = true)
        } else {
            player.isGlowing = true
        }

        hooker.freezeManager.updateIceGlowing(player, isGlowing = true)
        hooker.messageManager.sendMiniMessage(player, key = "info-glow-on")
    }

    fun removeGlow(player: Player) {
        glowingPlayers.remove(player)

        if (hooker.utils.isLaying(player)) {
            hooker.layManager.updateNpcGlowing(player, isGlowing = false)
        } else if (hooker.utils.isDisguised(player)) {
            hooker.disguiseManager.updateEntityGlowing(player, isGlowing = false)
        }

        player.isGlowing = false
        hooker.freezeManager.updateIceGlowing(player, isGlowing = false)
        hooker.messageManager.sendMiniMessage(player, key = "info-glow-off")
    }

    fun refreshGlow(player: Player) {
        if (glowingPlayers.contains(player)) {
            player.isGlowing = true
        }
    }
}
