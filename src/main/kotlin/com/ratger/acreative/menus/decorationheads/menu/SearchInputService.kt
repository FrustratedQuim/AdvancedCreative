package com.ratger.acreative.menus.decorationheads.menu

import com.ratger.acreative.menus.decorationheads.support.SignInputService
import org.bukkit.entity.Player

class SearchInputService(
    private val signInputService: SignInputService,
    private val onSubmit: (Player, String?) -> Unit,
    private val onLeave: (Player) -> Unit
) {
    fun open(player: Player) {
        signInputService.open(
            player = player,
            templateLines = arrayOf("", "↑ Что ищем? ↑", "", ""),
            onSubmit = onSubmit,
            onLeave = onLeave
        )
    }
}
