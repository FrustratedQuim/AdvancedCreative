package com.ratger.acreative.core

import com.ratger.acreative.AdvancedCreative
import org.bukkit.Bukkit

class TickScheduler(
    private val plugin: AdvancedCreative
) {

    private data class ScheduledTask(
        val id: Int,
        var nextRunTick: Long,
        val periodTicks: Long?,
        val action: () -> Unit
    )

    private val lock = Any()
    private val tasks = linkedMapOf<Int, ScheduledTask>()
    private var bukkitTickerTaskId: Int? = null
    private var currentTick: Long = 0L
    private var nextTaskId: Int = 1
    private var lastTickAtMs: Long = 0L

    fun start() {
        ensureTickerRunning(forceHealthCheck = false)
    }

    fun shutdown() {
        val tickerTaskId = synchronized(lock) {
            tasks.clear()
            bukkitTickerTaskId.also { bukkitTickerTaskId = null }
        }
        tickerTaskId?.let { Bukkit.getScheduler().cancelTask(it) }
    }

    fun runNow(action: () -> Unit): Int {
        return runLater(0L, action)
    }

    fun runLater(delayTicks: Long, action: () -> Unit): Int {
        ensureTickerRunning(forceHealthCheck = true)
        val normalizedDelay = delayTicks.coerceAtLeast(0L)
        return synchronized(lock) {
            val id = nextTaskId++
            tasks[id] = ScheduledTask(
                id = id,
                nextRunTick = currentTick + normalizedDelay,
                periodTicks = null,
                action = action
            )
            id
        }
    }

    fun runRepeating(initialDelayTicks: Long, periodTicks: Long, action: () -> Unit): Int {
        ensureTickerRunning(forceHealthCheck = true)
        return synchronized(lock) {
            val id = nextTaskId++
            tasks[id] = ScheduledTask(
                id = id,
                nextRunTick = currentTick + initialDelayTicks.coerceAtLeast(0L),
                periodTicks = periodTicks.coerceAtLeast(1L),
                action = action
            )
            id
        }
    }

    fun cancel(taskId: Int) {
        synchronized(lock) {
            tasks.remove(taskId)
        }
    }

    private fun tick() {
        val dueTasks = synchronized(lock) {
            currentTick++
            lastTickAtMs = System.currentTimeMillis()
            tasks.values
                .filter { it.nextRunTick <= currentTick }
                .map { it.id }
        }

        for (taskId in dueTasks) {
            val task = synchronized(lock) { tasks[taskId] } ?: continue
            try {
                task.action.invoke()
            } catch (t: Throwable) {
                plugin.logger.severe("TickScheduler task #${task.id} failed: ${t.message}")
                t.printStackTrace()
            }

            synchronized(lock) {
                val activeTask = tasks[taskId] ?: return@synchronized
                if (activeTask.periodTicks == null) {
                    tasks.remove(taskId)
                } else {
                    activeTask.nextRunTick = currentTick + activeTask.periodTicks
                }
            }
        }
    }

    private fun ensureTickerRunning(forceHealthCheck: Boolean) {
        if (!plugin.isEnabled) return
        synchronized(lock) {
            val isStalled = forceHealthCheck && bukkitTickerTaskId != null &&
                tasks.isNotEmpty() &&
                lastTickAtMs > 0L &&
                System.currentTimeMillis() - lastTickAtMs > 3_000L

            if (isStalled) {
                bukkitTickerTaskId?.let { Bukkit.getScheduler().cancelTask(it) }
                bukkitTickerTaskId = null
            }

            if (bukkitTickerTaskId != null) return

            lastTickAtMs = System.currentTimeMillis()
            bukkitTickerTaskId = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
                tick()
            }, 1L, 1L).taskId
        }
    }
}
