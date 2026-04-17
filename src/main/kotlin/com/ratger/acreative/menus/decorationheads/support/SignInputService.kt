package com.ratger.acreative.menus.decorationheads.support

import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.input.Input
import ru.violence.coreapi.bukkit.api.input.InputRegisterResult

class SignInputService(
    private val plugin: org.bukkit.plugin.Plugin
) {
    fun open(
        player: Player,
        templateLines: Array<String>,
        onSubmit: (Player, String?) -> Unit,
        onLeave: (Player) -> Unit
    ) {
        val registerResult = Input.sign().builder(plugin, player)
            .lines(templateLines)
            .onInput { p, lines ->
                val firstMeaningful = lines
                    .mapIndexedNotNull { index, line ->
                        line.trim()
                            .takeIf { it.isNotEmpty() }
                            ?.takeUnless { it == templateLines.getOrNull(index) }
                    }
                    .firstOrNull()
                onSubmit(p, firstMeaningful)
            }
            .onLeave(onLeave)
            .async(true)
            .register(true)

        if (registerResult != InputRegisterResult.SUCCESS) {
            onLeave(player)
        }
    }
}
