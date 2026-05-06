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
        "advancedcreative.itemdb" to listOf(
            entry("/itemdb", "Показать строковый и числовой ID предмета", PluginCommandType.ITEMDB)
        ),
        "advancedcreative.sit" to listOf(
            entry("/sit", "Сесть", PluginCommandType.SIT)
        ),
        "advancedcreative.lay" to listOf(
            entry("/lay", "Лечь", PluginCommandType.LAY)
        ),
        "advancedcreative.crawl" to listOf(
            entry("/crawl", "Ползти", PluginCommandType.CRAWL)
        ),
        "advancedcreative.hide" to listOf(
            entry("/hide <игрок>", "Скрыть игрока для себя", PluginCommandType.HIDE)
        ),
        "advancedcreative.strength" to listOf(
            entry("/strength <значение>", "Изменить силу удара", PluginCommandType.STRENGTH)
        ),
        "advancedcreative.health" to listOf(
            entry("/health <значение>", "Изменить максимальное здоровье", PluginCommandType.HEALTH)
        ),
        "advancedcreative.effects" to listOf(
            entry("/effects <эффект> [уровень]", "Выдать себе постоянный эффект", PluginCommandType.EFFECTS)
        ),
        "advancedcreative.decorationheads" to listOf(
            entry("/decorationheads", "Открыть меню декоративных голов", PluginCommandType.DECORATIONHEADS)
        ),
        "advancedcreative.decorationbanners" to listOf(
            entry("/banner", "Открыть меню флагов и публикации", PluginCommandType.BANNER),
            entry("/decorationbanners [игрок]", "Открыть галерею опубликованных флагов", PluginCommandType.DECORATIONBANNERS),
            entry("/myflags", "Открыть личное хранилище флагов", PluginCommandType.MYFLAGS),
            entry("/banneredit", "Открыть редактор флагов", PluginCommandType.BANNEREDIT)
        ),
        "advancedcreative.glide" to listOf(
            entry("/glide", "Переключить парение без элитр", PluginCommandType.GLIDE)
        ),
        "advancedcreative.gravity" to listOf(
            entry("/gravity <значение>", "Изменить гравитацию", PluginCommandType.GRAVITY)
        ),
        "advancedcreative.sneeze" to listOf(
            entry("/sneeze", "Чихнуть", PluginCommandType.SNEEZE)
        ),
        "advancedcreative.edit" to listOf(
            entry("/edit", "Открыть редактор предмета", PluginCommandType.EDIT)
        ),
        "advancedcreative.freeze" to listOf(
            entry("/freeze", "Заморозить себя", PluginCommandType.FREEZE)
        ),
        "advancedcreative.resize" to listOf(
            entry("/resize <значение>", "Изменить размер персонажа", PluginCommandType.RESIZE)
        ),
        "advancedcreative.paint" to listOf(
            entry("/paint [1x1-4x4]", "Открыть режим рисования", PluginCommandType.PAINT)
        ),
        "advancedcreative.glow" to listOf(
            entry("/glow", "Переключить свечение", PluginCommandType.GLOW)
        ),
        "advancedcreative.disguise" to listOf(
            entry("/disguise <существо> [-self|-noself]", "Превратиться в существо", PluginCommandType.DISGUISE)
        ),
        "advancedcreative.sithead" to listOf(
            entry("/sithead toggle", "Переключить посадку на голову по клику", PluginCommandType.SITHEAD)
        ),
        "advancedcreative.spit" to listOf(
            entry("/spit", "Плюнуть", PluginCommandType.SPIT)
        ),
        "advancedcreative.piss" to listOf(
            entry("/piss", "Пописсать", PluginCommandType.PISS)
        ),
        "advancedcreative.disguise.nick" to listOf(
            entry("/disguise <существо> -withnick", "Скрыть ник в облике", PluginCommandType.DISGUISE)
        ),
        "advancedcreative.disguise.extended" to listOf(
            entry("/disguise <существо>", "Использовать расширенный список обликов", PluginCommandType.DISGUISE)
        ),
        "advancedcreative.acreative" to listOf(
            entry("/acreative", "Управление системами и служебными функциями", PluginCommandType.ACREATIVE)
        ),
        "advancedcreative.sithead.other" to listOf(
            entry("/sithead <цель> [игрок]", "Посадить игрока на голову другого", PluginCommandType.SITHEAD)
        ),
        "advancedcreative.freeze.other" to listOf(
            entry("/freeze <игрок>", "Заморозить другого игрока", PluginCommandType.FREEZE)
        ),
        "advancedcreative.effects.other" to listOf(
            entry("/effects <эффект> [уровень] [игрок]", "Выдать постоянный эффект другому игроку", PluginCommandType.EFFECTS)
        ),
        "advancedcreative.paint.moderation" to listOf(
            entry("/paint ban <игрок> [причина]", "Выдать или снять бан на рисование", PluginCommandType.PAINT),
            entry("/paint banlist", "Открыть список банов рисования", PluginCommandType.PAINT)
        ),
        "advancedcreative.decorationbanners.moderation" to listOf(
            entry("/banner ban", "Выдать или снять бан с узора в руке", PluginCommandType.BANNER),
            entry("/banner banlist", "Открыть список забаненных узоров", PluginCommandType.BANNER),
            entry("/banner banuser <игрок> [причина]", "Выдать или снять бан с игрока", PluginCommandType.BANNER),
            entry("/banner banuserlist", "Открыть список забаненных игроков", PluginCommandType.BANNER),
            entry("/decorationbanners [игрок] [-m]", "Открыть галерею флагов в режиме модерации", PluginCommandType.DECORATIONBANNERS)
        ),
        "advancedcreative.grab" to listOf(
            entry("/grab <игрок> [-force]", "Схватить игрока перед собой", PluginCommandType.GRAB)
        ),
        "advancedcreative.slap" to listOf(
            entry("/slap", "Переключить режим пощечин", PluginCommandType.SLAP)
        ),
        "advancedcreative.jar" to listOf(
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
            appendLine("<#FFD700><st>                      </st><<#FFE68A><b> Полезные команды </b><#FFD700>><st>                      </st>")
            appendLine("<#EDC800>Доступно для ${roleLabel(currentPage.role)}")
            currentPage.entries.forEach { entry ->
                appendLine("<#C7A300> ● <#FFE68A>${escapeMiniMessage(entry.usage)} <#EDC800>- <#FFF3E0>${escapeMiniMessage(entry.description)}")
            }
            appendLine("<#FFD700><st>                           </st> <st>                    </st> <st>          </st>")
            append("<#FFD700>▍ <#FFE68A>Страница: <#FFF3E0>$currentPageNumber/$totalPages <#FFD700>→ ( ${buildNavigation(currentPageNumber, totalPages)} <#FFD700>)")
        }.trimEnd()
    }

    private fun buildPages(player: Player): List<HelpPage> {
        return hooker.permissionManager.orderedRoles()
            .filter { role -> shouldIncludeRoleSection(player, role) }
            .flatMap { role ->
                val entries = role.permissions
                    .asSequence()
                    .flatMap { permission -> helpEntriesByPermission[permission.lowercase()].orEmpty().asSequence() }
                    .filter(::isEnabled)
                    .toList()

                entries.chunked(PAGE_SIZE).map { chunk -> HelpPage(role = role, entries = chunk) }
            }
    }

    private fun isEnabled(entry: HelpEntry): Boolean {
        val managedSystem = entry.commandType.managedSystem ?: return true
        return hooker.systemToggleService.isEnabled(managedSystem)
    }

    private fun buildNavigation(currentPage: Int, totalPages: Int): String {
        return visiblePageNumbers(currentPage, totalPages).joinToString(" <#FFD700>| ") { item ->
            when (item) {
                null -> "<#FFF3E0>..."
                currentPage -> "<#FFF3E0><u>$item</u></#FFF3E0>"
                else -> "<#FFF3E0><click:run_command:'/ahelp $item'>$item</click></#FFF3E0>"
            }
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

    private fun shouldIncludeRoleSection(player: Player, role: PermissionManager.Role): Boolean {
        if (role.key != MODER_ROLE_KEY) {
            return true
        }

        return role.rankPermissions.any(player::hasPermission) || role.permissions.any(player::hasPermission)
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
    }
}
