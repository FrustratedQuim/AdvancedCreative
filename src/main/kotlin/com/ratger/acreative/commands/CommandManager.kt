package com.ratger.acreative.commands

import com.ratger.acreative.commands.framework.CommandCooldownService
import com.ratger.acreative.commands.framework.ExecutableCommand
import com.ratger.acreative.commands.framework.PluginCommandType
import com.ratger.acreative.commands.handlers.*
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class CommandManager(functionHooker: FunctionHooker) : CommandExecutor, TabCompleter {

    val cooldownService = CommandCooldownService(functionHooker.configManager)

    private val handlers: Map<PluginCommandType, ExecutableCommand> = listOf(
        AhelpCommand(functionHooker),
        SitCommand(functionHooker),
        LayCommand(functionHooker),
        CrawlCommand(functionHooker),
        HideCommand(functionHooker),
        SneezeCommand(functionHooker),
        GlideCommand(functionHooker),
        GravityCommand(functionHooker),
        ResizeCommand(functionHooker),
        StrengthCommand(functionHooker),
        HealthCommand(functionHooker),
        FreezeCommand(functionHooker),
        GlowCommand(functionHooker),
        SpitCommand(functionHooker),
        PissCommand(functionHooker),
        DisguiseCommand(functionHooker),
        EffectsCommand(functionHooker),
        SlapCommand(functionHooker),
        SitheadCommand(functionHooker),
        ItemdbCommand(functionHooker)
    ).associateBy { it.type }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val type = PluginCommandType.fromId(command.name) ?: return false
        val handler = handlers[type] ?: return false
        return handler.execute(sender, args)
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        val type = PluginCommandType.fromId(command.name) ?: return emptyList()
        val handler = handlers[type] ?: return emptyList()
        return handler.tabComplete(sender, args)
    }
}
