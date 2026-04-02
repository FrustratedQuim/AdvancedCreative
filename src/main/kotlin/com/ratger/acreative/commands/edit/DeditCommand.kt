package com.ratger.acreative.commands.edit

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.itemedit.core.ItemEditingService
import com.ratger.acreative.itemedit.experimental.ComponentsService
import com.ratger.acreative.itemedit.head.HeadProfileService
import com.ratger.acreative.itemedit.meta.MetaActionsApplier
import com.ratger.acreative.itemedit.meta.MiniMessageParser
import com.ratger.acreative.itemedit.show.ShowService
import com.ratger.acreative.itemedit.validation.ValidationService
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class DeditCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.DEDIT) {
    private val parser = EditParsers()
    private val resolver = EditTargetResolver()
    private val show = ShowService()
    private val validation = ValidationService()
    private val service = ItemEditingService(
        targetResolver = resolver,
        validationService = validation,
        showService = show,
        metaActionsApplier = MetaActionsApplier(hooker.plugin, parser, MiniMessageParser()),
        componentsService = ComponentsService(),
        headProfileService = HeadProfileService(hooker.plugin, resolver)
    )
    private val tabSupport = EditTabCompleterSupport(parser)

    override fun handle(player: Player, args: Array<out String>) {
        val action = parser.parseAction(args)
        if (action == null) {
            when (args.firstOrNull()?.lowercase()) {
                "trim" -> player.sendRichMessage("<red>Использование: /dedit trim set <pattern_template_id> <material_id> | /dedit trim clear")
                "pot" -> player.sendRichMessage("<red>Использование: /dedit pot clear | /dedit pot set <back> <left> <right> <front> | /dedit pot side <back|left|right|front> <item_id>")
                "container" -> player.sendRichMessage("<red>Использование: /dedit container <index>")
                "frame" -> player.sendRichMessage("<red>Использование: /dedit frame invisible <on|off>")
                else -> player.sendRichMessage("<red>Использование: /dedit show | /dedit reset <all> | /dedit id <item> | /dedit name|lore|component|enchant|tooltip|potion|head(clear|from_texture|from_name|from_online)|attribute|consumable|death_protection|equippable|remainder|tool|lock|container|trim|pot|frame ...")
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
