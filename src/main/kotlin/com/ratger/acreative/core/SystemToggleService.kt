package com.ratger.acreative.core

import java.util.EnumMap

class SystemToggleService(
    private val hooker: FunctionHooker
) {
    data class Status(
        val system: ManagedSystem,
        val enabled: Boolean
    )

    private val states = EnumMap<ManagedSystem, Boolean>(ManagedSystem::class.java)

    init {
        reload()
    }

    fun reload() {
        ManagedSystem.entries.forEach { system ->
            states[system] = hooker.configManager.config.getBoolean(path(system), defaultEnabled(system))
        }
    }

    fun isEnabled(system: ManagedSystem): Boolean = states[system] ?: true

    fun toggle(system: ManagedSystem): Boolean = setEnabled(system, !isEnabled(system))

    fun setEnabled(system: ManagedSystem, enabled: Boolean): Boolean {
        states[system] = enabled
        hooker.configManager.set(path(system), enabled)
        hooker.configManager.save()

        if (!enabled) {
            releaseActiveUsage(system)
        }

        return enabled
    }

    fun statuses(): List<Status> = ManagedSystem.entries.map { Status(it, isEnabled(it)) }

    private fun releaseActiveUsage(system: ManagedSystem) {
        when (system) {
            ManagedSystem.EDIT -> hooker.menuServiceOrNull()?.closeAllItemEditorSessions()
            ManagedSystem.PAINT -> hooker.paintManagerOrNull()?.releaseAll()
            ManagedSystem.DECORATION_BANNERS -> hooker.bannerSubsystemOrNull()?.menuService?.closeAllSessions()
            ManagedSystem.DECORATION_HEADS -> hooker.subsystemOrNull()?.menuService?.closeAllSessions()
            ManagedSystem.PLOT_EDIT,
            ManagedSystem.PLOT_MASSCLAIM,
            ManagedSystem.LOGGER -> Unit
        }
    }

    private fun path(system: ManagedSystem): String = "$CONFIG_ROOT.${system.id}"

    private fun defaultEnabled(system: ManagedSystem): Boolean = system != ManagedSystem.LOGGER

    private companion object {
        const val CONFIG_ROOT = "systems"
    }
}
