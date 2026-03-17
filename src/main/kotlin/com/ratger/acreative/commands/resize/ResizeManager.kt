package com.ratger.acreative.commands.resize

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.NumericAttributeManager
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.MessageKey
import com.ratger.acreative.utils.PlayerStateManager.PlayerStateType
import org.bukkit.Bukkit
import org.bukkit.attribute.Attribute
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import kotlin.math.abs

class ResizeManager(hooker: FunctionHooker) : NumericAttributeManager(hooker) {

    companion object {
        private const val MIN_SCALE_VALUE = 0.1
        private const val MAX_SCALE_VALUE = 15.0
        private const val DEFAULT_SCALE_VALUE = 1.0
        private const val RESIZE_THRESHOLD = 3.0
        private const val TRANSITION_STEPS = 5
        private const val TRANSITION_PERIOD_TICKS = 1L
        private val RESIZE_SUGGESTIONS = listOf("0.1", "0.5", "1.0", "1.5", "5.0", "10.0", "15.0", "basic")
    }

    override val minValue: Double = MIN_SCALE_VALUE
    override val maxValue: Double = MAX_SCALE_VALUE
    override val defaultValue: Double = DEFAULT_SCALE_VALUE
    override val usageMessageKey: MessageKey = MessageKey.USAGE_RESIZE
    override val successSetMessageKey: MessageKey = MessageKey.SUCCESS_RESIZE_SET
    override val successResetMessageKey: MessageKey = MessageKey.SUCCESS_RESIZE_RESET
    override val trackedPlayers: MutableMap<Player, Double> = mutableMapOf()
    private val activeResizeTasks: MutableMap<Player, Int> = mutableMapOf()
    val scaledPlayers: MutableMap<Player, Double> get() = trackedPlayers
    override val playerStateType: PlayerStateType = PlayerStateType.CUSTOM_SIZE

    override fun normalizeParsedValue(value: Double): Double {
        return when {
            value > maxValue -> maxValue
            value < minValue && value != 0.0 -> minValue
            value == 0.0 || value == defaultValue -> defaultValue
            else -> value
        }
    }

    override fun normalizeNegativeInput(): Double = minValue

    override fun applyAttribute(player: Player, value: Double) {
        startSmoothResize(player, value)
    }

    override fun removeAttribute(player: Player) {
        cancelResizeTask(player)
        resetAttributes(player)
    }

    override fun onAfterEffectRemoved(player: Player) {
        hooker.playerStateManager.refreshPlayerPose(player)
    }

    fun tabCompletions(args: Array<out String>): List<String> {
        return if (args.size == 1) {
            RESIZE_SUGGESTIONS.filter { it.startsWith(args[0], ignoreCase = true) }
        } else {
            emptyList()
        }
    }

    private fun resetAttributes(player: Player) {
        applyScaleAttributes(player, DEFAULT_SCALE_VALUE)
    }

    private fun applyScaleAttributes(player: Player, value: Double) {
        player.getAttribute(Attribute.GENERIC_SCALE)?.baseValue = value

        if (value >= RESIZE_THRESHOLD) {
            val interactDistance = calculateInteractDistance(value)
            player.getAttribute(Attribute.PLAYER_BLOCK_INTERACTION_RANGE)?.baseValue = 4.5 + interactDistance
            player.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE)?.baseValue = 3.0 + interactDistance

            val stepHeight = calculateStepHeight(value)
            player.getAttribute(Attribute.GENERIC_STEP_HEIGHT)?.baseValue = 0.6 + stepHeight

            val speed = calculateSpeed(value)
            player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)?.baseValue = 0.1 + speed
            return
        }

        player.getAttribute(Attribute.PLAYER_BLOCK_INTERACTION_RANGE)?.baseValue = 4.5
        player.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE)?.baseValue = 3.0
        player.getAttribute(Attribute.GENERIC_STEP_HEIGHT)?.baseValue = 0.6
        player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)?.baseValue = 0.1
    }

    private fun startSmoothResize(player: Player, targetValue: Double) {
        hooker.utils.checkCrawlUncrawl(player)
        cancelResizeTask(player)

        val startValue = player.getAttribute(Attribute.GENERIC_SCALE)?.baseValue ?: DEFAULT_SCALE_VALUE
        if (abs(startValue - targetValue) < 0.0001) {
            applyScaleAttributes(player, targetValue)
            return
        }

        val stepDelta = (targetValue - startValue) / TRANSITION_STEPS
        var currentStep = 0
        val taskId = Bukkit.getScheduler().runTaskTimer(hooker.plugin, Runnable {
            if (!player.isOnline) {
                cancelResizeTask(player)
                return@Runnable
            }

            currentStep++
            val currentValue = if (currentStep >= TRANSITION_STEPS) {
                targetValue
            } else {
                startValue + stepDelta * currentStep
            }

            applyScaleAttributes(player, currentValue)

            if (currentStep >= TRANSITION_STEPS) {
                cancelResizeTask(player)
            }
        }, 0L, TRANSITION_PERIOD_TICKS).taskId

        activeResizeTasks[player] = taskId
    }

    private fun cancelResizeTask(player: Player) {
        activeResizeTasks.remove(player)?.let(Bukkit.getScheduler()::cancelTask)
    }

    private fun calculateInteractDistance(scale: Double): Double {
        return if (scale >= RESIZE_THRESHOLD) {
            1.0 + (scale - RESIZE_THRESHOLD) * (25.0 - 1.0) / (15.0 - RESIZE_THRESHOLD)
        } else 0.0
    }

    private fun calculateStepHeight(scale: Double): Double {
        return if (scale >= RESIZE_THRESHOLD) {
            2.0 + (scale - RESIZE_THRESHOLD) * (8.0 - 2.0) / (15.0 - RESIZE_THRESHOLD)
        } else 0.0
    }

    private fun calculateSpeed(scale: Double): Double {
        return if (scale >= RESIZE_THRESHOLD) {
            0.04 + (scale - RESIZE_THRESHOLD) * (0.15 - 0.04) / (15.0 - RESIZE_THRESHOLD)
        } else 0.0
    }
}

class ResizeCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.RESIZE) {
    override fun handle(player: Player, args: Array<out String>) = hooker.resizeManager.applyEffect(player, args.firstOrNull())

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return hooker.resizeManager.tabCompletions(args)
    }
}
