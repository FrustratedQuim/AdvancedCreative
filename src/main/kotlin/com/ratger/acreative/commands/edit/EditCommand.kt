package com.ratger.acreative.commands.edit

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class EditCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.EDIT) {
    private val parser = EditParsers()
    private val resolver = EditTargetResolver(hooker)
    private val show = EditShowService()
    private val validation = EditValidationService()
    private val service = EditService(resolver, validation, show, parser, EditMiniMessage())
    private val tabSupport = EditTabCompleterSupport(parser)

    override fun handle(player: Player, args: Array<out String>) {
        val action = parser.parseAction(args)
        if (action == null) {
            player.sendRichMessage("<red>Использование: /edit show | /edit reset <all|plugin> | /edit name|lore|component|enchant|tooltip|potion|head|attribute|consumable|death_protection|equippable|remainder|tool ...")
            return
        }

        val result = service.execute(player, action)
        result.lines.forEach(player::sendMessage)
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        if (sender !is Player) return emptyList()
        return tabSupport.complete(sender, args)
    }
}
