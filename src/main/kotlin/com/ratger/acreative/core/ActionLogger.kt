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
    private enum class LogSeverity { INFO, WARNING, SEVERE }

    fun isEnabled(): Boolean = hooker.systemToggleService.isEnabled(ManagedSystem.LOGGER)

    fun info(message: String) = emit(LogSeverity.INFO) { message }

    fun info(message: () -> String) = emit(LogSeverity.INFO, message)

    fun auditInfo(message: String) = emit(LogSeverity.INFO) { message }

    fun auditInfo(message: () -> String) = emit(LogSeverity.INFO, message)

    fun warning(message: String) = emit(LogSeverity.WARNING) { message }

    fun warning(message: () -> String) = emit(LogSeverity.WARNING, message)

    fun auditWarning(message: String) = emit(LogSeverity.WARNING) { message }

    fun auditWarning(message: () -> String) = emit(LogSeverity.WARNING, message)

    fun severe(message: String) = emit(LogSeverity.SEVERE) { message }

    fun infoThrottled(key: String, intervalMs: Long, message: () -> String) =
        emitThrottled(LogSeverity.INFO, key, intervalMs, message)

    fun warningThrottled(key: String, intervalMs: Long, message: () -> String) =
        emitThrottled(LogSeverity.WARNING, key, intervalMs, message)

    fun playerRef(player: Player): String = "${player.name} (${player.uniqueId})"

    fun locationRef(location: Location?): String {
        if (location == null) {
            return "location=unknown"
        }

        val worldName = location.world?.name ?: "unknown"
        return "world=$worldName x=${location.x.pretty()} y=${location.y.pretty()} z=${location.z.pretty()} yaw=${location.yaw.pretty()} pitch=${location.pitch.pretty()}"
    }

    fun viewerCountRef(count: Int): String = "viewers=$count"

    fun commandRef(label: String, args: Array<out String>): String {
        val commandArgs = if (args.isEmpty()) "" else " ${args.joinToString(" ")}"
        return "/$label$commandArgs"
    }

    private fun emit(severity: LogSeverity, message: () -> String) {
        if (!isEnabled()) return
        val formattedMessage = "$prefix ${message()}"
        when (severity) {
            LogSeverity.INFO -> hooker.plugin.logger.info(formattedMessage)
            LogSeverity.WARNING -> hooker.plugin.logger.warning(formattedMessage)
            LogSeverity.SEVERE -> hooker.plugin.logger.severe(formattedMessage)
        }
    }

    private fun emitThrottled(severity: LogSeverity, key: String, intervalMs: Long, message: () -> String) {
        if (!isEnabled()) return
        if (!markThrottle(key, intervalMs)) return
        emit(severity, message)
    }

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
