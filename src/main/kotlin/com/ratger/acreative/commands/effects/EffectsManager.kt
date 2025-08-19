package com.ratger.acreative.commands.effects

import com.ratger.acreative.core.FunctionHooker
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class EffectsManager(private val hooker: FunctionHooker) {

    val activeEffects = ConcurrentHashMap<UUID, MutableMap<PotionEffectType, Int>>()
    private val effectTasks = ConcurrentHashMap<UUID, MutableMap<PotionEffectType, Int>>()

    fun applyEffect(player: Player, effectName: String?, level: String? = "1", targetName: String? = null) {
        if (effectName == null) {
            handleNoEffectSpecified(player)
            return
        }

        if (effectName.equals("clear", ignoreCase = true)) {
            clearEffects(player)
            return
        }

        val effectType = Registry.EFFECT.get(NamespacedKey.minecraft(effectName.lowercase()))
        if (effectType == null) {
            hooker.messageManager.sendMiniMessage(player, key = "error-effect-unknown")
            return
        }

        val target = if (targetName != null && player.hasPermission("advancedcreative.effects.admin")) {
            Bukkit.getPlayer(targetName) ?: run {
                hooker.messageManager.sendMiniMessage(player, key = "error-unknown-player")
                return
            }
        } else {
            player
        }

        val levelInt = level?.toIntOrNull()?.coerceIn(1, 255) ?: 1

        val playerEffects = activeEffects.computeIfAbsent(target.uniqueId) { mutableMapOf() }
        if (playerEffects.containsKey(effectType)) {
            if (playerEffects[effectType] == levelInt) {
                removeEffect(target, effectType)
                hooker.messageManager.sendMiniMessage(
                    player,
                    key = if (target == player) "success-effect-removed" else "success-effect-removed-target",
                    variables = mapOf("player" to target.name)
                )
                return
            } else {
                playerEffects[effectType] = levelInt
                applyEffectToPlayer(target, effectType, levelInt)
                startEffectTask(target, effectType, levelInt)
                hooker.messageManager.sendMiniMessage(
                    player,
                    key = if (target == player) "success-effect" else "success-effect-target",
                    variables = mapOf("player" to target.name)
                )
                return
            }
        }

        playerEffects[effectType] = levelInt
        applyEffectToPlayer(target, effectType, levelInt)

        hooker.messageManager.sendMiniMessage(
            player,
            key = if (target == player) "success-effect" else "success-effect-target",
            variables = mapOf("player" to target.name)
        )

        startEffectTask(target, effectType, levelInt)
    }

    private fun handleNoEffectSpecified(player: Player) {
        val playerEffects = activeEffects[player.uniqueId]
        if (playerEffects.isNullOrEmpty()) {
            hooker.messageManager.sendMiniMessage(player, key = "usage-effects")
            return
        }

        if (playerEffects.size == 1) {
            val effectType = playerEffects.keys.first()
            removeEffect(player, effectType)
            hooker.messageManager.sendMiniMessage(player, key = "success-effect-removed")
        } else {
            clearEffects(player, sendMessage = true)
        }
    }

    fun clearEffects(player: Player, sendMessage: Boolean = true) {
        val playerEffects = activeEffects[player.uniqueId] ?: return
        playerEffects.keys.toList().forEach { removeEffect(player, it) }
        activeEffects.remove(player.uniqueId)
        if (sendMessage) {
            hooker.messageManager.sendMiniMessage(player, key = "success-effects-cleared")
        }
    }

    private fun applyEffectToPlayer(player: Player, effectType: PotionEffectType, level: Int) {
        player.removePotionEffect(effectType)
        player.addPotionEffect(PotionEffect(effectType, 20 * 20, level - 1, false, false, false))
    }

    private fun startEffectTask(player: Player, effectType: PotionEffectType, level: Int) {
        val playerTasks = effectTasks.computeIfAbsent(player.uniqueId) { mutableMapOf() }
        if (playerTasks.containsKey(effectType)) {
            Bukkit.getScheduler().cancelTask(playerTasks[effectType]!!)
        }

        val taskId = Bukkit.getScheduler().runTaskTimer(hooker.plugin, Runnable {
            if (!player.isOnline || !activeEffects[player.uniqueId].orEmpty().containsKey(effectType)) {
                removeEffect(player, effectType)
                return@Runnable
            }
            applyEffectToPlayer(player, effectType, level)
        }, 0L, 5 * 20L).taskId

        playerTasks[effectType] = taskId
    }

    fun removeEffect(player: Player, effectType: PotionEffectType) {
        val playerEffects = activeEffects[player.uniqueId] ?: return
        playerEffects.remove(effectType)
        if (playerEffects.isEmpty()) {
            activeEffects.remove(player.uniqueId)
        }

        val playerTasks = effectTasks[player.uniqueId]
        playerTasks?.get(effectType)?.let { taskId ->
            Bukkit.getScheduler().cancelTask(taskId)
            playerTasks.remove(effectType)
            if (playerTasks.isEmpty()) {
                effectTasks.remove(player.uniqueId)
            }
        }

        player.removePotionEffect(effectType)
    }
}