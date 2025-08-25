package com.ratger.acreative.utils

import com.ratger.acreative.core.FunctionHooker
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.block.data.type.Bed
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityToggleGlideEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.*
import org.bukkit.inventory.EquipmentSlot
import kotlin.math.roundToInt

class EventHandler(val hooker: FunctionHooker) : Listener {

    private val utils = hooker.utils
    private val sitManager = hooker.sitManager
    private val layManager = hooker.layManager
    private val hideManager = hooker.hideManager
    private val bindManager = hooker.bindManager

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        utils.unsetAllPoses(player, true)
        utils.unsetAllStates(player)
        utils.checkBindClear(player)
        utils.checkGlowDisable(player)
        utils.checkPissStop(player)
        utils.checkDisguiseDisable(player)
        utils.checkCustomEffectsDisable(player)
        utils.checkSlapUnslap(player)
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.player
        utils.unsetAllPoses(player, true)
        utils.unsetAllStates(player)
        utils.checkPissStop(player)
        utils.checkDisguiseDisable(player)
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onGameModeChange(event: PlayerGameModeChangeEvent) {
        if (event.newGameMode == GameMode.SPECTATOR) {
            val player = event.player
            utils.unsetAllPoses(player, true)
            utils.checkPissStop(player)
            utils.checkDisguiseDisable(player)
            val sitData = sitManager.sittingMap[player]
            if (sitData != null && sitData.style == "head") {
                sitManager.unsitPlayer(player)
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerToggleSneak(event: PlayerToggleSneakEvent) {
        if (event.isSneaking) {
            utils.unsetAllPoses(event.player)
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerTeleport(event: PlayerTeleportEvent) {
        val player = event.player
        val destination = event.to
        val world = destination.world ?: return
        val occupiedPlayers = (sitManager.sittingMap.keys + layManager.layingMap.keys).distinct()

        for (other in occupiedPlayers) {
            if (other.world == world && other.location.distanceSquared(destination) <= 1.0) {
                event.to = destination.clone().apply { y = y.roundToInt().toDouble() + 1.1 }
                break
            }
        }

        if (utils.isDisguised(player)) {
            hooker.disguiseManager.recreateDisguise(player, event.to)
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val player = event.player

        if (utils.isFrozen(player)) {
            event.isCancelled = true
            return
        }

        if (utils.isSitting(player) || utils.isLaying(player) || utils.isDisguised(player)) {
            val entityType = event.rightClicked.type
            if (entityType != EntityType.ITEM_FRAME && entityType != EntityType.ARMOR_STAND) {
                event.isCancelled = true
                return
            }
        }

        if (event.rightClicked is Player && player.inventory.itemInMainHand.type == Material.AIR) {
            val target = event.rightClicked as Player
            if (target.gameMode == GameMode.SPECTATOR) {
                event.isCancelled = true
                return
            }
            if (player.hasPermission("advancedcreative.sit.head")) {
                sitManager.sitOnHead(player, target)
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player

        if (event.action.isClickAction()) {
            bindManager.executeBind(player)
        }

        if (utils.isFrozen(player)) {
            event.isCancelled = true
            return
        }

        if (event.action == Action.LEFT_CLICK_AIR) {
            val headPassenger = sitManager.getHeadPassenger(player)
            if (headPassenger != null) {
                sitManager.launchHeadPassenger(player)
                return
            }
        }

        handleRightClickBlock(event, player)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val block = event.block

        if (utils.isFrozen(player) || utils.isBarrierAboveCrawlingPlayer(block)) {
            event.isCancelled = true
            return
        }

        if (
            block.type.name.contains("STAIRS") ||
            block.type.name.contains("SLAB") ||
            block.type.name.contains("BED")
        ) {
            utils.checkSitUnsit(player)
            utils.checkLayingUnlay(player)
            sitManager.handleBlockBreak(block)
            if (block.type.name.contains("BED")) {
                val bedData = block.blockData as? Bed
                val headBlock = if (bedData?.part == Bed.Part.HEAD) {
                    block
                } else {
                    val facing = bedData?.facing ?: return
                    val headLocation = block.location.clone()
                    when (facing.name) {
                        "WEST" -> headLocation.add(-1.0, 0.0, 0.0)
                        "SOUTH" -> headLocation.add(0.0, 0.0, 1.0)
                        "NORTH" -> headLocation.add(0.0, 0.0, -1.0)
                        "EAST" -> headLocation.add(1.0, 0.0, 0.0)
                    }
                    headLocation.block
                }
                layManager.layingMap.entries
                    .filter { it.value.bedLocation != null && it.value.bedLocation == headBlock.location }
                    .forEach { layManager.unlayPlayer(it.key) }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onEntityToggleGlide(event: EntityToggleGlideEvent) {
        val player = event.entity as? Player ?: return
        if (utils.isGliding(player) && !event.isGliding) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerToggleFlight(event: PlayerToggleFlightEvent) {
        val player = event.player
        utils.checkCrawlUncrawl(player)
        utils.checkLayingUnlay(player)
        utils.checkSitUnsit(player)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        val player = event.player

        if (utils.isFrozen(player)) {
            event.isCancelled = true
            return
        }

        val droppedItem = event.itemDrop
        utils.getPlayersWithHides().forEach { hideManager.hideDroppedItem(it, droppedItem, player) }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerPickupItem(event: PlayerAttemptPickupItemEvent) {
        val player = event.player
        if (utils.isFrozen(player)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onProjectileLaunch(event: ProjectileLaunchEvent) {
        val projectile = event.entity
        utils.getPlayersWithHides().forEach { hideManager.hideEntity(it, projectile) }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val joiningPlayer = event.player
        hideManager.reapplyAllHides(joiningPlayer)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as Player
        if (utils.isLaying(player) || utils.isDisguised(player)) {
            if (event.slotType == InventoryType.SlotType.ARMOR || event.slot == 40) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerItemHeld(event: PlayerItemHeldEvent) {
        val player = event.player
        if (utils.isLaying(player)) hooker.playerStateManager.handleItemSwitch(player, event.newSlot)
        if (utils.isDisguised(player)) {
            hooker.playerStateManager.handleItemSwitch(player, event.newSlot)
            hooker.disguiseManager.updateMainHandEquipment(player)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        if (utils.isFrozen(player) && event.from != event.to) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        hooker.glowManager.refreshGlow(event.player)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onFallDamage(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return
        if (hooker.slapManager.fallProtectedPlayers.contains(player.uniqueId) &&
            event.cause == EntityDamageEvent.DamageCause.FALL
        ) {
            event.isCancelled = true
        }
    }

    private fun handleRightClickBlock(event: PlayerInteractEvent, player: Player) {
        if (event.action != Action.RIGHT_CLICK_BLOCK || event.hand != EquipmentSlot.HAND) return
        if (player.gameMode == GameMode.SPECTATOR || player.inventory.itemInMainHand.type != Material.AIR) return

        val block = event.clickedBlock ?: return
        val aboveBlock = block.location.clone().add(0.0, 1.0, 0.0).block
        if (aboveBlock.type.isSolid) return

        val handled = (player.hasPermission("advancedcreative.sit") && sitManager.handleRightClickBlock(player, block)) ||
                (player.hasPermission("advancedcreative.lay") && layManager.handleRightClickBlock(player, block))

        if (handled) {
            event.isCancelled = true
        }
    }

    private fun Action.isClickAction(): Boolean {
        return this == Action.LEFT_CLICK_AIR || this == Action.LEFT_CLICK_BLOCK ||
                this == Action.RIGHT_CLICK_AIR || this == Action.RIGHT_CLICK_BLOCK
    }
}