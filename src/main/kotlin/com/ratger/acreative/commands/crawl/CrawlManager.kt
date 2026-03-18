package com.ratger.acreative.commands.crawl

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.MessageChannel
import com.ratger.acreative.core.MessageKey
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.ratger.acreative.core.FunctionHooker
import me.tofaa.entitylib.wrapper.WrapperEntity
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import com.ratger.acreative.utils.PlayerStateManager.PlayerStateType
import com.github.retrooper.packetevents.protocol.world.Location as PacketLocation

class CrawlManager(private val hooker: FunctionHooker) {

    companion object {
        private const val UPDATE_BARRIER_PERIOD_TICKS = 2L
    }

    val crawlingPlayers = mutableMapOf<Player, CrawlingPlayer>()

    inner class CrawlingPlayer(val player: Player) {
        var barrierLocation: Location? = null
        var shulkerEntity: WrapperEntity? = null

        fun updateBarrier(): Boolean {
            if (!player.isOnline || player.isDead || player.isFlying ||
                player.location.block.type == Material.WATER || player.location.block.type == Material.LAVA
            ) {
                // Do not modify crawlingPlayers here to avoid CME during iteration.
                // Perform local cleanup and signal the manager to uncrawl after the loop.
                removeBarrier()
                removeShulker()
                return false
            }

            val loc = player.location
            // Keep server-side swimming state while crawling
            if (!player.isSwimming) player.isSwimming = true

            val newLoc = loc.clone().add(0.0, 1.5, 0.0).toBlockLocation()
            val overheadBlock = newLoc.block

            // If overhead is AIR, show a packetized barrier for this player only
            if (overheadBlock.type.isAir) {
                // Remove shulker if present from previous state
                removeShulker()

                if (barrierLocation == null || !barrierLocation!!.equals(newLoc)) {
                    // Revert previous fake barrier, if any
                    barrierLocation?.let { prev ->
                        if (player.isOnline) {
                            player.sendBlockChange(prev, prev.block.blockData)
                        }
                    }
                    barrierLocation = newLoc
                }
                // Always re-send fake barrier to keep it persistent client-side
                player.sendBlockChange(newLoc, Material.BARRIER.createBlockData())
                return true
            }

            // Overhead is not air: revert any barrier, and if bounding box indicates partial/unsafe headroom,
            // spawn a hidden shulker client-entity to avoid accidental standing without altering visuals.
            removeBarrier()
            if (shouldSpawnShulker(loc)) {
                spawnOrMoveShulker(newLoc.add(0.5, 0.0, 0.5))
            } else {
                // Full block or other shape – shulker not needed
                removeShulker()
            }
            return true
        }

        fun removeBarrier() {
            barrierLocation?.let { prev ->
                if (player.isOnline) {
                    player.sendBlockChange(prev, prev.block.blockData)
                }
            }
            barrierLocation = null
        }

        private fun shouldSpawnShulker(playerLoc: Location): Boolean {
            // Paper API: use bounding boxes to evaluate headroom.
            val tickLoc = playerLoc.clone()
            val baseBlock = tickLoc.block
            val blockSize = ((tickLoc.y - baseBlock.y) * 100.0).toInt()
            tickLoc.y = baseBlock.y + if (blockSize >= 40) 2.49 else 1.49

            val aboveBlock = tickLoc.block
            val isAir = aboveBlock.type.isAir
            // If it's air, shulker is not needed here (handled by packet barrier branch)
            if (isAir) return false

            // Safeguard: if Paper collision API is unavailable for some reason, fall back to type-based heuristic
            return try {
                val bb = aboveBlock.boundingBox
                val containsProbe = bb.contains(tickLoc.toVector())
                val collisions = aboveBlock.collisionShape.boundingBoxes
                val hasCollision = collisions.isNotEmpty()

                // If there is a full solid collision at the probe point, player already cannot stand -> no shulker needed
                if (hasCollision && containsProbe) return false

                // For any non-air block above (including non-colliding like buttons/torches), keep shulker to force crawling
                !aboveBlock.type.isAir
            } catch (e: Throwable) {
                e.printStackTrace()
                // Be conservative: when unsure and block is not air, keep crawling with a shulker
                !aboveBlock.type.isAir
            }
        }

        private fun spawnOrMoveShulker(center: Location) {
            val existing = shulkerEntity
            if (existing == null || !existing.isSpawned) {
                // Create new client-side Shulker entity, invisible for the crawling player only
                val entity = WrapperEntity(EntityTypes.SHULKER)
                // Hide from everyone by default, then add only the crawler as viewer
                entity.addViewer(player.uniqueId)
                // Try to keep it invisible/silent to avoid any visuals
                entity.entityMeta.isInvisible = true
                entity.entityMeta.isSilent = true
                entity.spawn(PacketLocation(center.x, center.y, center.z, 0f, 0f))
                shulkerEntity = entity
            } else {
                // Teleport/move existing to new center
                existing.teleport(PacketLocation(center.x, center.y, center.z, 0f, 0f))
            }
        }

        fun removeShulker() {
            shulkerEntity?.let { ent ->
                if (ent.isSpawned) ent.remove()
            }
            shulkerEntity = null
        }
    }

    fun canCrawl(player: Player): Boolean {
        val blockBelow = player.location.clone().add(0.0, -1.0, 0.0).block
        return player.gameMode != GameMode.SPECTATOR &&
                !player.isFlying &&
                blockBelow.type.isSolid
    }

    fun crawlPlayer(player: Player) {
        if (!canCrawl(player)) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_CRAWL_IN_AIR)
            return
        }
        hooker.playerStateManager.activateState(player, PlayerStateType.CRAWLING)
        if (crawlingPlayers.containsKey(player)) {
            hooker.playerStateManager.deactivateState(player, PlayerStateType.CRAWLING)
            return
        }
        val crawling = CrawlingPlayer(player)
        crawlingPlayers[player] = crawling
        player.isSwimming = true
        crawling.updateBarrier()

        hooker.messageManager.sendChat(player, MessageKey.INFO_CRAWL_ON)
        hooker.messageManager.startRepeatingActionBar(player, MessageKey.ACTION_POSE_UNSET)
    }

    fun uncrawlPlayer(player: Player) {
        if (!crawlingPlayers.containsKey(player)) {
            hooker.playerStateManager.deactivateState(player, PlayerStateType.CRAWLING)
            return
        }
        crawlingPlayers.remove(player)?.let {
            it.removeBarrier()
            it.removeShulker()
        }

        hooker.messageManager.sendChat(player, MessageKey.INFO_CRAWL_OFF)
        hooker.messageManager.stopRepeating(player, MessageChannel.ACTION_BAR)
        if (player.isOnline) player.isSwimming = false
        hooker.playerStateManager.deactivateState(player, PlayerStateType.CRAWLING)
        hooker.playerStateManager.refreshPlayerPose(player)
    }

    fun startBarrierUpdater() {
        hooker.tickScheduler.runRepeating(0L, UPDATE_BARRIER_PERIOD_TICKS) {
            val toUncrawl = mutableListOf<Player>()
            // Iterate over a snapshot to avoid ConcurrentModificationException
            val snapshot = crawlingPlayers.values.toList()
            for (crawler in snapshot) {
                val keepCrawling = try {
                    crawler.updateBarrier()
                } catch (t: Throwable) {
                    t.printStackTrace()
                    false
                }
                if (!keepCrawling) {
                    toUncrawl.add(crawler.player)
                }
            }

            // Process uncrawl after iteration to avoid modifying the map during traversal
            for (p in toUncrawl) {
                uncrawlPlayer(p)
            }
        }
    }
}
