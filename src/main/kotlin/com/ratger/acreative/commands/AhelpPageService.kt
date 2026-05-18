package com.ratger.acreative.commands

import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.PermissionManager
import org.bukkit.entity.Player

class AhelpPageService(
    private val hooker: FunctionHooker
) {

    private data class HelpEntry(
        val usage: String,
        val description: String,
        val commandType: PluginCommandType
    )

    private data class HelpPage(
        val role: PermissionManager.Role,
        val entries: List<HelpEntry>
    )

    private val helpEntriesByPermission: Map<String, List<HelpEntry>> = mapOf(
        "acreative.itemdb" to listOf(
            entry("/id", "Узнать ID предмета", PluginCommandType.ITEMDB)
        ),
        "acreative.sit" to listOf(
            entry("/sit", "Круто сесть", PluginCommandType.SIT)
        ),
        "acreative.lay" to listOf(
            entry("/lay", "Клёво прилечь", PluginCommandType.LAY)
        ),
        "acreative.crawl" to listOf(
            entry("/crawl", "Очень скрыто ползти", PluginCommandType.CRAWL)
        ),
        "acreative.hide" to listOf(
            entry("/hide <игрок>", "Скрыть игрока для себя", PluginCommandType.HIDE)
        ),
        "acreative.strength" to listOf(
            entry("/strength <значение>", "Изменить силу удара", PluginCommandType.STRENGTH)
        ),
        "acreative.health" to listOf(
            entry("/health <значение>", "Изменить максимальное здоровье", PluginCommandType.HEALTH)
        ),
        "acreative.effects" to listOf(
            entry("/effects <эффект> [уровень]", "Выдать эффект", PluginCommandType.EFFECTS)
        ),
        "acreative.decorationheads" to listOf(
            entry("/dh", "Открыть меню декоративных голов", PluginCommandType.DECORATIONHEADS)
        ),
        "acreative.decorationbanners" to listOf(
            entry("/banner", "Открыть менеджер флагов", PluginCommandType.BANNER),
            entry("/db [игрок]", "Открыть галерею флагов", PluginCommandType.DECORATIONBANNERS),
            entry("/myflags", "Открыть свои флаги", PluginCommandType.MYFLAGS),
            entry("/bedit", "Открыть редактор флагов", PluginCommandType.BANNEREDIT)
        ),
        "acreative.plots.edit" to listOf(
            entry("/p edit", "Открыть настройки участка", PluginCommandType.PLOT_EDIT)
        ),
        "acreative.plots.usage" to listOf(
            entry("/p usage", "Узнать остаток участков", PluginCommandType.PLOT_USAGE)
        ),
        "acreative.plots.massclaim" to listOf(
            entry("/p mc <ширина> <длина>", "Занять большой участок", PluginCommandType.PLOT_MASSCLAIM)
        ),
        "acreative.glide" to listOf(
            entry("/glide", "Переключить парение без элитр", PluginCommandType.GLIDE)
        ),
        "acreative.gravity" to listOf(
            entry("/gravity <значение>", "Изменить гравитацию", PluginCommandType.GRAVITY)
        ),
        "acreative.sneeze" to listOf(
            entry("/sneeze", "Чуть-чуть чихнуть", PluginCommandType.SNEEZE)
        ),
        "acreative.edit" to listOf(
            entry("/edit", "Открыть редактор предмета", PluginCommandType.EDIT)
        ),
        "acreative.freeze" to listOf(
            entry("/freeze", "Заморозить себя", PluginCommandType.FREEZE)
        ),
        "acreative.resize" to listOf(
            entry("/resize <значение>", "Изменить размер персонажа", PluginCommandType.RESIZE)
        ),
        "acreative.paint" to listOf(
            entry("/paint [размер]", "Открыть режим рисования", PluginCommandType.PAINT)
        ),
        "acreative.glow" to listOf(
            entry("/glow", "Переключить свечение", PluginCommandType.GLOW)
        ),
        "acreative.disguise" to listOf(
            entry("/dis <существо>", "Превратиться в существо", PluginCommandType.DISGUISE)
        ),
        "acreative.sithead" to listOf(
            entry("/sithead toggle", "Переключить посадку на голову по клику", PluginCommandType.SITHEAD)
        ),
        "acreative.spit" to listOf(
            entry("/spit", "Плюнуть, агрессивно", PluginCommandType.SPIT)
        ),
        "acreative.piss" to listOf(
            entry("/piss", "Пописсать на негодяев", PluginCommandType.PISS)
        ),
        "acreative.disguise.extended" to listOf(
            entry("/dis <существо>", "Превращение в боссов", PluginCommandType.DISGUISE)
        ),
        "acreative.disguise.nick" to listOf(
            entry("/dis <существо> [-withnick]", "Скрыть ник в облике", PluginCommandType.DISGUISE)
        ),
        "acreative.disguise.player" to listOf(
            entry("/dis player <игрок>", "Превратиться в онлайн-игрока", PluginCommandType.DISGUISE)
        ),
        "acreative.disguise.text" to listOf(
            entry("/dis text_display <текст>", "Превратиться в текст-дисплей", PluginCommandType.DISGUISE)
        ),
        "acreative.acreative" to listOf(
            entry("/acreative", "Служебные функции", PluginCommandType.ACREATIVE)
        ),
        "acreative.sithead.other" to listOf(
            entry("/sithead <цель> [игрок]", "Посадить на голову другого", PluginCommandType.SITHEAD)
        ),
        "acreative.plots.usage.other" to listOf(
            entry("/p usage <игрок>", "Узнать остаток участков игрока", PluginCommandType.PLOT_USAGE)
        ),
        "acreative.freeze.other" to listOf(
            entry("/freeze <игрок>", "Заморозить другого игрока", PluginCommandType.FREEZE)
        ),
        "acreative.effects.other" to listOf(
            entry("/effects ... [игрок]", "Выдать эффект другому игроку", PluginCommandType.EFFECTS)
        ),
        "acreative.paint.moderation" to listOf(
            entry("/paint ban <игрок> [причина]", "Бан/разбан рисования", PluginCommandType.PAINT)
        ),
        "acreative.decorationbanners.moderation" to listOf(
            entry("/banner ban", "Бан/разбан паттерна банера в руке", PluginCommandType.BANNER),
            entry("/banner banuser <игрок> [причина]", "Бан/разбан игрока", PluginCommandType.BANNER),
            entry("/db [игрок] [-m]", "Галерея в режиме модерации", PluginCommandType.DECORATIONBANNERS)
        ),
        "acreative.grab" to listOf(
            entry("/grab <игрок> [-force]", "Схватить игрока перед собой", PluginCommandType.GRAB)
        ),
        "acreative.slap" to listOf(
            entry("/slap", "Переключить режим пощечин", PluginCommandType.SLAP)
        ),
        "acreative.jar" to listOf(
            entry("/jar <игрок> [-const]", "Выдать банку для поимки игрока", PluginCommandType.JAR)
        )
    )

    fun renderFor(player: Player, requestedPage: Int?): String {
        val pages = buildPages(player)
        if (pages.isEmpty()) {
            return buildEmptyState()
        }

        val pageIndex = ((requestedPage ?: 1) - 1).coerceIn(0, pages.lastIndex)
        val currentPage = pages[pageIndex]
        val currentPageNumber = pageIndex + 1
        val totalPages = pages.size

        return buildString {
            appendLine("<#FFD700><st>                       </st><<#FFE68A><b> Полезные команды </b><#FFD700>><st>                        </st>")
            appendLine("<#EDC800>Доступно для ${roleLabel(currentPage.role)}")
            currentPage.entries.forEach { entry ->
                appendLine("<#C7A300> ● <#FFE68A>${renderUsage(entry.usage)} <#EDC800>- <#FFF3E0>${escapeMiniMessage(entry.description)}")
            }
            appendLine("<#FFD700><st>                                                                                </st>")
            append("<#FFD700>▍ <#FFE68A>Страница: <#FFF3E0>$currentPageNumber/$totalPages <#FFD700>→ (${buildNavigation(currentPageNumber, totalPages)}<#FFD700>)")
        }.trimEnd()
    }

    private fun buildPages(player: Player): List<HelpPage> {
        return hooker.permissionManager.orderedRoles()
            .flatMap { role ->
                val entries = buildRoleEntries(player, role)
                entries.chunked(PAGE_SIZE).map { chunk -> HelpPage(role = role, entries = chunk) }
            }
    }

    private fun buildRoleEntries(player: Player, role: PermissionManager.Role): List<HelpEntry> {
        return role.permissions
            .asSequence()
            .filter { permission -> shouldIncludePermission(player, role, permission) }
            .flatMap { permission -> helpEntriesByPermission[permission.lowercase()].orEmpty().asSequence() }
            .filter(::isEnabled)
            .toList()
    }

    private fun isEnabled(entry: HelpEntry): Boolean {
        val managedSystem = entry.commandType.managedSystem ?: return true
        return hooker.systemToggleService.isEnabled(managedSystem)
    }

    private fun buildNavigation(currentPage: Int, totalPages: Int): String {
        return visiblePageNumbers(currentPage, totalPages)
            .joinToString("<#FFD700>|") { item -> renderNavigationToken(item, currentPage) }
    }

    private fun renderNavigationToken(page: Int?, currentPage: Int): String {
        return when (page) {
            null -> "<#FFF3E0> ... "
            currentPage -> "<#FFF3E0> <u>$page</u> </#FFF3E0>"
            else -> "<#FFF3E0><click:run_command:'/ahelp $page'> $page </click></#FFF3E0>"
        }
    }

    private fun visiblePageNumbers(currentPage: Int, totalPages: Int): List<Int?> {
        if (totalPages <= 7) {
            return (1..totalPages).map(Int::toInt)
        }

        val visible = sortedSetOf(1, totalPages, currentPage)
        if (currentPage > 1) visible += currentPage - 1
        if (currentPage < totalPages) visible += currentPage + 1
        if (currentPage <= 3) {
            visible += 2
            visible += 3
        }
        if (currentPage >= totalPages - 2) {
            visible += totalPages - 1
            visible += totalPages - 2
        }

        val pages = visible.filter { it in 1..totalPages }
        val result = mutableListOf<Int?>()
        pages.forEachIndexed { index, page ->
            if (index > 0 && page - pages[index - 1] > 1) {
                result += null
            }
            result += page
        }
        return result
    }

    private fun roleLabel(role: PermissionManager.Role): String {
        if (role.key == PLAYER_ROLE_KEY && (role.prefix.isBlank() || role.prefix == LEGACY_PLAYER_PREFIX)) {
            return PLAYER_HELP_PREFIX
        }

        return role.prefix.takeUnless(String::isBlank)
            ?: "<#8C8C8C><b>${escapeMiniMessage(role.display)}</b>"
    }

    private fun buildEmptyState(): String {
        return """
            <#FFD700><st>                      </st><<#FFE68A><b> Полезные команды </b><#FFD700>><st>                      </st>
            <#EDC800>Для вас сейчас нет доступных команд
            <#FFD700><st>                           </st> <st>                    </st> <st>          </st>
            <#FFD700>▍ <#FFE68A>Страница: <#FFF3E0>1/1
        """.trimIndent()
    }

    private fun escapeMiniMessage(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("<", "\\<")
    }

    private fun renderUsage(value: String): String {
        val result = StringBuilder()
        var currentIndex = 0

        USAGE_ARGUMENT_PATTERN.findAll(value).forEach { match ->
            if (match.range.first > currentIndex) {
                result.append(escapeMiniMessage(value.substring(currentIndex, match.range.first)))
            }
            result.append("<$ARGUMENT_COLOR>${escapeMiniMessage(match.value)}</$ARGUMENT_COLOR>")
            currentIndex = match.range.last + 1
        }

        if (currentIndex < value.length) {
            result.append(escapeMiniMessage(value.substring(currentIndex)))
        }

        return result.toString()
    }

    private fun shouldIncludePermission(player: Player, role: PermissionManager.Role, permission: String): Boolean {
        return if (role.key == MODER_ROLE_KEY) {
            player.hasPermission(permission)
        } else {
            true
        }
    }

    private fun entry(usage: String, description: String, commandType: PluginCommandType): HelpEntry {
        return HelpEntry(usage = usage, description = description, commandType = commandType)
    }

    private companion object {
        const val PAGE_SIZE = 10
        const val PLAYER_ROLE_KEY = "player"
        const val MODER_ROLE_KEY = "moder"
        const val LEGACY_PLAYER_PREFIX = "<#FFF3E0>Player"
        const val PLAYER_HELP_PREFIX = "<#8C8C8C><b>ɢᴀᴍᴇʀ</b>"
        const val ARGUMENT_COLOR = "#C7A300"
        val USAGE_ARGUMENT_PATTERN = Regex("""<[^>]+>|\[[^]]+]""")
    }
}
