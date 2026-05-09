package com.ratger.acreative.commands.crawl

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.world.Location as PacketLocation
import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.MessageChannel
import com.ratger.acreative.core.MessageKey
import com.ratger.acreative.utils.PlayerStateManager.PlayerStateType
import me.tofaa.entitylib.wrapper.WrapperEntity
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player

class CrawlManager(private val hooker: FunctionHooker) {

    companion object {
        private const val UPDATE_BARRIER_PERIOD_TICKS = 2L
    }

    private val constraintAdapter = CrawlConstraintAdapter()
    private val barrierPresenter = BarrierPresenter()
    private val shulkerPresenter = ShulkerPresenter()

    private val crawlingPlayers = mutableMapOf<Player, CrawlSession>()

    fun isCrawling(player: Player): Boolean = crawlingPlayers.containsKey(player)

    fun activeCrawlers(): Set<Player> = crawlingPlayers.keys

    fun canCrawl(player: Player): Boolean {
        val blockBelow = player.location.clone().add(0.0, -1.0, 0.0).block
        return player.gameMode != GameMode.SPECTATOR &&
                !player.isFlying &&
                blockBelow.type.isSolid
    }

    fun crawlPlayer(player: Player) {
        if (isCrawling(player)) {
            uncrawlPlayer(player)
            return
        }
        if (!canCrawl(player)) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_CRAWL_IN_AIR)
            return
        }
        hooker.playerStateManager.activateState(player, PlayerStateType.CRAWLING)
        val session = CrawlSession(player)
        crawlingPlayers[player] = session
        hooker.actionLogger.info(
            "Starting crawl for ${hooker.actionLogger.playerRef(player)} activeCrawlers=${crawlingPlayers.size}"
        )
        updateSession(session)

        hooker.messageManager.sendChat(player, MessageKey.INFO_CRAWL_ON)
        hooker.messageManager.startRepeatingActionBar(player, MessageKey.ACTION_POSE_UNSET)
    }

    fun uncrawlPlayer(player: Player) {
        if (!crawlingPlayers.containsKey(player)) {
            hooker.playerStateManager.deactivateState(player, PlayerStateType.CRAWLING)
            return
        }
        hooker.actionLogger.info(
            "Stopping crawl for ${hooker.actionLogger.playerRef(player)}"
        )
        crawlingPlayers.remove(player)?.let { session ->
            barrierPresenter.clear(session)
            shulkerPresenter.clear(session)
        }

        hooker.messageManager.sendChat(player, MessageKey.INFO_CRAWL_OFF)
        hooker.messageManager.stopRepeating(player, MessageChannel.ACTION_BAR)
        hooker.playerStateManager.deactivateState(player, PlayerStateType.CRAWLING)
        hooker.playerStateManager.refreshPlayerPose(player)
    }

    fun startBarrierUpdater() {
        hooker.tickScheduler.runRepeating(0L, UPDATE_BARRIER_PERIOD_TICKS) {
            val toUncrawl = mutableListOf<Player>()
            val snapshot = crawlingPlayers.values.toList()
            for (session in snapshot) {
                val keepCrawling = try {
                    updateSession(session)
                } catch (t: Throwable) {
                    t.printStackTrace()
                    false
                }
                if (!keepCrawling) {
                    toUncrawl.add(session.player)
                }
            }

            for (player in toUncrawl) {
                uncrawlPlayer(player)
            }
        }
    }

    private fun updateSession(session: CrawlSession): Boolean {
        val player = session.player
        if (!player.isOnline || player.isDead || player.isFlying ||
            player.location.block.type == Material.WATER || player.location.block.type == Material.LAVA
        ) {
            barrierPresenter.clear(session)
            shulkerPresenter.clear(session)
            return false
        }

        return when (constraintAdapter.resolve(session)) {
            CrawlConstraintMode.BARRIER -> {
                shulkerPresenter.clear(session)
                barrierPresenter.apply(session)
                true
            }

            CrawlConstraintMode.SHULKER -> {
                barrierPresenter.clear(session)
                shulkerPresenter.apply(session)
                true
            }

            CrawlConstraintMode.NONE -> {
                barrierPresenter.clear(session)
                shulkerPresenter.clear(session)
                true
            }
        }
    }

    private data class CrawlSession(
        val player: Player,
        var barrierLocation: Location? = null,
        var pendingBarrierLocation: Location? = null,
        var shulkerEntity: WrapperEntity? = null
    )

    private enum class CrawlConstraintMode {
        BARRIER,
        SHULKER,
        NONE
    }

    private interface CrawlPresenter {
        fun apply(session: CrawlSession)
        fun clear(session: CrawlSession)
    }

    private inner class CrawlConstraintAdapter {
        fun resolve(session: CrawlSession): CrawlConstraintMode {
            val playerLoc = session.player.location
            val overheadLocation = playerLoc.clone().add(0.0, 1.5, 0.0).toBlockLocation()
            val overheadBlock = overheadLocation.block

            if (overheadBlock.type.isAir) {
                session.pendingBarrierLocation = overheadLocation
                return CrawlConstraintMode.BARRIER
            }

            session.pendingBarrierLocation = null
            return if (shouldSpawnShulker(playerLoc)) CrawlConstraintMode.SHULKER else CrawlConstraintMode.NONE
        }

        private fun shouldSpawnShulker(playerLoc: Location): Boolean {
            val tickLoc = playerLoc.clone()
            val baseBlock = tickLoc.block
            val blockSize = ((tickLoc.y - baseBlock.y) * 100.0).toInt()
            tickLoc.y = baseBlock.y + if (blockSize >= 40) 2.49 else 1.49

            val aboveBlock = tickLoc.block
            if (aboveBlock.type.isAir) return false

            return try {
                val boundingBox = aboveBlock.boundingBox
                val containsProbe = boundingBox.contains(tickLoc.toVector())
                val collisions = aboveBlock.collisionShape.boundingBoxes
                val hasCollision = collisions.isNotEmpty()

                if (hasCollision && containsProbe) return false
                !aboveBlock.type.isAir
            } catch (e: Throwable) {
                e.printStackTrace()
                !aboveBlock.type.isAir
            }
        }
    }

    private inner class BarrierPresenter : CrawlPresenter {
        override fun apply(session: CrawlSession) {
            val player = session.player
            val barrierLocation = session.pendingBarrierLocation ?: return

            val previousBarrier = session.barrierLocation
            if (previousBarrier != null && previousBarrier != barrierLocation && player.isOnline) {
                player.sendBlockChange(previousBarrier, previousBarrier.block.blockData)
            }

            player.sendBlockChange(barrierLocation, Material.BARRIER.createBlockData())
            session.barrierLocation = barrierLocation
        }

        override fun clear(session: CrawlSession) {
            val player = session.player
            session.barrierLocation?.let { location ->
                if (player.isOnline) {
                    player.sendBlockChange(location, location.block.blockData)
                }
            }
            session.barrierLocation = null
            session.pendingBarrierLocation = null
        }
    }

    private inner class ShulkerPresenter : CrawlPresenter {
        override fun apply(session: CrawlSession) {
            val player = session.player
            val playerLocation = player.location
            val spawnLocation = playerLocation.clone().add(0.0, 1.5, 0.0).toBlockLocation().add(0.5, 0.0, 0.5)

            val entity = session.shulkerEntity
            if (entity == null || !entity.isSpawned) {
                hooker.actionLogger.info(
                    "Spawning crawl shulker for ${hooker.actionLogger.playerRef(player)} at ${hooker.actionLogger.locationRef(spawnLocation)}"
                )
                val shulker = WrapperEntity(EntityTypes.SHULKER)
                shulker.addViewer(player.uniqueId)
                shulker.entityMeta.isInvisible = true
                shulker.entityMeta.isSilent = true
                shulker.spawn(PacketLocation(spawnLocation.x, spawnLocation.y, spawnLocation.z, 0f, 0f))
                session.shulkerEntity = shulker
                return
            }

            entity.teleport(PacketLocation(spawnLocation.x, spawnLocation.y, spawnLocation.z, 0f, 0f))
        }

        override fun clear(session: CrawlSession) {
            if (session.shulkerEntity != null) {
                hooker.actionLogger.info(
                    "Removing crawl shulker for ${hooker.actionLogger.playerRef(session.player)}"
                )
            }
            session.shulkerEntity?.let { entity ->
                if (entity.isSpawned) entity.remove()
            }
            session.shulkerEntity = null
        }
    }
}
