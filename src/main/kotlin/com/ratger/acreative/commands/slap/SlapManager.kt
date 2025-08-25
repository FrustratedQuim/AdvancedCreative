package com.ratger.acreative.commands.slap

import com.ratger.acreative.core.FunctionHooker
import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import java.util.UUID

class SlapManager(private val hooker: FunctionHooker) : Listener {

    val slappingPlayers = mutableSetOf<Player>()
    val fallProtectedPlayers = mutableSetOf<UUID>()
    private val cooldownPlayers = mutableSetOf<UUID>()

    init {
        hooker.plugin.server.pluginManager.registerEvents(this, hooker.plugin)
    }

    fun slapPlayer(player: Player) {
        if (slappingPlayers.contains(player)) {
            unslapPlayer(player)
        } else {
            slappingPlayers.add(player)
            hooker.messageManager.sendMiniMessage(player, key = "info-slap-on")
        }
    }

    fun unslapPlayer(player: Player) {
        slappingPlayers.remove(player)
        hooker.messageManager.sendMiniMessage(player, key = "info-slap-off")
    }

    fun applySlap(attacker: Player, target: Player) {
        if (target.hasPermission("advancedcreative.slap.bypass")) return
        if (cooldownPlayers.contains(target.uniqueId)) return

        cooldownPlayers.add(target.uniqueId)

        hooker.utils.unsetAllPoses(target)
        target.leaveVehicle()

        Bukkit.getScheduler().runTaskLater(hooker.plugin, Runnable {
            val location = target.location.clone().add(0.0, 1.5, 0.0)
            target.world.spawnParticle(Particle.FLASH, location, 1)

            target.addPotionEffect(
                PotionEffect(
                    PotionEffectType.BLINDNESS,
                    20,
                    0,
                    false,
                    false
                )
            )

            val direction = attacker.location.direction.normalize()
            val velocity = Vector(direction.x * 2.5, 1.0, direction.z * 2.5)
            target.velocity = velocity

            Bukkit.getScheduler().runTaskLater(hooker.plugin, Runnable {
                fallProtectedPlayers.remove(target.uniqueId)
            }, 100L)

            Bukkit.getScheduler().runTaskLater(hooker.plugin, Runnable {
                cooldownPlayers.remove(target.uniqueId)
            }, 20L)
        }, 2L)
    }
}