package com.ratger.acreative.commands.crawl

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.world.Location as PacketLocation
import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.MessageChannel
import com.ratger.acreative.core.MessageKey
import com.ratger.acreative.utils.PlayerStateManager.PlayerStateType
import me.tofaa.entitylib.wrapper.WrapperEntity
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.entity.Pose

class CrawlManager(private val hooker: FunctionHooker) {

    companion object {
        private const val UPDATE_CRAWL_PERIOD_TICKS = 2L
    }

    private val posePresenter = CrawlPosePresenter()
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
            shulkerPresenter.clear(session)
            posePresenter.clear(session.player)
        }

        hooker.messageManager.sendChat(player, MessageKey.INFO_CRAWL_OFF)
        hooker.messageManager.stopRepeating(player, MessageChannel.ACTION_BAR)
        hooker.playerStateManager.deactivateState(player, PlayerStateType.CRAWLING)
        hooker.playerStateManager.refreshPlayerPose(player)
    }

    fun startCrawlUpdater() {
        hooker.tickScheduler.runRepeating(0L, UPDATE_CRAWL_PERIOD_TICKS) {
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
            shulkerPresenter.clear(session)
            posePresenter.clear(player)
            return false
        }

        posePresenter.apply(player)
        shulkerPresenter.apply(session)
        return true
    }

    private data class CrawlSession(
        val player: Player,
        var shulkerEntity: WrapperEntity? = null
    )

    private interface CrawlPresenter {
        fun apply(session: CrawlSession)
        fun clear(session: CrawlSession)
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

    private inner class CrawlPosePresenter {
        fun apply(player: Player) {
            if (player.pose == Pose.SWIMMING && player.hasFixedPose()) return
            player.setPose(Pose.SWIMMING, true)
        }

        fun clear(player: Player) {
            if (!player.hasFixedPose() && player.pose != Pose.SWIMMING) return
            player.setPose(Pose.STANDING, false)
        }
    }
}
