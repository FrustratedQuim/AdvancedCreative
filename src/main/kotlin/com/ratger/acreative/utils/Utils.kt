package com.ratger.acreative.utils

import com.ratger.acreative.commands.bind.BindManager
import com.ratger.acreative.commands.crawl.CrawlManager
import com.ratger.acreative.commands.disguise.DisguiseManager
import com.ratger.acreative.commands.effects.EffectsManager
import com.ratger.acreative.commands.freeze.FreezeManager
import com.ratger.acreative.commands.glide.GlideManager
import com.ratger.acreative.commands.glow.GlowManager
import com.ratger.acreative.commands.gravity.GravityManager
import com.ratger.acreative.commands.health.HealthManager
import com.ratger.acreative.commands.hide.HideManager
import com.ratger.acreative.commands.lay.LayManager
import com.ratger.acreative.commands.piss.PissManager
import com.ratger.acreative.commands.resize.ResizeManager
import com.ratger.acreative.commands.sit.SitManager
import com.ratger.acreative.commands.slap.SlapManager
import com.ratger.acreative.commands.strength.StrengthManager
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.Bukkit
import org.bukkit.block.Block
import org.bukkit.entity.Player

class Utils(
    private val hooker: FunctionHooker,
    private val sitManager: SitManager,
    private val glideManager: GlideManager,
    private val crawlManager: CrawlManager,
    private val hideManager: HideManager,
    private val layManager: LayManager,
    private val gravityManager: GravityManager,
    private val resizeManager: ResizeManager,
    private val strengthManager: StrengthManager,
    private val healthManager: HealthManager,
    private val freezeManager: FreezeManager,
    private val bindManager: BindManager,
    private val glowManager: GlowManager,
    private val pissManager: PissManager,
    private val disguiseManager: DisguiseManager,
    private val effectsManager: EffectsManager,
    private val slapManager: SlapManager
) {

    private inline fun <T> stopAll(playersMap: Map<Player, T>, stopAction: (Player) -> Unit) {
        playersMap.keys.toList().forEach(stopAction)
    }

    private inline fun checkDisable(player: Player, isActive: (Player) -> Boolean, disable: (Player) -> Unit) {
        if (isActive(player)) disable(player)
    }

    fun isSitting(player: Player) = sitManager.sittingMap.containsKey(player)
    fun isGliding(player: Player) = glideManager.glidingPlayers.contains(player)
    fun isCrawling(player: Player) = crawlManager.crawlingPlayers.containsKey(player)
    fun isLaying(player: Player) = layManager.layingMap.containsKey(player)
    fun isCustomGravity(player: Player) = gravityManager.gravityPlayers.containsKey(player)
    fun isCustomSize(player: Player) = resizeManager.scaledPlayers.containsKey(player)
    fun isCustomStrength(player: Player) = strengthManager.strengthPlayers.containsKey(player)
    fun isCustomHealth(player: Player) = healthManager.healthPlayers.containsKey(player)
    fun isFrozen(player: Player) = freezeManager.frozenPlayers.containsKey(player)
    fun isBound(player: Player) = bindManager.binds.containsKey(player.uniqueId)
    fun isGlowing(player: Player) = glowManager.glowingPlayers.contains(player)
    fun isPissing(player: Player) = pissManager.pissingPlayers.containsKey(player)
    fun isDisguised(player: Player) = disguiseManager.disguisedPlayers.containsKey(player)
    fun isCustomEffect(player: Player) = effectsManager.activeEffects.containsKey(player.uniqueId)
    fun isSlapping(player: Player) = slapManager.slappingPlayers.contains(player)

    fun stopAllGlides() = stopAll(glideManager.glidingPlayers.associateWith { true }) { glideManager.unglidePlayer(it) }
    fun stopAllCrawls() = stopAll(crawlManager.crawlingPlayers) { crawlManager.uncrawlPlayer(it) }
    fun stopAllLays() = stopAll(layManager.layingMap) { layManager.unlayPlayer(it) }
    fun stopAllCustomGravity() = stopAll(gravityManager.gravityPlayers) { gravityManager.removeEffect(it) }
    fun stopAllCustomResize() = stopAll(resizeManager.scaledPlayers) { resizeManager.removeEffect(it) }
    fun stopAllCustomStrength() = stopAll(strengthManager.strengthPlayers) { strengthManager.removeEffect(it) }
    fun stopAllCustomHealth() = stopAll(healthManager.healthPlayers) { healthManager.removeEffect(it) }
    fun stopAllFreezes() = stopAll(freezeManager.frozenPlayers) { freezeManager.unfreezePlayer(it) }
    fun stopAllGlows() = stopAll(glowManager.glowingPlayers.associateWith { null }) { glowManager.removeGlow(it) }
    fun stopAllPiss() = pissManager.pissingPlayers.keys.toList().forEach { pissManager.stopPiss(it) }
    fun stopAllDisguises() = stopAll(disguiseManager.disguisedPlayers) { disguiseManager.undisguisePlayer(it) }
    fun stopAllSlaps() = stopAll(slapManager.slappingPlayers.associateWith { true }) { slapManager.unslapPlayer(it) }

    fun checkSitUnsit(player: Player) = checkDisable(player, ::isSitting, sitManager::unsitPlayer)
    fun checkGlideUnglide(player: Player) = checkDisable(player, ::isGliding, glideManager::unglidePlayer)
    fun checkCrawlUncrawl(player: Player) = checkDisable(player, ::isCrawling, crawlManager::uncrawlPlayer)
    fun checkLayingUnlay(player: Player) = checkDisable(player, ::isLaying, layManager::unlayPlayer)
    fun checkCustomGravityDisable(player: Player) = checkDisable(player, ::isCustomGravity, gravityManager::removeEffect)
    fun checkCustomSizeDisable(player: Player) = checkDisable(player, ::isCustomSize, resizeManager::removeEffect)
    fun checkCustomStrengthDisable(player: Player) = checkDisable(player, ::isCustomStrength, strengthManager::removeEffect)
    fun checkCustomHealthDisable(player: Player) = checkDisable(player, ::isCustomHealth, healthManager::removeEffect)
    fun checkFreezeUnfreeze(player: Player) = checkDisable(player, ::isFrozen, freezeManager::unfreezePlayer)
    fun checkBindClear(player: Player) = checkDisable(player, ::isBound, bindManager::clearBinds)
    fun checkGlowDisable(player: Player) = checkDisable(player, ::isGlowing, glowManager::removeGlow)
    fun checkPissStop(player: Player) = checkDisable(player, ::isPissing, pissManager::stopPiss)
    fun checkDisguiseDisable(player: Player) = checkDisable(player, ::isDisguised, disguiseManager::undisguisePlayer)
    fun checkCustomEffectsDisable(player: Player) = checkDisable(player, ::isCustomEffect, effectsManager::clearEffects)
    fun checkSlapUnslap(player: Player) = checkDisable(player, ::isSlapping, slapManager::unslapPlayer)

    fun isBarrierAboveCrawlingPlayer(block: Block) =
        crawlManager.crawlingPlayers.values.any { it.barrierBlock == block }

    fun isHiddenFromPlayer(hider: Player, target: Player) =
        hideManager.hiddenPlayers[hider.uniqueId]?.contains(target.uniqueId) ?: false

    fun getPlayersWithHides() =
        hideManager.hiddenPlayers.keys.mapNotNull(Bukkit::getPlayer).filter { it.isOnline }

    fun stopAllHides() {
        getPlayersWithHides().forEach { player ->
            hideManager.hiddenPlayers[player.uniqueId]?.forEach { targetId ->
                Bukkit.getPlayer(targetId)?.let { target ->
                    if (player.isOnline) player.showPlayer(hooker.plugin, target)
                }
            }
            hideManager.hiddenPlayers.remove(player.uniqueId)
        }
    }

    fun stopAllCustomEffects() {
        val playerMap = effectsManager.activeEffects.keys
            .mapNotNull { Bukkit.getPlayer(it) }
            .associateWith { true }
        stopAll(playerMap) { player ->
            effectsManager.activeEffects[player.uniqueId]?.keys?.toList()?.forEach { effectType ->
                effectsManager.removeEffect(player, effectType)
            }
        }
    }

    fun stopAllBinds() {
        bindManager.binds.keys.mapNotNull(Bukkit::getPlayer).forEach { bindManager.clearBinds(it) }
    }

    fun checkAndRemovePose(player: Player): Boolean {
        val actions: List<Pair<(Player) -> Boolean, (Player) -> Unit>> = listOf(
            { p: Player -> isSitting(p) } to { p: Player -> sitManager.unsitPlayer(p) },
            { p: Player -> isGliding(p) } to { p: Player -> glideManager.unglidePlayer(p) },
            { p: Player -> isCrawling(p) } to { p: Player -> crawlManager.uncrawlPlayer(p) },
            { p: Player -> isLaying(p) } to { p: Player -> layManager.unlayPlayer(p) },
            { p: Player -> isFrozen(p) } to { p: Player -> freezeManager.unfreezePlayer(p) },
            { p: Player -> isPissing(p) } to { p: Player -> pissManager.stopPiss(p) }
        )

        for ((check, disable) in actions) {
            if (check(player)) {
                disable(player)
                return false
            }
        }
        return true
    }

    fun stopAllSits() {
        val playersToUnsit = mutableListOf<Player>()
        val processed = mutableSetOf<Player>()
        val maxDepth = 10

        for (player in sitManager.sittingMap.keys) {
            if (player in processed) continue
            var current: Player? = player
            val stack = mutableListOf<Player>()
            var depth = 0
            while (current != null && depth < maxDepth) {
                if (current in processed) break
                stack.add(current)
                processed.add(current)
                current = sitManager.getHeadPassenger(current)
                depth++
            }
            playersToUnsit.addAll(stack.reversed())
        }

        playersToUnsit.forEach { player ->
            if (sitManager.sittingMap.containsKey(player)) {
                sitManager.unsitPlayer(player)
            }
        }
    }

    fun unsetAllPoses(player: Player, removeHeadPassengers: Boolean = false) {
        checkGlideUnglide(player)
        checkCrawlUncrawl(player)
        checkLayingUnlay(player)
        checkFreezeUnfreeze(player)
        checkPissStop(player)

        if (isSitting(player)) {
            sitManager.unsitPlayer(player, !removeHeadPassengers)
        }

        if (removeHeadPassengers) {
            val headPassenger = sitManager.getHeadPassenger(player)
            if (headPassenger != null) {
                sitManager.unsitPlayer(headPassenger, true)
            }
        }
    }

    fun unsetAllStates(player: Player) {
        checkCustomGravityDisable(player)
        checkCustomSizeDisable(player)
        checkCustomStrengthDisable(player)
        checkCustomHealthDisable(player)
        checkPissStop(player)
    }
}