package com.ratger.acreative.commands.edit

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class EditCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.EDIT) {
    private val parser = EditParsers()
    private val resolver = EditTargetResolver()
    private val show = EditShowService()
    private val validation = EditValidationService()
    private val service = EditService(hooker.plugin, resolver, validation, show, parser, EditMiniMessage())
    private val tabSupport = EditTabCompleterSupport(parser)

    override fun handle(player: Player, args: Array<out String>) {
        val action = parser.parseAction(args)
        if (action == null) {
            when (args.firstOrNull()?.lowercase()) {
                "trim" -> player.sendRichMessage("<red>Использование: /edit trim set <pattern_template_id> <material_id> | /edit trim clear")
                "pot" -> player.sendRichMessage("<red>Использование: /edit pot clear | /edit pot set <back> <left> <right> <front> | /edit pot side <back|left|right|front> <item_id>")
                "container" -> player.sendRichMessage("<red>Использование: /edit container <index>")
                else -> player.sendRichMessage("<red>Использование: /edit show | /edit reset <all> | /edit id <item> | /edit name|lore|component|enchant|tooltip|potion|head(clear|from_texture|from_name|from_online)|attribute|consumable|death_protection|equippable|remainder|tool|lock|container|trim|pot ...")
            }
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
