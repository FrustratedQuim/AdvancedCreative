package com.ratger.acreative.decorationheads.menu

import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.input.Input

class DecorationHeadsSearchInputService(
    private val plugin: org.bukkit.plugin.Plugin,
    private val onSubmit: (Player, String?) -> Unit,
    private val onLeave: (Player) -> Unit
) {
    fun open(player: Player) {
        Input.sign().builder(plugin, player)
            .lines(arrayOf("", "↑ Что ищем? ↑", "Укажите на", "английском."))
            .onInput { p, lines ->
                val query = lines.firstOrNull { it.isNotBlank() }?.trim()
                onSubmit(p, query)
            }
            .onLeave(onLeave)
            .async(true)
            .register()
    }
}
