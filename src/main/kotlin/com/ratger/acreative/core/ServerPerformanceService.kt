package com.ratger.acreative.core

import org.bukkit.Bukkit

class ServerPerformanceService {

    fun isStableForTickSensitiveActivation(minTps: Double): Boolean {
        return currentPrimaryTps() > minTps
    }

    fun currentPrimaryTps(): Double {
        val currentTps = Bukkit.getTPS().firstOrNull() ?: DEFAULT_TPS
        if (!currentTps.isFinite()) {
            return DEFAULT_TPS
        }
        return currentTps.coerceAtLeast(0.0)
    }

    private companion object {
        const val DEFAULT_TPS = 20.0
    }
}
