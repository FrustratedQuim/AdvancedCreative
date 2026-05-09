package com.ratger.acreative.core

import org.bukkit.Bukkit

class TickScheduler(
    private val hooker: FunctionHooker
) {
    private val plugin = hooker.plugin

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

    private companion object {
        const val SLOW_TASK_THRESHOLD_MS = 10L
        const val SLOW_TICK_THRESHOLD_MS = 25L
    }

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
        val traceSlowTasks = hooker.actionLogger.isEnabled()
        val tickStartNs = if (traceSlowTasks) System.nanoTime() else 0L
        val dueTasks = synchronized(lock) {
            currentTick++
            lastTickAtMs = System.currentTimeMillis()
            tasks.values
                .filter { it.nextRunTick <= currentTick }
                .map { it.id }
        }

        for (taskId in dueTasks) {
            val task = synchronized(lock) { tasks[taskId] } ?: continue
            val taskStartNs = if (traceSlowTasks) System.nanoTime() else 0L
            try {
                task.action.invoke()
            } catch (t: Throwable) {
                plugin.logger.severe("TickScheduler task #${task.id} failed: ${t.message}")
                t.printStackTrace()
            }
            if (traceSlowTasks) {
                val taskDurationMs = (System.nanoTime() - taskStartNs) / 1_000_000
                if (taskDurationMs >= SLOW_TASK_THRESHOLD_MS) {
                    hooker.actionLogger.warning(
                        "TickScheduler task #${task.id} took ${taskDurationMs}ms"
                    )
                }
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

        if (traceSlowTasks) {
            val tickDurationMs = (System.nanoTime() - tickStartNs) / 1_000_000
            if (tickDurationMs >= SLOW_TICK_THRESHOLD_MS) {
                hooker.actionLogger.warning(
                    "TickScheduler processed ${dueTasks.size} task(s) in ${tickDurationMs}ms"
                )
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
