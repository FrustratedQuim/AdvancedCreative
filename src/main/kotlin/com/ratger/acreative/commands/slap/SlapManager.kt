package com.ratger.acreative.commands.slap

import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.MessageKey
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import java.util.*

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
            hooker.messageManager.sendChat(player, MessageKey.INFO_SLAP_ON)
        }
    }

    fun unslapPlayer(player: Player) {
        slappingPlayers.remove(player)
        hooker.messageManager.sendChat(player, MessageKey.INFO_SLAP_OFF)
    }

    fun applySlap(attacker: Player, target: Player) {
        if (target.hasPermission("advancedcreative.slap.bypass")) return
        if (cooldownPlayers.contains(target.uniqueId)) return

        cooldownPlayers.add(target.uniqueId)

        hooker.utils.unsetAllPoses(target)
        target.leaveVehicle()

        hooker.tickScheduler.runLater(2L) {
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

            // Protect from fall damage for a short duration after slap
            fallProtectedPlayers.add(target.uniqueId)

            hooker.tickScheduler.runLater(100L) {
                fallProtectedPlayers.remove(target.uniqueId)
            }

            hooker.tickScheduler.runLater(20L) {
                cooldownPlayers.remove(target.uniqueId)
            }
        }
    }
}
