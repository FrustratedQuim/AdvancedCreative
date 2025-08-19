package com.ratger.acreative.utils

import com.ratger.acreative.core.FunctionHooker
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.*
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.*
import org.bukkit.inventory.EquipmentSlot
import kotlin.math.roundToInt

class EventHandler(val hooker: FunctionHooker) : Listener {

    private val utils = hooker.utils
    private val sitManager = hooker.sitManager
    private val layManager = hooker.layManager
    private val hideManager = hooker.hideManager
    private val bindManager = hooker.bindManager

    private val interactableBlocks = listOf(
        "LEVER", "BUTTON", "REPEATER", "COMPARATOR",
        "GATE", "DOOR", "TRAPDOOR", "BELL", "SIGN",
        "ANCHOR", "TNT", "REDSTONE"
    )

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        utils.unsetAllPoses(player)
        utils.unsetAllStates(player)
        utils.checkBindClear(player)
        utils.checkGlowDisable(player)
        utils.checkPissStop(player)
        utils.checkDisguiseDisable(player)
        utils.checkCustomEffectsDisable(player)
        utils.checkSlapUnslap(player)
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.player
        utils.unsetAllPoses(player)
        utils.unsetAllStates(player)
        utils.checkPissStop(player)
        utils.checkDisguiseDisable(player)
    }

    @EventHandler
    fun onGameModeChange(event: PlayerGameModeChangeEvent) {
        if (event.newGameMode == GameMode.SPECTATOR) {
            val player = event.player
            utils.unsetAllPoses(player)
            utils.checkPissStop(player)
            utils.checkDisguiseDisable(player)
        }
    }

    @EventHandler
    fun onPlayerToggleSneak(event: PlayerToggleSneakEvent) {
        if (event.isSneaking) {
            utils.unsetAllPoses(event.player)
        }
    }

    @EventHandler
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

    @EventHandler
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val player = event.player

        if (utils.isFrozen(player)) {
            event.isCancelled = true
            return
        }

        if (utils.isSitting(player) || utils.isLaying(player)) {
            val entityType = event.rightClicked.type
            if (entityType != EntityType.ITEM_FRAME && entityType != EntityType.ARMOR_STAND) {
                event.isCancelled = true
                return
            }
        }

        if (event.rightClicked is Player && utils.isSlapping(player)) {
            hooker.slapManager.applySlap(player, event.rightClicked as Player)
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val damager = event.damager as? Player ?: return
        val target = event.entity as? Player ?: return

        if (utils.isSlapping(damager)) {
            hooker.slapManager.applySlap(damager, target)
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player

        if (utils.isDisguised(player)) {
            hooker.disguiseManager.sendSwingAnimation(player)
        }

        if (event.action.isClickAction()) {
            bindManager.executeBind(player)
        }

        if (shouldCancelInteraction(event, player)) {
            event.isCancelled = true
            return
        }

        handleRightClickBlock(event, player)
    }

    @EventHandler
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
            layManager.layingMap.entries
                .filter { it.value.bedY == block.location.y }
                .forEach { layManager.unlayPlayer(it.key) }
        }

    }

    @EventHandler
    fun onEntityToggleGlide(event: EntityToggleGlideEvent) {
        val player = event.entity as? Player ?: return
        if (utils.isGliding(player) && !event.isGliding) event.isCancelled = true
    }

    @EventHandler
    fun onPlayerToggleFlight(event: PlayerToggleFlightEvent) {
        val player = event.player
        utils.checkCrawlUncrawl(player)
        utils.checkLayingUnlay(player)
        utils.checkSitUnsit(player)
    }

    @EventHandler
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        val player = event.player

        if (utils.isFrozen(player) || utils.isLaying(player) || utils.isDisguised(player)) {
            event.isCancelled = true
            return
        }

        val droppedItem = event.itemDrop
        utils.getPlayersWithHides().forEach { hideManager.hideDroppedItem(it, droppedItem, player) }
    }

    @EventHandler
    fun onPlayerPickupItem(event: PlayerAttemptPickupItemEvent) {
        val player = event.player
        if (utils.isFrozen(player) || utils.isLaying(player) || utils.isDisguised(player)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onProjectileLaunch(event: ProjectileLaunchEvent) {
        val projectile = event.entity
        utils.getPlayersWithHides().forEach { hideManager.hideEntity(it, projectile) }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val joiningPlayer = event.player
        utils.getPlayersWithHides().forEach {
            if (utils.isHiddenFromPlayer(it, joiningPlayer)) hideManager.reapplyHide(it, joiningPlayer)
        }
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as Player
        if (utils.isLaying(player) || utils.isDisguised(player)) event.isCancelled = true
    }

    @EventHandler
    fun onPlayerItemHeld(event: PlayerItemHeldEvent) {
        val player = event.player
        if (utils.isLaying(player)) hooker.playerStateManager.handleItemSwitch(player, event.newSlot)
        if (utils.isDisguised(player)) {
            hooker.playerStateManager.handleItemSwitch(player, event.newSlot)
            hooker.disguiseManager.updateMainHandEquipment(player)
        }
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        if (utils.isFrozen(player) && event.from != event.to) event.isCancelled = true
    }

    @EventHandler
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        hooker.glowManager.refreshGlow(event.player)
    }

    @EventHandler
    fun onFallDamage(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return
        if (hooker.slapManager.fallProtectedPlayers.contains(player.uniqueId) &&
            event.cause == EntityDamageEvent.DamageCause.FALL
        ) {
            event.isCancelled = true
        }
    }

    private fun shouldCancelInteraction(event: PlayerInteractEvent, player: Player): Boolean {
        if (utils.isFrozen(player) || utils.isLaying(player) || utils.isDisguised(player)) {
            val block = event.clickedBlock
            return !((utils.isLaying(player) || utils.isDisguised(player)) && block != null &&
                    interactableBlocks.any { block.type.name.contains(it) })
        }
        return false
    }

    private fun handleRightClickBlock(event: PlayerInteractEvent, player: Player) {
        if (event.action != Action.RIGHT_CLICK_BLOCK || event.hand != EquipmentSlot.HAND) return
        if (player.gameMode == GameMode.SPECTATOR || player.inventory.itemInMainHand.type != Material.AIR) return

        val block = event.clickedBlock ?: return
        val aboveBlock = block.location.clone().add(0.0, 1.0, 0.0).block
        if (aboveBlock.type.isSolid) return

        if (sitManager.handleRightClickBlock(player, block) || layManager.handleRightClickBlock(player, block)) {
            event.isCancelled = true
        }
    }

    private fun Action.isClickAction(): Boolean {
        return this == Action.LEFT_CLICK_AIR || this == Action.LEFT_CLICK_BLOCK ||
                this == Action.RIGHT_CLICK_AIR || this == Action.RIGHT_CLICK_BLOCK
    }
}
