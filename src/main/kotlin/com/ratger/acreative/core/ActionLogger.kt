package com.ratger.acreative.core

import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class ActionLogger(
    private val hooker: FunctionHooker
) {
    private val prefix = "[ACreative/logger]"
    private val throttleState = ConcurrentHashMap<String, Long>()

    fun isEnabled(): Boolean = hooker.systemToggleService.isEnabled(ManagedSystem.LOGGER)

    fun info(message: String) {
        if (!isEnabled()) return
        hooker.plugin.logger.info("$prefix $message")
    }

    fun info(message: () -> String) {
        if (!isEnabled()) return
        hooker.plugin.logger.info("$prefix ${message()}")
    }

    fun warning(message: String) {
        if (!isEnabled()) return
        hooker.plugin.logger.warning("$prefix $message")
    }

    fun warning(message: () -> String) {
        if (!isEnabled()) return
        hooker.plugin.logger.warning("$prefix ${message()}")
    }

    fun severe(message: String) {
        if (!isEnabled()) return
        hooker.plugin.logger.severe("$prefix $message")
    }

    fun infoThrottled(key: String, intervalMs: Long, message: () -> String) {
        if (!isEnabled()) return
        if (!markThrottle(key, intervalMs)) return
        hooker.plugin.logger.info("$prefix ${message()}")
    }

    fun warningThrottled(key: String, intervalMs: Long, message: () -> String) {
        if (!isEnabled()) return
        if (!markThrottle(key, intervalMs)) return
        hooker.plugin.logger.warning("$prefix ${message()}")
    }

    fun playerRef(player: Player): String = "${player.name} (${player.uniqueId})"

    fun locationRef(location: Location?): String {
        if (location == null) {
            return "location=unknown"
        }

        val worldName = location.world?.name ?: "unknown"
        return "world=$worldName x=${location.x.pretty()} y=${location.y.pretty()} z=${location.z.pretty()} yaw=${location.yaw.pretty()} pitch=${location.pitch.pretty()}"
    }

    fun viewerCountRef(count: Int): String = "viewers=$count"

    private fun markThrottle(key: String, intervalMs: Long): Boolean {
        if (intervalMs <= 0L) return true
        val now = System.currentTimeMillis()
        val last = throttleState[key]
        if (last != null && now - last < intervalMs) {
            return false
        }

        if (throttleState.size > MAX_THROTTLE_KEYS) {
            throttleState.clear()
        }
        throttleState[key] = now
        return true
    }

    private fun Double.pretty(): String = String.format(Locale.US, "%.2f", this)
    private fun Float.pretty(): String = String.format(Locale.US, "%.2f", this)

    private companion object {
        const val MAX_THROTTLE_KEYS = 2_048
    }
}
