package com.ratger.acreative.commands.resize

import com.ratger.acreative.commands.common.NumericAttributeManager
import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.MessageKey
import com.ratger.acreative.utils.PlayerStateManager.PlayerStateType
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import java.math.BigDecimal
import kotlin.math.abs

class ResizeManager(hooker: FunctionHooker) : NumericAttributeManager(hooker) {

    companion object {
        private const val MIN_SCALE_VALUE = 0.1
        private const val MAX_SCALE_VALUE = 15.0
        private const val DEFAULT_SCALE_VALUE = 1.0
        private const val RESIZE_THRESHOLD = 3.0
        private const val TRANSITION_STEPS = 5
        private const val TRANSITION_PERIOD_TICKS = 1L
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

    fun applyEffectFromCommand(player: Player, arg: String?) {
        if (arg == null) {
            if (trackedPlayers.containsKey(player)) {
                removeEffectSmooth(player)
            } else {
                hooker.messageManager.sendChat(player, usageMessageKey)
            }
            return
        }

        val value = parseResizeValue(arg) ?: run {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_UNKNOWN_VALUE)
            return
        }

        if (value == defaultValue) {
            removeEffectSmooth(player)
            return
        }

        playerStateType.let { hooker.playerStateManager.activateState(player, it) }
        applyAttribute(player, value)
        trackedPlayers[player] = value

        hooker.messageManager.sendChat(
            player,
            successSetMessageKey,
            variables = mapOf("value" to formatResizeValue(value))
        )
    }

    override fun onAfterEffectRemoved(player: Player) {
        hooker.playerStateManager.refreshPlayerPose(player)
    }

    private fun resetAttributes(player: Player) {
        applyScaleAttributes(player, DEFAULT_SCALE_VALUE)
    }

    private fun removeEffectSmooth(player: Player) {
        if (!trackedPlayers.containsKey(player)) return

        startSmoothResize(player, DEFAULT_SCALE_VALUE)
        trackedPlayers.remove(player)
        hooker.playerStateManager.deactivateState(player, playerStateType)
        onAfterEffectRemoved(player)
        hooker.messageManager.sendChat(player, successResetMessageKey)
    }

    fun smoothTransitionScale(player: Player, targetValue: Double, onComplete: (() -> Unit)? = null) {
        startSmoothResize(player, targetValue, onComplete)
    }

    private fun applyScaleAttributes(player: Player, value: Double) {
        player.getAttribute(Attribute.SCALE)?.baseValue = value

        if (value >= RESIZE_THRESHOLD) {
            val interactDistance = calculateInteractDistance(value)
            player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE)?.baseValue = 4.5 + interactDistance
            player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE)?.baseValue = 3.0 + interactDistance

            val stepHeight = calculateStepHeight(value)
            player.getAttribute(Attribute.STEP_HEIGHT)?.baseValue = 0.6 + stepHeight

            val speed = calculateSpeed(value)
            player.getAttribute(Attribute.MOVEMENT_SPEED)?.baseValue = 0.1 + speed
            return
        }

        player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE)?.baseValue = 4.5
        player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE)?.baseValue = 3.0
        player.getAttribute(Attribute.STEP_HEIGHT)?.baseValue = 0.6
        player.getAttribute(Attribute.MOVEMENT_SPEED)?.baseValue = 0.1
    }

    private fun startSmoothResize(
        player: Player,
        targetValue: Double,
        onComplete: (() -> Unit)? = null
    ) {
        cancelResizeTask(player)

        val startValue = player.getAttribute(Attribute.SCALE)?.baseValue ?: DEFAULT_SCALE_VALUE
        if (abs(startValue - targetValue) < 0.0001) {
            applyScaleAttributes(player, targetValue)
            onComplete?.invoke()
            return
        }

        val stepDelta = (targetValue - startValue) / TRANSITION_STEPS
        var currentStep = 0
        val taskId = hooker.tickScheduler.runRepeating(0L, TRANSITION_PERIOD_TICKS) {
            if (!player.isOnline) {
                cancelResizeTask(player)
                onComplete?.invoke()
                return@runRepeating
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
                onComplete?.invoke()
            }
        }

        activeResizeTasks[player] = taskId
    }

    private fun cancelResizeTask(player: Player) {
        activeResizeTasks.remove(player)?.let(hooker.tickScheduler::cancel)
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

    private fun parseResizeValue(arg: String): Double? {
        if (arg.equals("basic", ignoreCase = true)) return defaultValue
        if (arg.startsWith("-")) return normalizeNegativeInput()

        val cleanedArg = arg.replace(",", ".").trim()
        val numericStr = when {
            cleanedArg.matches(Regex("^\\d+$")) -> cleanedArg
            cleanedArg.matches(Regex("^\\d*\\.\\d+$")) -> cleanedArg
            else -> return null
        }

        val value = numericStr.toDoubleOrNull() ?: return null
        return normalizeParsedValue(value)
    }

    private fun formatResizeValue(value: Double): String {
        return BigDecimal.valueOf(value)
            .stripTrailingZeros()
            .toPlainString()
    }
}
