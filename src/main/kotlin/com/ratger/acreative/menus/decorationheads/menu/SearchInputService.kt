package com.ratger.acreative.menus.decorationheads.menu

import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.input.Input
import ru.violence.coreapi.bukkit.api.input.InputRegisterResult

class SearchInputService(
    private val plugin: org.bukkit.plugin.Plugin,
    private val onSubmit: (Player, String?) -> Unit,
    private val onLeave: (Player) -> Unit
) {
    fun open(player: Player) {
        val templateLines = arrayOf("", "↑ Что ищем? ↑", "Укажите на", "английском.")

        val registerResult = Input.sign().builder(plugin, player)
            .lines(templateLines)
            .onInput { p, lines ->
                val firstLine = lines.firstOrNull()?.trim().orEmpty()
                val query = firstLine.takeIf { it.isNotEmpty() }
                    ?: lines
                        .mapIndexedNotNull { index, line ->
                            line.trim()
                                .takeIf { it.isNotEmpty() }
                                ?.takeUnless { it == templateLines.getOrNull(index) }
                        }
                        .firstOrNull()

                onSubmit(p, query)
            }
            .onLeave(onLeave)
            .async(true)
            .register(true)

        if (registerResult != InputRegisterResult.SUCCESS) {
            onLeave(player)
        }
    }
}
