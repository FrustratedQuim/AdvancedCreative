package com.ratger.acreative.commands.crawl

import com.ratger.acreative.core.FunctionHooker
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Player

class CrawlManager(private val hooker: FunctionHooker) {

    companion object {
        private const val UPDATE_BARRIER_PERIOD_TICKS = 2L
    }

    val crawlingPlayers = mutableMapOf<Player, CrawlingPlayer>()

    inner class CrawlingPlayer(val player: Player) {
        var barrierBlock: org.bukkit.block.Block? = null

        fun updateBarrier(): Boolean {
            if (!player.isOnline || player.isDead || player.isFlying ||
                player.location.block.type == Material.WATER || player.location.block.type == Material.LAVA
            ) {
                uncrawlPlayer(player)
                return false
            }

            val loc = player.location
            val newLoc = loc.clone().add(0.0, 1.5, 0.0).toBlockLocation()
            val newBlock = newLoc.block

            if (newBlock == barrierBlock) return true

            removeBarrier()
            if (!newBlock.type.isAir) return true

            newBlock.type = Material.BARRIER
            barrierBlock = newBlock
            return true
        }

        fun removeBarrier() {
            barrierBlock?.takeIf { it.type == Material.BARRIER }?.type = Material.AIR
            barrierBlock = null
        }
    }

    fun canCrawl(player: Player): Boolean {
        val blockBelow = player.location.clone().add(0.0, -1.0, 0.0).block
        return player.gameMode != GameMode.SPECTATOR &&
                !player.isFlying &&
                blockBelow.type.isSolid
    }

    fun crawlPlayer(player: Player) {
        if (hooker.utils.isDisguised(player)) {
            hooker.messageManager.sendMiniMessage(player, key = "error-cannot-disguised")
            return
        }
        if (!canCrawl(player)) {
            hooker.messageManager.sendMiniMessage(player, key = "error-crawl-in-air")
            return
        }
        if (!hooker.utils.checkAndRemovePose(player)) {
            return
        }
        hooker.utils.checkCustomSizeDisable(player)
        if (crawlingPlayers.containsKey(player)) return
        val crawling = CrawlingPlayer(player)
        crawlingPlayers[player] = crawling
        crawling.updateBarrier()

        hooker.messageManager.sendMiniMessage(player, key = "info-crawl-on")
        hooker.messageManager.sendMiniMessage(player, "ACTION", "action-pose-unset", repeatable = true)
    }

    fun uncrawlPlayer(player: Player) {
        if (!crawlingPlayers.containsKey(player)) return
        crawlingPlayers.remove(player)?.removeBarrier()

        hooker.messageManager.sendMiniMessage(player, key = "info-crawl-off")
        hooker.messageManager.sendMiniMessage(player, "ACTION_STOP")
        hooker.playerStateManager.refreshPlayerPose(player)
    }

    fun startBarrierUpdater() {
        Bukkit.getScheduler().runTaskTimer(hooker.plugin, Runnable {
            val iterator = crawlingPlayers.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val player = entry.key
                val crawler = entry.value
                if (!crawler.updateBarrier()) {
                    iterator.remove()
                    continue
                }
                hooker.messageManager.sendMiniMessage(player, "ACTION", "action-pose-unset", null, true)
            }
        }, 0L, UPDATE_BARRIER_PERIOD_TICKS)
    }
}