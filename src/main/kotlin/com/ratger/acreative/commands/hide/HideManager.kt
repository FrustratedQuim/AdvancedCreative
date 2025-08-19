package com.ratger.acreative.commands.hide

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class HideManager(private val hooker: FunctionHooker) {

    val hiddenPlayers = ConcurrentHashMap<UUID, MutableSet<UUID>>()
    private val notificationCooldowns = ConcurrentHashMap<UUID, Long>()
    private val hideDuration: Long
        get() = hooker.configManager.config.getLong("hideState", 1800000L)
    private val notifyCooldown = 60 * 1000L

    fun prepareToHidePlayer(hider: Player, targetName: String?) {
        if (targetName == null) {
            hooker.messageManager.sendMiniMessage(hider, key = "usage-hide")
            return
        }
        val target = Bukkit.getPlayer(targetName)
        if (target == null) {
            hooker.messageManager.sendMiniMessage(hider, key = "error-unknown-player")
            return
        }
        hidePlayer(hider, target)
    }

    fun hidePlayer(hider: Player, target: Player) {
        if (hider == target) {
            hooker.messageManager.sendMiniMessage(hider, key = "error-hide-self")
            return
        }
        if (target.hasPermission("advancedcreative.hide.bypass")) {
            hooker.messageManager.sendMiniMessage(hider, key = "error-hide-bypass")
            return
        }

        val hiderId = hider.uniqueId
        val targetId = target.uniqueId
        val hiddenSet = hiddenPlayers.computeIfAbsent(hiderId) { mutableSetOf() }

        if (hiddenSet.contains(targetId)) {
            unhidePlayer(hider, target)
            return
        }

        hiddenSet.add(targetId)
        hider.hidePlayer(hooker.plugin, target)
        hideFreezeBlocks(hider, target)
        hidePuddleDisplays(hider, target)

        if (hooker.utils.isDisguised(target)) {
            hooker.disguiseManager.disguisedPlayers[target]?.let { (entityId, _, _) ->
                val protocolManager = ProtocolLibrary.getProtocolManager()
                val destroyPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY)
                destroyPacket.intLists.write(0, listOf(entityId))
                try {
                    protocolManager.sendServerPacket(hider, destroyPacket)
                    hooker.plugin.logger.info("Sent ENTITY_DESTROY packet for ${target.name}'s disguise to ${hider.name}")
                } catch (e: Exception) {
                    hooker.plugin.logger.warning("Failed to send ENTITY_DESTROY packet for ${target.name}'s disguise to ${hider.name}: ${e.message}")
                }
            }
        }

        hooker.messageManager.sendMiniMessage(
            hider,
            key = "success-hide",
            variables = mapOf("target" to target.name)
        )

        if (notificationCooldowns.getOrDefault(targetId, 0L) + notifyCooldown < System.currentTimeMillis()) {
            hooker.messageManager.sendMiniMessage(
                target,
                key = "notify-hide",
                variables = mapOf("hider" to hider.name)
            )
            notificationCooldowns[targetId] = System.currentTimeMillis()
        }

        Bukkit.getScheduler().runTaskLater(hooker.plugin, Runnable {
            if (hiddenSet.contains(targetId)) {
                hiddenSet.remove(targetId)
                if (hiddenSet.isEmpty()) {
                    hiddenPlayers.remove(hiderId)
                }
                if (hider.isOnline && target.isOnline) {
                    hider.showPlayer(hooker.plugin, target)
                    hooker.messageManager.sendMiniMessage(
                        hider,
                        key = "success-hide-removed",
                        variables = mapOf("target" to target.name)
                    )
                    unhidePlayer(hider, target)
                }
            }
        }, hideDuration / 50L)
    }

    fun reapplyHide(hider: Player, target: Player) {
        hider.hidePlayer(hooker.plugin, target)
        hideFreezeBlocks(hider, target)
        hidePuddleDisplays(hider, target)
    }

    fun hideEntity(hider: Player, entity: Entity) {
        if (entity is Projectile) {
            val owner = (entity.shooter as? Player)?.uniqueId
            if (owner != null && hiddenPlayers[hider.uniqueId]?.contains(owner) == true) {
                hider.hideEntity(hooker.plugin, entity)
            }
        }
    }

    fun hideDroppedItem(hider: Player, itemEntity: Entity, owner: Player) {
        if (hiddenPlayers[hider.uniqueId]?.contains(owner.uniqueId) == true) {
            hider.hideEntity(hooker.plugin, itemEntity)
        }
    }

    private fun hideFreezeBlocks(hider: Player, target: Player) {
        val blocks = hooker.freezeManager.frozenPlayers[target]
        if (blocks != null) {
            blocks.forEach { hider.hideEntity(hooker.plugin, it) }
            val hiddenBlocks = hooker.freezeManager.hiddenFreezeBlocks.computeIfAbsent(hider.uniqueId) { ConcurrentHashMap() }
            hiddenBlocks[target.uniqueId] = blocks
        }
    }

    private fun showFreezeBlocks(hider: Player, target: Player) {
        val blocks = hooker.freezeManager.frozenPlayers[target]
        blocks?.forEach { block ->
            if (block.isValid) {
                hider.showEntity(hooker.plugin, block)
            }
        }
        hooker.freezeManager.hiddenFreezeBlocks[hider.uniqueId]?.remove(target.uniqueId)
        if (hooker.freezeManager.hiddenFreezeBlocks[hider.uniqueId]?.isEmpty() == true) {
            hooker.freezeManager.hiddenFreezeBlocks.remove(hider.uniqueId)
        }
    }

    private fun hidePuddleDisplays(hider: Player, target: Player) {
        hooker.pissManager.scorePoints.filter { it.creator == target.uniqueId && it.display != null }.forEach { point ->
            val display = point.display!!
            hider.hideEntity(hooker.plugin, display)
            val hiddenMap = hooker.pissManager.hiddenPuddleDisplays.computeIfAbsent(hider.uniqueId) { ConcurrentHashMap() }
            val list = hiddenMap.computeIfAbsent(target.uniqueId) { mutableListOf() }
            if (!list.contains(display)) {
                list.add(display)
            }
        }
    }

    private fun showPuddleDisplays(hider: Player, target: Player) {
        val hiddenMap = hooker.pissManager.hiddenPuddleDisplays[hider.uniqueId] ?: return
        val list = hiddenMap[target.uniqueId] ?: return
        list.forEach { display ->
            if (display.isValid) {
                hider.showEntity(hooker.plugin, display)
            }
        }
        hiddenMap.remove(target.uniqueId)
        if (hiddenMap.isEmpty()) {
            hooker.pissManager.hiddenPuddleDisplays.remove(hider.uniqueId)
        }
    }

    private fun unhidePlayer(hider: Player, target: Player) {
        val hiderId = hider.uniqueId
        val targetId = target.uniqueId
        val hiddenSet = hiddenPlayers[hiderId] ?: return

        if (hider.isOnline && target.isOnline) {
            hiddenSet.remove(targetId)
            hider.showPlayer(hooker.plugin, target)
            showFreezeBlocks(hider, target)
            showPuddleDisplays(hider, target)

            if (hooker.utils.isDisguised(target)) {
                hooker.plugin.logger.info("Scheduling disguise update for ${target.name} to ${hider.name}")
                Bukkit.getScheduler().runTaskLater(hooker.plugin, Runnable {
                    if (!hider.isOnline || !target.isOnline) {
                        hooker.plugin.logger.info("Skipped disguise update for ${target.name} to ${hider.name}: one of the players is offline")
                        return@Runnable
                    }
                    hooker.disguiseManager.updateDisguiseForPlayer(target, hider)
                }, 3L)
            }

            hooker.messageManager.sendMiniMessage(
                hider,
                key = "success-hide-removed",
                variables = mapOf("target" to target.name)
            )
            if (hiddenSet.isEmpty()) {
                hiddenPlayers.remove(hiderId)
            }
        }
    }
}