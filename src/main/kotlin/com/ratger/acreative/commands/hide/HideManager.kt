package com.ratger.acreative.commands.hide

import com.ratger.acreative.core.FunctionHooker
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import java.util.*
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

        val hiderPlayersToUnsit = mutableListOf<Player>()
        if (hooker.utils.isSitting(hider)) {
            var current = hider
            val checkedPlayers = mutableSetOf<Player>()
            var depth = 0
            val maxDepth = 10
            while (depth < maxDepth) {
                if (current in checkedPlayers) break
                checkedPlayers.add(current)
                hiderPlayersToUnsit.add(current)
                val passenger = hooker.sitManager.getHeadPassenger(current)
                if (passenger == null) break
                current = passenger
                depth++
            }

            hiderPlayersToUnsit.reversed().forEach { player ->
                hooker.sitManager.unsitPlayer(player)
            }
        }

        val targetChain = mutableListOf<Player>()
        var current = target
        val checkedPlayers = mutableSetOf<Player>()
        var depth = 0
        val maxDepth = 10
        while (depth < maxDepth) {
            if (current in checkedPlayers) {
                hooker.messageManager.sendMiniMessage(hider, key = "error-hide-failed")
                return
            }
            checkedPlayers.add(current)
            targetChain.add(current)
            val passenger = hooker.sitManager.getHeadPassenger(current)
            if (passenger == null) break
            current = passenger
            depth++
        }
        if (depth >= maxDepth) {
            hooker.messageManager.sendMiniMessage(hider, key = "error-hide-failed")
            return
        }

        var basePlayer = target
        val baseCheckedPlayers = mutableSetOf<Player>()
        depth = 0
        while (depth < maxDepth) {
            if (basePlayer in baseCheckedPlayers) {
                hooker.messageManager.sendMiniMessage(hider, key = "error-hide-failed")
                return
            }
            baseCheckedPlayers.add(basePlayer)
            if (hider == basePlayer) {
                targetChain.reversed().forEach { player ->
                    if (hooker.utils.isSitting(player)) {
                        hooker.sitManager.unsitPlayer(player)
                    }
                }
                break
            }
            val sitData = hooker.sitManager.sittingMap[basePlayer]
            if (sitData == null || sitData.style != "head") break
            val armorStand = basePlayer.world.getEntity(sitData.armorStandId) ?: break
            val vehicle = armorStand.vehicle as? Player ?: break
            basePlayer = vehicle
            depth++
        }

        val hiderId = hider.uniqueId
        val targetId = target.uniqueId
        val hiddenSet = hiddenPlayers.computeIfAbsent(hiderId) { mutableSetOf() }

        if (hiddenSet.contains(targetId)) {
            unhidePlayer(hider, target)
            return
        }

        hiddenSet.add(targetId)
        Bukkit.getScheduler().runTaskLater(hooker.plugin, Runnable {
            if (hider.isOnline && target.isOnline) {
                hider.hidePlayer(hooker.plugin, target)
                hideFreezeBlocks(hider, target)
                hidePuddleDisplays(hider, target)
            }
        }, 1L)

        if (hooker.utils.isDisguised(target)) {
            hooker.disguiseManager.disguisedPlayers[target]?.entity?.removeViewer(hider.uniqueId)
        }

        if (hooker.utils.isLaying(target)) {
            hooker.layManager.layingMap[target]?.let { layData ->
                layData.npc.removeViewer(hider.uniqueId)
                val armorStand = target.world.getEntity(layData.armorStandId)
                if (armorStand != null) {
                    hider.hideEntity(hooker.plugin, armorStand)
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
        if (hooker.utils.isLaying(target)) {
            hooker.layManager.layingMap[target]?.let { layData ->
                layData.npc.removeViewer(hider.uniqueId)
                val armorStand = target.world.getEntity(layData.armorStandId)
                if (armorStand != null) {
                    hider.hideEntity(hooker.plugin, armorStand)
                }
            }
        }
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
            blocks.forEach { it.removeViewer(hider.uniqueId) }
            val hiddenBlocks = hooker.freezeManager.hiddenFreezeBlocks.computeIfAbsent(hider.uniqueId) { ConcurrentHashMap() }
            hiddenBlocks[target.uniqueId] = blocks
        }
    }

    private fun showFreezeBlocks(hider: Player, target: Player) {
        val blocks = hooker.freezeManager.frozenPlayers[target]
        blocks?.forEach { block ->
            if (block.isSpawned) {
                block.addViewer(hider.uniqueId)
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
            display.removeViewer(hider.uniqueId)
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
            if (display.isSpawned) {
                display.addViewer(hider.uniqueId)
            }
        }
        hiddenMap.remove(target.uniqueId)
        if (hiddenMap.isEmpty()) {
            hooker.pissManager.hiddenPuddleDisplays.remove(hider.uniqueId)
        }
    }

    fun unhidePlayer(hider: Player, target: Player) {
        val hiderId = hider.uniqueId
        val targetId = target.uniqueId
        val hiddenSet = hiddenPlayers[hiderId] ?: return

        if (hider.isOnline && target.isOnline) {
            hiddenSet.remove(targetId)
            hider.showPlayer(hooker.plugin, target)
            showFreezeBlocks(hider, target)
            showPuddleDisplays(hider, target)

            if (hooker.utils.isDisguised(target)) {
                Bukkit.getScheduler().runTaskLater(hooker.plugin, Runnable {
                    if (!hider.isOnline || !target.isOnline) {
                        return@Runnable
                    }
                    hooker.disguiseManager.updateDisguiseForPlayer(target, hider)
                }, 3L)
            }

            if (hooker.utils.isLaying(target)) {
                hooker.layManager.layingMap[target]?.let { layData ->
                    if (layData.npc.isSpawned) {
                        layData.npc.addViewer(hider.uniqueId)
                        val armorStand = target.world.getEntity(layData.armorStandId)
                        if (armorStand != null && armorStand.isValid) {
                            hider.showEntity(hooker.plugin, armorStand)
                        }
                    }
                }
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

    fun reapplyAllHides(player: Player) {
        val playerId = player.uniqueId

        // Повторно скрываем скрытых игроков
        val hiddenSet = hiddenPlayers[playerId] ?: emptySet()
        hiddenSet.forEach { targetId ->
            val target = Bukkit.getPlayer(targetId)
            if (target != null && target.isOnline) {
                reapplyHide(player, target)
            }
        }

        // Скрываем игрока от тех, кто скрыл его
        hiddenPlayers.forEach { (hiderId, targets) ->
            if (playerId in targets) {
                val hider = Bukkit.getPlayer(hiderId)
                if (hider != null && hider.isOnline) {
                    reapplyHide(hider, player)
                }
            }
        }
    }

}