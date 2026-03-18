package com.ratger.acreative.utils

import com.ratger.acreative.core.FunctionHooker
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class Utils(private val hooker: FunctionHooker) {

    private inline fun <T> stopAll(playersMap: Map<Player, T>, stopAction: (Player) -> Unit) {
        playersMap.keys.toList().forEach(stopAction)
    }

    private inline fun checkDisable(player: Player, isActive: (Player) -> Boolean, disable: (Player) -> Unit) {
        if (isActive(player)) disable(player)
    }

    fun isSitting(player: Player) = hooker.sitManagerOrNull()?.isSitting(player) ?: false
    fun isGliding(player: Player) = hooker.glideManagerOrNull()?.glidingPlayers?.contains(player) ?: false
    fun isCrawling(player: Player) = hooker.crawlManagerOrNull()?.crawlingPlayers?.containsKey(player) ?: false
    fun isLaying(player: Player) = hooker.layManagerOrNull()?.layingMap?.containsKey(player) ?: false
    fun isCustomGravity(player: Player) = hooker.gravityManagerOrNull()?.gravityPlayers?.containsKey(player) ?: false
    fun isCustomSize(player: Player) = hooker.resizeManagerOrNull()?.scaledPlayers?.containsKey(player) ?: false
    fun isCustomStrength(player: Player) = hooker.strengthManagerOrNull()?.strengthPlayers?.containsKey(player) ?: false
    fun isCustomHealth(player: Player) = hooker.healthManagerOrNull()?.healthPlayers?.containsKey(player) ?: false
    fun isFrozen(player: Player) = hooker.freezeManagerOrNull()?.frozenPlayers?.containsKey(player) ?: false
    fun isGlowing(player: Player) = hooker.glowManagerOrNull()?.glowingPlayers?.contains(player) ?: false
    fun isPissing(player: Player) = hooker.pissManagerOrNull()?.pissingPlayers?.containsKey(player) ?: false
    fun isDisguised(player: Player) = hooker.disguiseManagerOrNull()?.disguisedPlayers?.containsKey(player) ?: false
    fun isCustomEffect(player: Player) = hooker.effectsManagerOrNull()?.activeEffects?.containsKey(player.uniqueId) ?: false
    fun isSlapping(player: Player) = hooker.slapManagerOrNull()?.slappingPlayers?.contains(player) ?: false

    fun stopAllGlides() {
        val glideManager = hooker.glideManagerOrNull() ?: return
        stopAll(glideManager.glidingPlayers.associateWith { true }) { glideManager.unglidePlayer(it) }
    }

    fun stopAllCrawls() {
        val crawlManager = hooker.crawlManagerOrNull() ?: return
        stopAll(crawlManager.crawlingPlayers) { crawlManager.uncrawlPlayer(it) }
    }

    fun stopAllLays() {
        val layManager = hooker.layManagerOrNull() ?: return
        stopAll(layManager.layingMap) { layManager.unlayPlayer(it) }
    }

    fun stopAllCustomGravity() {
        val gravityManager = hooker.gravityManagerOrNull() ?: return
        stopAll(gravityManager.gravityPlayers) { gravityManager.removeEffect(it) }
    }

    fun stopAllCustomResize() {
        val resizeManager = hooker.resizeManagerOrNull() ?: return
        stopAll(resizeManager.scaledPlayers) { resizeManager.removeEffect(it) }
    }

    fun stopAllCustomStrength() {
        val strengthManager = hooker.strengthManagerOrNull() ?: return
        stopAll(strengthManager.strengthPlayers) { strengthManager.removeEffect(it) }
    }

    fun stopAllCustomHealth() {
        val healthManager = hooker.healthManagerOrNull() ?: return
        stopAll(healthManager.healthPlayers) { healthManager.removeEffect(it) }
    }

    fun stopAllFreezes() {
        val freezeManager = hooker.freezeManagerOrNull() ?: return
        stopAll(freezeManager.frozenPlayers) { freezeManager.unfreezePlayer(it) }
    }

    fun stopAllGlows() {
        val glowManager = hooker.glowManagerOrNull() ?: return
        stopAll(glowManager.glowingPlayers.associateWith { null }) { glowManager.removeGlow(it) }
    }

    fun stopAllPiss() {
        val pissManager = hooker.pissManagerOrNull() ?: return
        pissManager.pissingPlayers.keys.toList().forEach { pissManager.stopPiss(it) }
    }

    fun stopAllDisguises() {
        val disguiseManager = hooker.disguiseManagerOrNull() ?: return
        stopAll(disguiseManager.disguisedPlayers) { disguiseManager.undisguisePlayer(it) }
    }

    fun stopAllSlaps() {
        val slapManager = hooker.slapManagerOrNull() ?: return
        stopAll(slapManager.slappingPlayers.associateWith { true }) { slapManager.unslapPlayer(it) }
    }

    fun checkSitUnsit(player: Player) {
        hooker.sitManagerOrNull()?.let { checkDisable(player, ::isSitting, it::unsitPlayer) }
    }
    fun checkGlideUnglide(player: Player) {
        hooker.glideManagerOrNull()?.let { checkDisable(player, ::isGliding, it::unglidePlayer) }
    }
    fun checkCrawlUncrawl(player: Player) {
        hooker.crawlManagerOrNull()?.let { checkDisable(player, ::isCrawling, it::uncrawlPlayer) }
    }
    fun checkLayingUnlay(player: Player) {
        hooker.layManagerOrNull()?.let { checkDisable(player, ::isLaying, it::unlayPlayer) }
    }
    fun checkCustomGravityDisable(player: Player) {
        hooker.gravityManagerOrNull()?.let { checkDisable(player, ::isCustomGravity, it::removeEffect) }
    }
    fun checkCustomSizeDisable(player: Player) {
        hooker.resizeManagerOrNull()?.let { checkDisable(player, ::isCustomSize, it::removeEffect) }
    }
    fun checkCustomStrengthDisable(player: Player) {
        hooker.strengthManagerOrNull()?.let { checkDisable(player, ::isCustomStrength, it::removeEffect) }
    }
    fun checkCustomHealthDisable(player: Player) {
        hooker.healthManagerOrNull()?.let { checkDisable(player, ::isCustomHealth, it::removeEffect) }
    }
    fun checkFreezeUnfreeze(player: Player) {
        hooker.freezeManagerOrNull()?.let { checkDisable(player, ::isFrozen, it::unfreezePlayer) }
    }
    fun checkGlowDisable(player: Player) {
        hooker.glowManagerOrNull()?.let { checkDisable(player, ::isGlowing, it::removeGlow) }
    }
    fun checkPissStop(player: Player) {
        hooker.pissManagerOrNull()?.let { checkDisable(player, ::isPissing, it::stopPiss) }
    }
    fun checkDisguiseDisable(player: Player) {
        hooker.disguiseManagerOrNull()?.let { checkDisable(player, ::isDisguised, it::undisguisePlayer) }
    }
    fun checkCustomEffectsDisable(player: Player) {
        hooker.effectsManagerOrNull()?.let { checkDisable(player, ::isCustomEffect, it::clearEffects) }
    }
    fun checkSlapUnslap(player: Player) {
        hooker.slapManagerOrNull()?.let { checkDisable(player, ::isSlapping, it::unslapPlayer) }
    }

    fun isHiddenFromPlayer(hider: Player, target: Player): Boolean {
        val hideManager = hooker.hideManagerOrNull() ?: return false
        return hideManager.hiddenPlayers[hider.uniqueId]?.contains(target.uniqueId) ?: false
    }

    fun getPlayersWithHides() =
        hooker.hideManagerOrNull()?.hiddenPlayers?.keys?.mapNotNull(Bukkit::getPlayer)?.filter { it.isOnline } ?: emptyList()

    fun stopAllHides() {
        // During shutdown, calling showPlayer/hidePlayer is illegal. Just clear state.
        val hideManager = hooker.hideManagerOrNull() ?: return
        if (!hooker.plugin.isEnabled) {
            hideManager.hiddenPlayers.clear()
            return
        }
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
        val effectsManager = hooker.effectsManagerOrNull() ?: return
        val playerMap = effectsManager.activeEffects.keys
            .mapNotNull { Bukkit.getPlayer(it) }
            .associateWith { true }
        stopAll(playerMap) { player ->
            effectsManager.activeEffects[player.uniqueId]?.keys?.toList()?.forEach { effectType ->
                effectsManager.removeEffect(player, effectType)
            }
        }
    }

    fun stopAllSits() {
        val sitManager = hooker.sitManagerOrNull() ?: return
        val playersToUnsit = mutableListOf<Player>()
        val processed = mutableSetOf<Player>()
        val maxDepth = 10

        for (player in sitManager.getSittingPlayers()) {
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
            if (sitManager.isSitting(player)) {
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
            hooker.sitManagerOrNull()?.unsitPlayer(player, !removeHeadPassengers)
        }

        if (removeHeadPassengers) {
            val headPassenger = hooker.sitManagerOrNull()?.getHeadPassenger(player)
            if (headPassenger != null) {
                hooker.sitManagerOrNull()?.unsitPlayer(headPassenger, true)
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
