package com.ratger.acreative.integration.plotsquared.commands

import com.plotsquared.core.PlotSquared
import com.plotsquared.core.player.PlotPlayer
import com.plotsquared.core.plot.Plot
import com.ratger.acreative.core.ManagedSystem
import com.ratger.acreative.core.MessageKey
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.PluginCommand
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.util.BukkitHelper
import ru.violence.coreapi.bukkit.api.util.ext.user
import java.lang.Runnable
import java.util.Locale
import java.util.UUID

class PlotCommandService(
    private val hooker: FunctionHooker
) : CommandExecutor, TabCompleter {

    private data class RegisteredRootCommand(
        val command: PluginCommand,
        val executor: CommandExecutor,
        val tabCompleter: TabCompleter?
    )

    private data class CsvSegments(
        val prefix: String,
        val current: String
    )

    private val registeredCommands = LinkedHashMap<String, RegisteredRootCommand>()
    private val ownerSuggestions = PlotOwnerSuggestionService()
    private val ownerExtensionService = PlotOwnerService()
    private val massClaimService = PlotMassClaimService(hooker)
    private val usageInfoService = PlotUsageInfoService()

    fun install() {
        if (!isPlotSquaredEnabled()) {
            return
        }

        ROOT_ALIASES.forEach { alias ->
            val pluginCommand = hooker.plugin.server.getPluginCommand(alias) ?: return@forEach
            if (registeredCommands.containsKey(pluginCommand.name.lowercase(Locale.ROOT))) {
                return@forEach
            }

            val originalExecutor = pluginCommand.executor
            val originalTabCompleter = pluginCommand.tabCompleter
            registeredCommands[pluginCommand.name.lowercase(Locale.ROOT)] = RegisteredRootCommand(
                command = pluginCommand,
                executor = originalExecutor,
                tabCompleter = originalTabCompleter
            )
            pluginCommand.setExecutor(this)
            pluginCommand.tabCompleter = this
        }
    }

    fun uninstall() {
        registeredCommands.values.forEach { registered ->
            registered.command.setExecutor(registered.executor)
            registered.command.tabCompleter = registered.tabCompleter
        }
        registeredCommands.clear()
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val registered = registeredCommands[command.name.lowercase(Locale.ROOT)] ?: return false
        val player = sender as? Player ?: return registered.executor.onCommand(sender, command, label, args)
        if (handleCustomSubcommand(player, args)) {
            return true
        }
        val rewritten = rewriteArgs(player, args) ?: return true
        val handled = registered.executor.onCommand(sender, command, label, rewritten)
        invalidateHomeCount(PlotPlayer.from(player).uuid)
        hooker.plotAccessGuardService.schedulePostCommandAccessCheck(player, rewritten)
        return handled
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String>? {
        val registered = registeredCommands[command.name.lowercase(Locale.ROOT)] ?: return null
        val baseCompletions = registered.tabCompleter?.onTabComplete(sender, command, alias, args)
        val player = sender as? Player ?: return baseCompletions
        completeIntercepted(player, args, baseCompletions.orEmpty())?.let { return it }
        return baseCompletions
    }

    private fun rewriteArgs(player: Player, args: Array<out String>): Array<String>? {
        if (args.isEmpty()) {
            return copyArgs(args)
        }

        val commandIndex = resolveCommandIndex(args)
        val commandName = args.getOrNull(commandIndex)?.lowercase(Locale.ROOT) ?: return copyArgs(args)
        val rewritten = copyArgs(args)

        return when (commandName) {
            in KNOWN_USER_COMMANDS if args.size > commandIndex + 1 -> {
                rewriteCsvArgument(
                    player,
                    rewritten,
                    commandIndex + 1,
                    allowEveryone = commandName in EVERYONE_TARGET_COMMANDS,
                    resolver = ::resolveExistingUserName
                )
            }
            in VISIT_COMMANDS if args.size > commandIndex + 1 -> {
                rewritePlotOwnerArgument(rewritten, commandIndex + 1)
            }
            in KICK_COMMANDS if args.size > commandIndex + 1 -> {
                rewriteCsvArgument(player, rewritten, commandIndex + 1, allowEveryone = false) { input ->
                    resolvePlotScopedUserName(player, rewritten, input) { plot ->
                        (plot.playersInPlot
                            .mapNotNull { it.name.takeIf(String::isNotBlank) }
                            + hooker.plotAccessGuardService.deniedOnlinePlayerNamesInside(plot))
                    }
                }
            }
            in REMOVE_COMMANDS if args.size > commandIndex + 1 -> {
                rewriteCsvArgument(player, rewritten, commandIndex + 1, allowEveryone = false) { input ->
                    resolvePlotScopedUserName(player, rewritten, input) { plot ->
                        ownerSuggestions.resolveScopedNames(plot.members + plot.trusted + plot.denied)
                    }
                }
            }
            in LIST_COMMANDS if args.size > commandIndex + 2 && args[commandIndex + 1].equals("player", ignoreCase = true) -> {
                rewritePlotOwnerArgument(rewritten, commandIndex + 2)
            }
            "grant" if args.size > commandIndex + 2 && args[commandIndex + 1].lowercase(Locale.ROOT) in GRANT_PLAYER_SUBCOMMANDS -> {
                rewriteCsvArgument(
                    player,
                    rewritten,
                    commandIndex + 2,
                    allowEveryone = false,
                    resolver = ::resolveExistingUserName
                )
            }
            "set" if args.size > commandIndex + 2 && args[commandIndex + 1].lowercase(Locale.ROOT) in OWNER_SUBCOMMANDS -> {
                rewriteCsvArgument(
                    player,
                    rewritten,
                    commandIndex + 2,
                    allowEveryone = false,
                    resolver = ::resolveExistingUserName
                )
            }
            else -> rewritten
        }
    }

    private fun handleCustomSubcommand(player: Player, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            return false
        }

        val commandIndex = resolveCommandIndex(args)
        val subcommand = args.getOrNull(commandIndex)?.lowercase(Locale.ROOT) ?: return false
        return when (subcommand) {
            in USAGE_COMMANDS -> {
                if (!player.hasPermission(PLOT_USAGE_PERMISSION)) {
                    hooker.permissionManager.sendPermissionDenied(player, PLOT_USAGE_PERMISSION)
                    return true
                }
                sendUsageInfo(player, args.getOrNull(commandIndex + 1))
                true
            }
            in MASSCLAIM_COMMANDS -> {
                if (!hooker.systemToggleService.isEnabled(ManagedSystem.PLOT_MASSCLAIM)) {
                    hooker.messageManager.sendChat(player, MessageKey.SYSTEM_DISABLED)
                    return true
                }
                if (!player.hasPermission(PLOT_MASSCLAIM_PERMISSION)) {
                    hooker.permissionManager.sendPermissionDenied(player, PLOT_MASSCLAIM_PERMISSION)
                    return true
                }
                massClaimService.handle(player, args.getOrNull(commandIndex + 1), args.getOrNull(commandIndex + 2))
                true
            }
            in ADD_OWNER_COMMANDS -> {
                if (!player.hasPermission(PLOT_OWNER_MANAGE_PERMISSION)) {
                    hooker.permissionManager.sendPermissionDenied(player, PLOT_OWNER_MANAGE_PERMISSION)
                    return true
                }
                handleOwnerExtension(player, args, commandIndex, addOwner = true)
                true
            }
            in REMOVE_OWNER_COMMANDS -> {
                if (!player.hasPermission(PLOT_OWNER_MANAGE_PERMISSION)) {
                    hooker.permissionManager.sendPermissionDenied(player, PLOT_OWNER_MANAGE_PERMISSION)
                    return true
                }
                handleOwnerExtension(player, args, commandIndex, addOwner = false)
                true
            }
            else -> false
        }
    }


    private fun sendUsageInfo(player: Player, rawTargetName: String?) {
        val target = resolveUsageTarget(player, rawTargetName)
        val usage = usageInfoService.snapshot(PlotPlayer.from(target))
        val messageKey = if (target == player) MessageKey.PLOT_USAGE_INFO else MessageKey.PLOT_USAGE_OTHER_INFO
        /*
        val totalText = if (allowed >= Settings.Limit.MAX_PLOTS) "∞" else allowed.toString()
        hooker.messageManager.sendChat(
        */
        hooker.messageManager.sendChat(
            player,
            messageKey,
            mapOf(
                "player" to target.name,
                "occupied" to usage.occupied.toString(),
                "total" to usage.totalText
            )
        )
    }

    private fun resolveUsageTarget(player: Player, rawTargetName: String?): Player {
        val requestedName = rawTargetName?.trim().takeUnless { it.isNullOrEmpty() } ?: return player
        if (!player.hasPermission(PLOT_USAGE_OTHER_PERMISSION)) {
            return player
        }
        return Bukkit.getOnlinePlayers()
            .firstOrNull { it.name.equals(requestedName, ignoreCase = true) }
            ?: player
    }

    private fun completeIntercepted(player: Player, args: Array<out String>, baseCompletions: List<String>): List<String>? {
        if (args.isEmpty()) {
            return null
        }

        val commandIndex = resolveCommandIndex(args)
        val commandName = args.getOrNull(commandIndex)?.lowercase(Locale.ROOT) ?: return null
        if (args.size == commandIndex + 1) {
            val merged = mergeRootCompletions(
                args[commandIndex],
                baseCompletions
            )
            if (merged.isNotEmpty()) {
                return merged
            }
        }

        return when (commandName) {
            in KNOWN_USER_COMMANDS if args.size == commandIndex + 2 -> {
                completeCsv(args[commandIndex + 1], knownUserCommandCompletions(commandName))
            }
            in VISIT_COMMANDS if args.size == commandIndex + 2 -> {
                completeCsv(args[commandIndex + 1], ownerSuggestions.plotOwnerNames(MAX_TAB_RESULTS))
            }
            in VISIT_COMMANDS if args.size == commandIndex + 3 -> {
                ownerSuggestions.completeVisitTargets(args[commandIndex + 1], args[commandIndex + 2], MAX_TAB_RESULTS)
            }
            in HOME_COMMANDS if args.size == commandIndex + 2 -> {
                completeHomePages(player, args[commandIndex + 1])
            }
            in USAGE_COMMANDS if args.size == commandIndex + 2 && player.hasPermission(PLOT_USAGE_OTHER_PERMISSION) -> {
                completeCsv(args[commandIndex + 1], onlinePlayerNames())
            }
            in MASSCLAIM_COMMANDS if args.size == commandIndex + 2 -> {
                (1..PlotMassClaimService.MAX_MASSCLAIM_SIZE)
                    .map(Int::toString)
                    .filter { it.startsWith(args[commandIndex + 1], ignoreCase = true) }
            }
            in MASSCLAIM_COMMANDS if args.size == commandIndex + 3 -> {
                (1..PlotMassClaimService.MAX_MASSCLAIM_SIZE)
                    .map(Int::toString)
                    .filter { it.startsWith(args[commandIndex + 2], ignoreCase = true) }
            }
            in ADD_OWNER_COMMANDS if args.size == commandIndex + 2 && player.hasPermission(PLOT_OWNER_MANAGE_PERMISSION) -> {
                completeCsv(args[commandIndex + 1], onlinePlayerNames())
            }
            in REMOVE_OWNER_COMMANDS if args.size == commandIndex + 2 && player.hasPermission(PLOT_OWNER_MANAGE_PERMISSION) -> {
                completeCsv(args[commandIndex + 1], currentPlotOwnerNames(player, args))
            }
            in KICK_COMMANDS if args.size == commandIndex + 2 -> {
                completeCsv(args[commandIndex + 1], currentPlotPlayerNames(player, args))
            }
            in REMOVE_COMMANDS if args.size == commandIndex + 2 -> {
                completeCsv(args[commandIndex + 1], currentPlotRelationNames(player, args))
            }
            in LIST_COMMANDS if args.size == commandIndex + 3 && args[commandIndex + 1].equals("player", ignoreCase = true) -> {
                completeCsv(args[commandIndex + 2], ownerSuggestions.plotOwnerNames(MAX_TAB_RESULTS))
            }
            "grant" if args.size == commandIndex + 3 && args[commandIndex + 1].lowercase(Locale.ROOT) in GRANT_PLAYER_SUBCOMMANDS -> {
                completeCsv(args[commandIndex + 2], onlinePlayerNames())
            }
            "set" if args.size == commandIndex + 3 && args[commandIndex + 1].lowercase(Locale.ROOT) in OWNER_SUBCOMMANDS -> {
                completeCsv(args[commandIndex + 2], onlinePlayerNames())
            }
            else -> null
        }
    }

    private fun mergeRootCompletions(raw: String, baseCompletions: List<String>): List<String> {
        val normalizedPrefix = raw.trim()
        val merged = LinkedHashMap<String, String>()
        baseCompletions
            .asSequence()
            .filter { it.startsWith(normalizedPrefix, ignoreCase = true) }
            .forEach { merged.putIfAbsent(normalizeNameKey(it), it) }
        ROOT_CUSTOM_SUBCOMMANDS
            .asSequence()
            .filter { it.startsWith(normalizedPrefix, ignoreCase = true) }
            .forEach { merged.putIfAbsent(normalizeNameKey(it), it) }
        return merged.values
            .take(MAX_TAB_RESULTS)
            .toList()
    }

    private fun rewriteCsvArgument(
        player: Player,
        args: Array<String>,
        index: Int,
        allowEveryone: Boolean,
        resolver: (String) -> String?
    ): Array<String>? {
        val rawValue = args.getOrNull(index) ?: return args
        val segments = rawValue.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (segments.isEmpty()) {
            sendUnknownPlayer(player)
            return null
        }

        val normalized = ArrayList<String>(segments.size)
        for (segment in segments) {
            if (allowEveryone && segment == EVERYONE_TOKEN) {
                normalized += EVERYONE_TOKEN
                continue
            }

            val resolved = resolver(segment)
            if (resolved == null) {
                sendUnknownPlayer(player)
                return null
            }
            normalized += resolved
        }

        args[index] = normalized.joinToString(",")
        return args
    }

    private fun rewritePlotOwnerArgument(args: Array<String>, index: Int): Array<String> {
        val input = args.getOrNull(index) ?: return args
        val resolved = ownerSuggestions.resolvePlotOwnerName(input) ?: return args
        args[index] = resolved
        return args
    }

    private fun resolveExistingUserName(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return null
        }

        val user = BukkitHelper.getUser(trimmed).orElse(null)
            ?: Bukkit.getOnlinePlayers()
                .firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
                ?.user
            ?: return null

        return user.name
    }

    private fun resolvePlotScopedUserName(
        player: Player,
        args: Array<out String>,
        input: String,
        namesProvider: (Plot) -> Collection<String>
    ): String? {
        val plot = resolveTargetPlot(player, args) ?: return null
        val normalizedInput = normalizeNameKey(input)
        return namesProvider(plot).firstOrNull { normalizeNameKey(it) == normalizedInput }
    }

    private fun onlinePlayerNames(): List<String> =
        Bukkit.getOnlinePlayers()
            .asSequence()
            .map { it.name }
            .filter { it.isNotBlank() }
            .distinctBy(::normalizeNameKey)
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
            .toList()

    private fun knownUserCommandCompletions(commandName: String): List<String> =
        if (commandName in EVERYONE_TARGET_COMMANDS) onlinePlayerNames() + EVERYONE_TOKEN else onlinePlayerNames()

    private fun completeHomePages(player: Player, raw: String): List<String> {
        val pageCount = ownerSuggestions.countHomeBasePlots(PlotPlayer.from(player).uuid)
        if (pageCount <= 0) return emptyList()
        val prefix = raw.trim()
        return (1..pageCount).asSequence().map(Int::toString).filter { it.startsWith(prefix) }.take(MAX_TAB_RESULTS).toList()
    }

    fun invalidateOwnerSuggestions() = ownerSuggestions.invalidateOwnerSuggestions()

    fun invalidateHomeCount(ownerId: UUID) = ownerSuggestions.invalidateHomeCount(ownerId)

    private fun currentPlotPlayerNames(player: Player, args: Array<out String>): List<String> {
        val plot = resolveTargetPlot(player, args) ?: return emptyList()
        return (plot.playersInPlot.mapNotNull { it.name.takeIf(String::isNotBlank) } +
            hooker.plotAccessGuardService.deniedOnlinePlayerNamesInside(plot))
            .distinctBy(::normalizeNameKey)
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
    }

    private fun currentPlotRelationNames(player: Player, args: Array<out String>): List<String> {
        val plot = resolveTargetPlot(player, args) ?: return emptyList()
        return ownerSuggestions.resolveScopedNames(plot.members + plot.trusted + plot.denied)
    }

    private fun currentPlotOwnerNames(player: Player, args: Array<out String>): List<String> {
        val plot = resolveTargetPlot(player, args) ?: return emptyList()
        return ownerSuggestions.resolveScopedNames(plot.owners)
    }

    private fun handleOwnerExtension(
        player: Player,
        args: Array<out String>,
        commandIndex: Int,
        addOwner: Boolean
    ) {
        val rawTargetName = args.getOrNull(commandIndex + 1)?.trim()
        if (rawTargetName.isNullOrEmpty()) {
            hooker.messageManager.sendChat(
                player,
                if (addOwner) MessageKey.PLOT_OWNER_ADD_USAGE else MessageKey.PLOT_OWNER_REMOVE_USAGE
            )
            return
        }

        val plot = resolveTargetPlot(player, args) ?: run {
            hooker.messageManager.sendChat(player, MessageKey.PLOT_EDIT_NOT_ON_PLOT)
            return
        }
        if (!plot.hasOwner()) {
            hooker.messageManager.sendChat(player, MessageKey.PLOT_OWNER_UNOWNED)
            return
        }

        resolveExistingPlotUser(rawTargetName) { resolved ->
            if (resolved == null) {
                Bukkit.getScheduler().runTask(hooker.plugin, Runnable {
                    if (!player.isOnline) {
                        return@Runnable
                    }
                    sendUnknownPlayer(player)
                })
                return@resolveExistingPlotUser
            }

            Bukkit.getScheduler().runTask(hooker.plugin, Runnable {
                if (!player.isOnline) {
                    return@Runnable
                }
                val outcome = if (addOwner) {
                    ownerExtensionService.addOwner(plot, resolved.uuid)
                } else {
                    ownerExtensionService.removeOwner(plot, resolved.uuid)
                }
                val ownerVariables = mapOf("player" to resolved.name)

                when (outcome) {
                    is PlotOwnerService.Outcome.Success -> {
                        invalidateOwnerSuggestions()
                        outcome.affectedOwnerIds.forEach(::invalidateHomeCount)
                        hooker.messageManager.sendChat(
                            player,
                            if (addOwner) MessageKey.PLOT_OWNER_ADD_SUCCESS else MessageKey.PLOT_OWNER_REMOVE_SUCCESS,
                            ownerVariables
                        )
                    }
                    PlotOwnerService.Outcome.AlreadyOwner -> {
                        hooker.messageManager.sendChat(player, MessageKey.PLOT_OWNER_ALREADY_OWNER, ownerVariables)
                    }
                    PlotOwnerService.Outcome.NoFreeOwnerSlot -> {
                        hooker.messageManager.sendChat(player, MessageKey.PLOT_OWNER_NO_FREE_SLOT)
                    }
                    PlotOwnerService.Outcome.NotOwner -> {
                        hooker.messageManager.sendChat(player, MessageKey.PLOT_OWNER_NOT_OWNER, ownerVariables)
                    }
                    PlotOwnerService.Outcome.CannotRemoveLastOwner -> {
                        hooker.messageManager.sendChat(player, MessageKey.PLOT_OWNER_CANNOT_REMOVE_LAST)
                    }
                }
            })
        }
    }

    private fun resolveExistingPlotUser(input: String, callback: (ResolvedPlotUser?) -> Unit) {
        val canonicalName = resolveExistingUserName(input) ?: run {
            callback(null)
            return
        }

        Bukkit.getOnlinePlayers()
            .firstOrNull { it.name.equals(canonicalName, ignoreCase = true) }
            ?.let { onlinePlayer ->
                callback(ResolvedPlotUser(onlinePlayer.uniqueId, onlinePlayer.name))
                return
            }

        PlotSquared.get().impromptuUUIDPipeline.getSingle(canonicalName) { uuid, throwable ->
            if (throwable != null || uuid == null) {
                callback(null)
                return@getSingle
            }
            callback(ResolvedPlotUser(uuid, canonicalName))
        }
    }

    private fun completeCsv(raw: String, values: List<String>): List<String> {
        val segments = splitCsvSegments(raw)
        val prefix = normalizeNameKey(segments.current)
        return values.asSequence()
            .filter { normalizeNameKey(it).startsWith(prefix) }
            .map { suggestion -> segments.prefix + suggestion }
            .distinctBy(::normalizeNameKey)
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
            .take(MAX_TAB_RESULTS)
            .toList()
    }

    private fun splitCsvSegments(raw: String): CsvSegments {
        val separatorIndex = raw.lastIndexOf(',')
        if (separatorIndex < 0) {
            return CsvSegments(prefix = "", current = raw.trim())
        }
        return CsvSegments(
            prefix = raw.substring(0, separatorIndex + 1),
            current = raw.substring(separatorIndex + 1).trim()
        )
    }

    private fun resolveCommandIndex(args: Array<out String>): Int {
        if (args.size > 1 && looksLikePlotArgument(args[0])) {
            return 1
        }
        return 0
    }

    private fun copyArgs(args: Array<out String>): Array<String> =
        Array(args.size) { index -> args[index] }

    private fun resolveTargetPlot(player: Player, args: Array<out String>): Plot? {
        val plotPlayer = PlotPlayer.from(player)
        if (args.isNotEmpty() && looksLikePlotArgument(args[0])) {
            return runCatching { Plot.getPlotFromString(plotPlayer, args[0], true) }.getOrNull() ?: plotPlayer.currentPlot
        }
        return plotPlayer.currentPlot
    }

    private fun looksLikePlotArgument(token: String): Boolean {
        val trimmed = token.trim()
        if (';' !in trimmed) {
            return false
        }
        if (',' in trimmed) {
            return false
        }
        return trimmed.count { it == ';' } >= 1
    }

    private fun sendUnknownPlayer(player: Player) {
        hooker.messageManager.sendChat(player, MessageKey.PLOT_ERROR_UNKNOWN_PLAYER)
    }

    private fun isPlotSquaredEnabled(): Boolean =
        hooker.plugin.server.pluginManager.getPlugin(PLOT_SQUARED_PLUGIN_NAME)?.isEnabled == true

    private fun normalizeNameKey(name: String): String = name.trim().lowercase(Locale.ROOT)

    private data class ResolvedPlotUser(
        val uuid: UUID,
        val name: String
    )


    private companion object {
        private const val PLOT_SQUARED_PLUGIN_NAME = "PlotSquared"
        private const val EVERYONE_TOKEN = "*"
        private const val MAX_TAB_RESULTS = 100

        private val ROOT_ALIASES = setOf("plots", "p", "plot", "ps", "plotsquared", "p2", "2", "plotme")
        private val EVERYONE_TARGET_COMMANDS = setOf("add", "trust", "t")
        private val KNOWN_USER_COMMANDS = EVERYONE_TARGET_COMMANDS + setOf(
            "deny", "d", "ban", "setowner", "owner", "so", "seto"
        )
        private val VISIT_COMMANDS = setOf("visit", "v", "tp", "teleport", "goto", "warp")
        private val HOME_COMMANDS = setOf("home", "h")
        private val KICK_COMMANDS = setOf("kick")
        private val REMOVE_COMMANDS = setOf("remove", "r", "untrust", "ut", "undeny", "ud", "unban")
        private val ADD_OWNER_COMMANDS = setOf("addowner")
        private val REMOVE_OWNER_COMMANDS = setOf("removeowner")
        private val LIST_COMMANDS = setOf("list", "l", "find", "search")
        private val USAGE_COMMANDS = setOf("usage", "us")
        private val GRANT_PLAYER_SUBCOMMANDS = setOf("add", "check")
        private val OWNER_SUBCOMMANDS = setOf("owner", "setowner", "so", "seto")
        private val ROOT_CUSTOM_SUBCOMMANDS = listOf("edit", "usage", "massclaim", "addowner", "removeowner")
        private val MASSCLAIM_COMMANDS = setOf("massclaim", "mc")
        private const val PLOT_USAGE_PERMISSION = "acreative.plots.usage"
        private const val PLOT_USAGE_OTHER_PERMISSION = "acreative.plots.usage.other"
        private const val PLOT_MASSCLAIM_PERMISSION = "acreative.plots.massclaim"
        private const val PLOT_OWNER_MANAGE_PERMISSION = "acreative.plots.owner.manage"
    }
}
