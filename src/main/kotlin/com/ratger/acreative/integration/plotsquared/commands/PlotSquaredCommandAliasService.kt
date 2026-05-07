package com.ratger.acreative.integration.plotsquared.commands

import com.plotsquared.bukkit.util.BukkitUtil
import com.plotsquared.core.PlotSquared
import com.plotsquared.core.configuration.caption.StaticCaption
import com.plotsquared.core.player.PlotPlayer
import com.plotsquared.core.plot.Plot
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.PluginCommand
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.util.BukkitHelper
import ru.violence.coreapi.bukkit.api.util.ext.player
import ru.violence.coreapi.bukkit.api.util.ext.user
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PlotSquaredCommandAliasService(
    private val hooker: FunctionHooker
) : CommandExecutor, TabCompleter {

    private data class RegisteredRootCommand(
        val command: PluginCommand,
        val executor: CommandExecutor,
        val tabCompleter: TabCompleter?
    )

    private data class OwnerNameEntry(
        val uuid: UUID,
        val name: String
    )

    private data class CsvSegments(
        val prefix: String,
        val current: String
    )

    private val registeredCommands = LinkedHashMap<String, RegisteredRootCommand>()
    private val ownerNamesByLowerName = LinkedHashMap<String, OwnerNameEntry>()
    private val ownerCacheLock = Any()
    private val pendingWarmups = ConcurrentHashMap.newKeySet<UUID>()

    @Volatile
    private var ownerCacheUpdatedAt = 0L

    @Volatile
    private var ownerWarmupInFlight = false

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
        val rewritten = rewriteArgs(player, args) ?: return true
        return registered.executor.onCommand(sender, command, label, rewritten)
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String>? {
        val registered = registeredCommands[command.name.lowercase(Locale.ROOT)] ?: return null
        val player = sender as? Player ?: return registered.tabCompleter?.onTabComplete(sender, command, alias, args)
        completeIntercepted(player, args)?.let { return it }
        return registered.tabCompleter?.onTabComplete(sender, command, alias, args)
    }

    private fun rewriteArgs(player: Player, args: Array<out String>): Array<String>? {
        if (args.isEmpty()) {
            return copyArgs(args)
        }

        val commandIndex = resolveCommandIndex(args)
        val commandName = args.getOrNull(commandIndex)?.lowercase(Locale.ROOT) ?: return copyArgs(args)
        val rewritten = copyArgs(args)

        return when (commandName) {
            in EXISTING_USER_COMMANDS if args.size > commandIndex + 1 -> {
                rewriteCsvArgument(player, rewritten, commandIndex + 1, ::resolveExistingUserName)
            }
            in VISIT_COMMANDS if args.size > commandIndex + 1 -> {
                rewritePlotOwnerArgument(rewritten, commandIndex + 1)
            }
            in KICK_COMMANDS if args.size > commandIndex + 1 -> {
                rewriteCsvArgument(player, rewritten, commandIndex + 1) { input ->
                    resolvePlotScopedUserName(player, rewritten, input) { plot ->
                        plot.playersInPlot
                            .mapNotNull { it.name.takeIf(String::isNotBlank) }
                    }
                }
            }
            in REMOVE_COMMANDS if args.size > commandIndex + 1 -> {
                rewriteCsvArgument(player, rewritten, commandIndex + 1) { input ->
                    resolvePlotScopedUserName(player, rewritten, input) { plot ->
                        resolveScopedNames(plot.members + plot.trusted + plot.denied)
                    }
                }
            }
            in LIST_COMMANDS if args.size > commandIndex + 2 && args[commandIndex + 1].equals("player", ignoreCase = true) -> {
                rewritePlotOwnerArgument(rewritten, commandIndex + 2)
            }
            "grant" if args.size > commandIndex + 2 && args[commandIndex + 1].lowercase(Locale.ROOT) in GRANT_PLAYER_SUBCOMMANDS -> {
                rewriteCsvArgument(player, rewritten, commandIndex + 2, ::resolveExistingUserName)
            }
            "set" if args.size > commandIndex + 2 && args[commandIndex + 1].lowercase(Locale.ROOT) in OWNER_SUBCOMMANDS -> {
                rewriteCsvArgument(player, rewritten, commandIndex + 2, ::resolveExistingUserName)
            }
            else -> rewritten
        }
    }

    private fun completeIntercepted(player: Player, args: Array<out String>): List<String>? {
        if (args.isEmpty()) {
            return null
        }

        val commandIndex = resolveCommandIndex(args)
        val commandName = args.getOrNull(commandIndex)?.lowercase(Locale.ROOT) ?: return null

        return when (commandName) {
            in EXISTING_USER_COMMANDS if args.size == commandIndex + 2 -> {
                completeCsv(args[commandIndex + 1], onlinePlayerNames())
            }
            in VISIT_COMMANDS if args.size == commandIndex + 2 -> {
                completeCsv(args[commandIndex + 1], plotOwnerNames())
            }
            in KICK_COMMANDS if args.size == commandIndex + 2 -> {
                completeCsv(args[commandIndex + 1], currentPlotPlayerNames(player, args))
            }
            in REMOVE_COMMANDS if args.size == commandIndex + 2 -> {
                completeCsv(args[commandIndex + 1], currentPlotRelationNames(player, args))
            }
            in LIST_COMMANDS if args.size == commandIndex + 3 && args[commandIndex + 1].equals("player", ignoreCase = true) -> {
                completeCsv(args[commandIndex + 2], plotOwnerNames())
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

    private fun rewriteCsvArgument(
        player: Player,
        args: Array<String>,
        index: Int,
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
            if (segment == EVERYONE_TOKEN) {
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
        val resolved = resolvePlotOwnerName(input) ?: return args
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

        return user.player?.name ?: user.name
    }

    private fun resolvePlotOwnerName(input: String): String? {
        refreshOwnerCacheIfNeeded()
        return ownerNamesByLowerName[normalizeNameKey(input)]?.name
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

    private fun currentPlotPlayerNames(player: Player, args: Array<out String>): List<String> {
        val plot = resolveTargetPlot(player, args) ?: return emptyList()
        return plot.playersInPlot
            .mapNotNull { it.name.takeIf(String::isNotBlank) }
            .distinctBy(::normalizeNameKey)
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
    }

    private fun currentPlotRelationNames(player: Player, args: Array<out String>): List<String> {
        val plot = resolveTargetPlot(player, args) ?: return emptyList()
        return resolveScopedNames(plot.members + plot.trusted + plot.denied)
    }

    private fun resolveScopedNames(uuids: Collection<UUID>): List<String> {
        if (uuids.isEmpty()) {
            return emptyList()
        }

        val pipeline = PlotSquared.get().impromptuUUIDPipeline
        val names = LinkedHashMap<String, String>()
        val unresolved = LinkedHashSet<UUID>()

        uuids.forEach { uuid ->
            val onlineName = Bukkit.getPlayer(uuid)?.name
                ?: PlotSquared.platform().playerManager().getPlayerIfExists(uuid)?.name
            when {
                !onlineName.isNullOrBlank() -> names.putIfAbsent(normalizeNameKey(onlineName), onlineName)
                else -> {
                    val mapping = pipeline.getImmediately(uuid)
                    if (mapping?.username().isNullOrBlank()) {
                        unresolved += uuid
                    } else {
                        val mappingName = mapping.username()
                        names.putIfAbsent(normalizeNameKey(mappingName), mappingName)
                    }
                }
            }
        }

        warmupNamesAsync(unresolved, invalidateOwners = false)

        return names.values
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
    }

    private fun plotOwnerNames(): List<String> {
        refreshOwnerCacheIfNeeded()
        return ownerNamesByLowerName.values
            .asSequence()
            .map { it.name }
            .distinctBy(::normalizeNameKey)
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
            .take(MAX_TAB_RESULTS)
            .toList()
    }

    private fun refreshOwnerCacheIfNeeded() {
        val now = System.currentTimeMillis()
        if (now - ownerCacheUpdatedAt < OWNER_CACHE_TTL_MILLIS && ownerNamesByLowerName.isNotEmpty()) {
            return
        }

        synchronized(ownerCacheLock) {
            if (now - ownerCacheUpdatedAt < OWNER_CACHE_TTL_MILLIS && ownerNamesByLowerName.isNotEmpty()) {
                return
            }

            val pipeline = PlotSquared.get().impromptuUUIDPipeline
            val fresh = LinkedHashMap<String, OwnerNameEntry>()
            val unresolved = LinkedHashSet<UUID>()

            PlotSquared.get().plotAreaManager.allPlotAreas
                .forEach { area ->
                    area.plots.forEach { plot ->
                        if (!plot.hasOwner()) {
                            return@forEach
                        }

                        plot.owners.forEach { uuid ->
                            val onlineName = Bukkit.getPlayer(uuid)?.name
                                ?: PlotSquared.platform().playerManager().getPlayerIfExists(uuid)?.name
                            val resolvedName = onlineName
                                ?: pipeline.getImmediately(uuid)?.username()

                            if (resolvedName.isNullOrBlank()) {
                                unresolved += uuid
                            } else {
                                val key = normalizeNameKey(resolvedName)
                                fresh.putIfAbsent(key, OwnerNameEntry(uuid, resolvedName))
                            }
                        }
                    }
                }

            ownerNamesByLowerName.clear()
            ownerNamesByLowerName.putAll(fresh)
            ownerCacheUpdatedAt = now
            warmupNamesAsync(unresolved, invalidateOwners = true)
        }
    }

    private fun warmupNamesAsync(uuids: Set<UUID>, invalidateOwners: Boolean) {
        if (uuids.isEmpty()) {
            return
        }

        val request = uuids.filter { pendingWarmups.add(it) }.toSet()
        if (request.isEmpty()) {
            return
        }

        if (invalidateOwners) {
            synchronized(ownerCacheLock) {
                if (ownerWarmupInFlight) {
                    request.forEach(pendingWarmups::remove)
                    return
                }
                ownerWarmupInFlight = true
            }
        }

        PlotSquared.get().impromptuUUIDPipeline.getNames(request)
            .whenComplete { _, _ ->
                request.forEach(pendingWarmups::remove)
                if (invalidateOwners) {
                    synchronized(ownerCacheLock) {
                        ownerWarmupInFlight = false
                        ownerCacheUpdatedAt = 0L
                    }
                }
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
        BukkitUtil.adapt(player).sendMessage(UNKNOWN_PLAYER_CAPTION)
    }

    private fun isPlotSquaredEnabled(): Boolean =
        hooker.plugin.server.pluginManager.getPlugin(PLOT_SQUARED_PLUGIN_NAME)?.isEnabled == true

    private fun normalizeNameKey(name: String): String = name.trim().lowercase(Locale.ROOT)

    private companion object {
        private const val PLOT_SQUARED_PLUGIN_NAME = "PlotSquared"
        private const val EVERYONE_TOKEN = "*"
        private const val OWNER_CACHE_TTL_MILLIS = 60_000L
        private const val MAX_TAB_RESULTS = 100

        private val UNKNOWN_PLAYER_CAPTION = StaticCaption.of("<dark_gray>[<gold>Creative<dark_gray>] <red>Неизвестный игрок")

        private val ROOT_ALIASES = setOf("plots", "p", "plot", "ps", "plotsquared", "p2", "2", "plotme")
        private val EXISTING_USER_COMMANDS = setOf("add", "trust", "deny", "setowner", "owner", "so", "seto")
        private val VISIT_COMMANDS = setOf("visit", "v", "tp", "teleport", "goto", "warp")
        private val KICK_COMMANDS = setOf("kick")
        private val REMOVE_COMMANDS = setOf("remove", "untrust", "undeny")
        private val LIST_COMMANDS = setOf("list", "l", "find", "search")
        private val GRANT_PLAYER_SUBCOMMANDS = setOf("add", "check")
        private val OWNER_SUBCOMMANDS = setOf("owner", "setowner", "so", "seto")
    }
}
