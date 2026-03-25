package com.ratger.acreative.commands

import com.ratger.acreative.commands.crawl.CrawlCommand
import com.ratger.acreative.commands.disguise.DisguiseCommand
import com.ratger.acreative.commands.edit.EditCommand
import com.ratger.acreative.commands.effects.EffectsCommand
import com.ratger.acreative.commands.freeze.FreezeCommand
import com.ratger.acreative.commands.glide.GlideCommand
import com.ratger.acreative.commands.glow.GlowCommand
import com.ratger.acreative.commands.gravity.GravityCommand
import com.ratger.acreative.commands.grab.GrabCommand
import com.ratger.acreative.commands.health.HealthCommand
import com.ratger.acreative.commands.hide.HideCommand
import com.ratger.acreative.commands.itemdb.ItemdbCommand
import com.ratger.acreative.commands.jar.JarCommand
import com.ratger.acreative.commands.lay.LayCommand
import com.ratger.acreative.commands.piss.PissCommand
import com.ratger.acreative.commands.resize.ResizeCommand
import com.ratger.acreative.commands.sit.SitCommand
import com.ratger.acreative.commands.sit.SitheadCommand
import com.ratger.acreative.commands.slap.SlapCommand
import com.ratger.acreative.commands.sneeze.SneezeCommand
import com.ratger.acreative.commands.spit.SpitCommand
import com.ratger.acreative.commands.strength.StrengthCommand
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
        JarCommand(functionHooker),
        GrabCommand(functionHooker),
        SlapCommand(functionHooker),
        SitheadCommand(functionHooker),
        ItemdbCommand(functionHooker),
        EditCommand(functionHooker)
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
