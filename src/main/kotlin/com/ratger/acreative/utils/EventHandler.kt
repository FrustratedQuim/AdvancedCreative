package com.ratger.acreative.utils

import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.commands.sit.SitStyle
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.block.data.type.Bed
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.entity.EntityToggleGlideEvent
import org.bukkit.event.entity.EntityToggleSwimEvent
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

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        hooker.grabManager.cleanupSessionsForPlayer(player.uniqueId)
        hooker.jarManager.cleanupSessionsForPlayer(player.uniqueId)
        hooker.disguiseManager.onViewerDisconnect(player.uniqueId)
        utils.unsetAllPoses(player, true)
        utils.unsetAllStates(player)
        utils.checkGlowDisable(player)
        utils.checkPissStop(player)
        utils.checkDisguiseDisable(player)
        utils.checkCustomEffectsDisable(player)
        utils.checkSlapUnslap(player)
        hooker.playerStateManager.clearPlayerStates(player)
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.player
        hooker.grabManager.cleanupSessionsForPlayer(player.uniqueId)
        hooker.jarManager.cleanupSessionsForPlayer(player.uniqueId)
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
            val sitData = sitManager.getSitSession(player)
            if (sitData != null && sitData.style == SitStyle.HEAD) {
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
        val occupiedPlayers = (sitManager.getSittingPlayers() + layManager.layingMap.keys).distinct()

        for (other in occupiedPlayers) {
            if (other.world == world && other.location.distanceSquared(destination) <= 1.0) {
                event.to = destination.clone().apply { y = y.roundToInt().toDouble() + 1.1 }
                break
            }
        }

        utils.checkFreezeUnfreeze(player)
        utils.checkSitUnsit(player)
        utils.checkLayingUnlay(player)
        if (utils.isDisguised(player)) hooker.disguiseManager.recreateDisguise(player, event.to)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val player = event.player

        // 1) Base restrictions that always have top priority.
        if (hooker.grabManager.blockGrabbedEntityInteraction(player)) {
            event.isCancelled = true
            return
        }
        if (hooker.jarManager.blockJarredInteraction(player)) {
            event.isCancelled = true
            return
        }
        if (utils.isFrozen(player)) {
            event.isCancelled = true
            return
        }

        // 2) Pose/state dependent restrictions.
        if (utils.isSitting(player) || utils.isLaying(player) || utils.isDisguised(player)) {
            val entityType = event.rightClicked.type
            if (entityType != EntityType.ITEM_FRAME && entityType != EntityType.ARMOR_STAND) {
                event.isCancelled = true
                return
            }
        }

        // 3) Optional feature action (sithead).
        if (event.rightClicked is Player && player.inventory.itemInMainHand.type == Material.AIR) {
            val target = event.rightClicked as Player
            if (target.gameMode == GameMode.SPECTATOR) {
                event.isCancelled = true
                return
            }
            if (player.hasPermission("advancedcreative.sithead") && !hooker.sitheadManager.isInteractionBlocked(player)) {
                sitManager.sitOnHead(player, target)
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player

        // 1) High priority state restrictions.
        if (hooker.grabManager.blockGrabbedInteraction(player, event.action)) {
            event.isCancelled = true
            return
        }
        if (hooker.jarManager.blockJarredInteraction(player, event.action)) {
            event.isCancelled = true
            return
        }
        if (utils.isFrozen(player)) {
            event.isCancelled = true
            return
        }

        // 2) Dependent pose interactions.
        if (event.action == Action.LEFT_CLICK_AIR) {
            val headPassenger = sitManager.getHeadPassenger(player)
            if (headPassenger != null) {
                sitManager.launchHeadPassenger(player)
                return
            }
        }

        // 3) Lowest priority interaction features.
        handleRightClickBlock(event, player)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val block = event.block

        if (hooker.grabManager.blockGrabbedBlockBreak(player)) {
            event.isCancelled = true
            return
        }
        hooker.jarManager.handleSupportBreak(event)
        if (event.isCancelled) return
        if (utils.isFrozen(player)) {
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
        if (hooker.grabManager.blockGrabbedGlide(player, event.isGliding)) {
            event.isCancelled = true
            return
        }
        if (hooker.jarManager.blockJarredGlide(player, event.isGliding)) {
            event.isCancelled = true
            return
        }
        if (utils.isGliding(player) && !event.isGliding) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerToggleFlight(event: PlayerToggleFlightEvent) {
        val player = event.player
        if (hooker.grabManager.enforceGrabbedFlight(player)) {
            event.isCancelled = true
            return
        }
        if (hooker.jarManager.enforceJarredFlight(player)) {
            event.isCancelled = true
            return
        }
        utils.checkCrawlUncrawl(player)
        utils.checkLayingUnlay(player)
        utils.checkSitUnsit(player)
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onEntityToggleSwim(event: EntityToggleSwimEvent) {
        val player = event.entity as? Player ?: return
        if (!event.isSwimming && utils.isCrawling(player)) {
            event.isCancelled = true
            if (!player.isSwimming) player.isSwimming = true
        }
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
        hooker.disguiseManager.onViewerJoin(joiningPlayer)
        hooker.layManager.onViewerJoin(joiningPlayer)
        hooker.jarManager.onViewerJoin(joiningPlayer)
        hideManager.reapplyAllHides(joiningPlayer)
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerKick(event: PlayerKickEvent) {
        hooker.disguiseManager.onViewerDisconnect(event.player.uniqueId)
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerChangedWorld(event: PlayerChangedWorldEvent) {
        hooker.disguiseManager.onViewerWorldOrRespawn(event.player)
        hooker.layManager.onViewerJoin(event.player)
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
        hooker.grabManager.onHolderHotbarScroll(player, event.previousSlot, event.newSlot)
        if (utils.isLaying(player)) hooker.playerStateManager.handleItemSwitch(player, event.newSlot)
        if (utils.isDisguised(player)) {
            hooker.playerStateManager.handleItemSwitch(player, event.newSlot)
            hooker.disguiseManager.updateMainHandEquipment(player)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerInteractAtEntity(event: PlayerInteractAtEntityEvent) {
        if (hooker.grabManager.blockGrabbedInteractAtEntity(event.player)) {
            event.isCancelled = true
            return
        }
        if (hooker.jarManager.blockJarredInteraction(event.player)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerCommandPreprocess(event: PlayerCommandPreprocessEvent) {
        if (hooker.grabManager.blockGrabbedCommand(event.player)) {
            event.isCancelled = true
            return
        }
        if (hooker.jarManager.blockJarredCommand(event.player)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val damagerPlayer = resolveDamagerPlayer(event.damager)
        val directDamager = event.damager as? Player

        val target = event.entity as? Player
        if (target != null && directDamager != null && hooker.grabManager.handleHolderAttack(directDamager, target)) {
            event.isCancelled = true
            return
        }
        if (target != null && damagerPlayer != null && hooker.jarManager.handleJarredAttack(damagerPlayer, target)) {
            event.isCancelled = true
            return
        }

        if (directDamager != null && hooker.grabManager.blockGrabbedDamage(directDamager)) {
            event.isCancelled = true
            return
        }

        if (damagerPlayer != null && hooker.jarManager.blockJarredInteraction(damagerPlayer)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onProjectileHit(event: ProjectileHitEvent) {
        val shooter = event.entity.shooter as? Player ?: return
        val target = event.hitEntity as? Player ?: return
        hooker.jarManager.handleJarredAttack(shooter, target)
    }

    private fun resolveDamagerPlayer(damager: org.bukkit.entity.Entity): Player? {
        return when (damager) {
            is Player -> damager
            is Projectile -> damager.shooter as? Player
            else -> null
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        if (utils.isFrozen(player) && hasPositionChanged(event)) event.isCancelled = true
        if (hooker.jarManager.blockJarredMove(player) && hasPositionChanged(event)) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBlockPlace(event: BlockPlaceEvent) {
        hooker.jarManager.handleJarBlockPlace(event)
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        hooker.disguiseManager.onViewerWorldOrRespawn(event.player)
        hooker.layManager.onViewerJoin(event.player)
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

    private fun hasPositionChanged(event: PlayerMoveEvent): Boolean {
        val to = event.to ?: return false
        return event.from.x != to.x || event.from.y != to.y || event.from.z != to.z
    }
}
