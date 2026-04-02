package com.ratger.acreative.commands.edit

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.entity.Player

class EditCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.EDIT) {
    override fun handle(player: Player, args: Array<out String>) {
        hooker.menuService.openItemEditor(player)
    }
}
