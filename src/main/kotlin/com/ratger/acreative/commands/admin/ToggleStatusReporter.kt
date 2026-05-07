package com.ratger.acreative.commands.admin

import com.ratger.acreative.core.FunctionHooker
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.entity.Player

class ToggleStatusReporter(
    private val hooker: FunctionHooker
) {
    private val mini = MiniMessage.miniMessage()

    fun show(player: Player) {
        player.sendMessage(mini.deserialize("<#FFD700><st>                          </st><<#FFE68A><b> Toggle Status </b><#FFD700>><st>                         </st>"))
        player.sendMessage(mini.deserialize("<#FFE68A><b>Актуальные:</b>"))

        hooker.systemToggleService.statuses().forEach { status ->
            val stateText = if (status.enabled) "<#00FF40>Включено" else "<#FF1500>Отключено"
            val line = "<#C7A300> ● <#FFE68A>${status.system.displayName} <#EDC800>- $stateText"
            player.sendMessage(mini.deserialize(line))
        }

        player.sendMessage(mini.deserialize("<#FFD700><st>                                                                             </st>"))
    }
}
