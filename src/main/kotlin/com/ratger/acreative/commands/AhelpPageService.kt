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
            entry("/itemdb", "ÐŸÐ¾ÐºÐ°Ð·Ð°Ñ‚ÑŒ ÑÑ‚Ñ€Ð¾ÐºÐ¾Ð²Ñ‹Ð¹ Ð¸ Ñ‡Ð¸ÑÐ»Ð¾Ð²Ð¾Ð¹ ID Ð¿Ñ€ÐµÐ´Ð¼ÐµÑ‚Ð°", PluginCommandType.ITEMDB)
        ),
        "acreative.sit" to listOf(
            entry("/sit", "Ð¡ÐµÑÑ‚ÑŒ", PluginCommandType.SIT)
        ),
        "acreative.lay" to listOf(
            entry("/lay", "Ð›ÐµÑ‡ÑŒ", PluginCommandType.LAY)
        ),
        "acreative.crawl" to listOf(
            entry("/crawl", "ÐŸÐ¾Ð»Ð·Ñ‚Ð¸", PluginCommandType.CRAWL)
        ),
        "acreative.hide" to listOf(
            entry("/hide <Ð¸Ð³Ñ€Ð¾Ðº>", "Ð¡ÐºÑ€Ñ‹Ñ‚ÑŒ Ð¸Ð³Ñ€Ð¾ÐºÐ° Ð´Ð»Ñ ÑÐµÐ±Ñ", PluginCommandType.HIDE)
        ),
        "acreative.strength" to listOf(
            entry("/strength <Ð·Ð½Ð°Ñ‡ÐµÐ½Ð¸Ðµ>", "Ð˜Ð·Ð¼ÐµÐ½Ð¸Ñ‚ÑŒ ÑÐ¸Ð»Ñƒ ÑƒÐ´Ð°Ñ€Ð°", PluginCommandType.STRENGTH)
        ),
        "acreative.health" to listOf(
            entry("/health <Ð·Ð½Ð°Ñ‡ÐµÐ½Ð¸Ðµ>", "Ð˜Ð·Ð¼ÐµÐ½Ð¸Ñ‚ÑŒ Ð¼Ð°ÐºÑÐ¸Ð¼Ð°Ð»ÑŒÐ½Ð¾Ðµ Ð·Ð´Ð¾Ñ€Ð¾Ð²ÑŒÐµ", PluginCommandType.HEALTH)
        ),
        "acreative.effects" to listOf(
            entry("/effects <ÑÑ„Ñ„ÐµÐºÑ‚> [ÑƒÑ€Ð¾Ð²ÐµÐ½ÑŒ]", "Ð’Ñ‹Ð´Ð°Ñ‚ÑŒ ÑÐµÐ±Ðµ Ð¿Ð¾ÑÑ‚Ð¾ÑÐ½Ð½Ñ‹Ð¹ ÑÑ„Ñ„ÐµÐºÑ‚", PluginCommandType.EFFECTS)
        ),
        "acreative.decorationheads" to listOf(
            entry("/decorationheads", "ÐžÑ‚ÐºÑ€Ñ‹Ñ‚ÑŒ Ð¼ÐµÐ½ÑŽ Ð´ÐµÐºÐ¾Ñ€Ð°Ñ‚Ð¸Ð²Ð½Ñ‹Ñ… Ð³Ð¾Ð»Ð¾Ð²", PluginCommandType.DECORATIONHEADS)
        ),
        "acreative.decorationbanners" to listOf(
            entry("/banner", "ÐžÑ‚ÐºÑ€Ñ‹Ñ‚ÑŒ Ð¼ÐµÐ½ÑŽ Ñ„Ð»Ð°Ð³Ð¾Ð² Ð¸ Ð¿ÑƒÐ±Ð»Ð¸ÐºÐ°Ñ†Ð¸Ð¸", PluginCommandType.BANNER),
            entry("/decorationbanners [Ð¸Ð³Ñ€Ð¾Ðº]", "ÐžÑ‚ÐºÑ€Ñ‹Ñ‚ÑŒ Ð³Ð°Ð»ÐµÑ€ÐµÑŽ Ð¾Ð¿ÑƒÐ±Ð»Ð¸ÐºÐ¾Ð²Ð°Ð½Ð½Ñ‹Ñ… Ñ„Ð»Ð°Ð³Ð¾Ð²", PluginCommandType.DECORATIONBANNERS),
            entry("/myflags", "ÐžÑ‚ÐºÑ€Ñ‹Ñ‚ÑŒ Ð»Ð¸Ñ‡Ð½Ð¾Ðµ Ñ…Ñ€Ð°Ð½Ð¸Ð»Ð¸Ñ‰Ðµ Ñ„Ð»Ð°Ð³Ð¾Ð²", PluginCommandType.MYFLAGS),
            entry("/banneredit", "ÐžÑ‚ÐºÑ€Ñ‹Ñ‚ÑŒ Ñ€ÐµÐ´Ð°ÐºÑ‚Ð¾Ñ€ Ñ„Ð»Ð°Ð³Ð¾Ð²", PluginCommandType.BANNEREDIT)
        ),
        "acreative.glide" to listOf(
            entry("/glide", "ÐŸÐµÑ€ÐµÐºÐ»ÑŽÑ‡Ð¸Ñ‚ÑŒ Ð¿Ð°Ñ€ÐµÐ½Ð¸Ðµ Ð±ÐµÐ· ÑÐ»Ð¸Ñ‚Ñ€", PluginCommandType.GLIDE)
        ),
        "acreative.gravity" to listOf(
            entry("/gravity <Ð·Ð½Ð°Ñ‡ÐµÐ½Ð¸Ðµ>", "Ð˜Ð·Ð¼ÐµÐ½Ð¸Ñ‚ÑŒ Ð³Ñ€Ð°Ð²Ð¸Ñ‚Ð°Ñ†Ð¸ÑŽ", PluginCommandType.GRAVITY)
        ),
        "acreative.sneeze" to listOf(
            entry("/sneeze", "Ð§Ð¸Ñ…Ð½ÑƒÑ‚ÑŒ", PluginCommandType.SNEEZE)
        ),
        "acreative.edit" to listOf(
            entry("/edit", "ÐžÑ‚ÐºÑ€Ñ‹Ñ‚ÑŒ Ñ€ÐµÐ´Ð°ÐºÑ‚Ð¾Ñ€ Ð¿Ñ€ÐµÐ´Ð¼ÐµÑ‚Ð°", PluginCommandType.EDIT)
        ),
        "acreative.freeze" to listOf(
            entry("/freeze", "Ð—Ð°Ð¼Ð¾Ñ€Ð¾Ð·Ð¸Ñ‚ÑŒ ÑÐµÐ±Ñ", PluginCommandType.FREEZE)
        ),
        "acreative.resize" to listOf(
            entry("/resize <Ð·Ð½Ð°Ñ‡ÐµÐ½Ð¸Ðµ>", "Ð˜Ð·Ð¼ÐµÐ½Ð¸Ñ‚ÑŒ Ñ€Ð°Ð·Ð¼ÐµÑ€ Ð¿ÐµÑ€ÑÐ¾Ð½Ð°Ð¶Ð°", PluginCommandType.RESIZE)
        ),
        "acreative.paint" to listOf(
            entry("/paint [1x1-4x4]", "ÐžÑ‚ÐºÑ€Ñ‹Ñ‚ÑŒ Ñ€ÐµÐ¶Ð¸Ð¼ Ñ€Ð¸ÑÐ¾Ð²Ð°Ð½Ð¸Ñ", PluginCommandType.PAINT)
        ),
        "acreative.glow" to listOf(
            entry("/glow", "ÐŸÐµÑ€ÐµÐºÐ»ÑŽÑ‡Ð¸Ñ‚ÑŒ ÑÐ²ÐµÑ‡ÐµÐ½Ð¸Ðµ", PluginCommandType.GLOW)
        ),
        "acreative.disguise" to listOf(
            entry("/disguise <ÑÑƒÑ‰ÐµÑÑ‚Ð²Ð¾> [-self|-noself]", "ÐŸÑ€ÐµÐ²Ñ€Ð°Ñ‚Ð¸Ñ‚ÑŒÑÑ Ð² ÑÑƒÑ‰ÐµÑÑ‚Ð²Ð¾", PluginCommandType.DISGUISE)
        ),
        "acreative.sithead" to listOf(
            entry("/sithead toggle", "ÐŸÐµÑ€ÐµÐºÐ»ÑŽÑ‡Ð¸Ñ‚ÑŒ Ð¿Ð¾ÑÐ°Ð´ÐºÑƒ Ð½Ð° Ð³Ð¾Ð»Ð¾Ð²Ñƒ Ð¿Ð¾ ÐºÐ»Ð¸ÐºÑƒ", PluginCommandType.SITHEAD)
        ),
        "acreative.spit" to listOf(
            entry("/spit", "ÐŸÐ»ÑŽÐ½ÑƒÑ‚ÑŒ", PluginCommandType.SPIT)
        ),
        "acreative.piss" to listOf(
            entry("/piss", "ÐŸÐ¾Ð¿Ð¸ÑÑÐ°Ñ‚ÑŒ", PluginCommandType.PISS)
        ),
        "acreative.disguise.nick" to listOf(
            entry("/disguise <ÑÑƒÑ‰ÐµÑÑ‚Ð²Ð¾> -withnick", "Ð¡ÐºÑ€Ñ‹Ñ‚ÑŒ Ð½Ð¸Ðº Ð² Ð¾Ð±Ð»Ð¸ÐºÐµ", PluginCommandType.DISGUISE)
        ),
        "acreative.disguise.extended" to listOf(
            entry("/disguise <ÑÑƒÑ‰ÐµÑÑ‚Ð²Ð¾>", "Ð˜ÑÐ¿Ð¾Ð»ÑŒÐ·Ð¾Ð²Ð°Ñ‚ÑŒ Ñ€Ð°ÑÑˆÐ¸Ñ€ÐµÐ½Ð½Ñ‹Ð¹ ÑÐ¿Ð¸ÑÐ¾Ðº Ð¾Ð±Ð»Ð¸ÐºÐ¾Ð²", PluginCommandType.DISGUISE)
        ),
        "acreative.acreative" to listOf(
            entry("/acreative", "Ð£Ð¿Ñ€Ð°Ð²Ð»ÐµÐ½Ð¸Ðµ ÑÐ¸ÑÑ‚ÐµÐ¼Ð°Ð¼Ð¸ Ð¸ ÑÐ»ÑƒÐ¶ÐµÐ±Ð½Ñ‹Ð¼Ð¸ Ñ„ÑƒÐ½ÐºÑ†Ð¸ÑÐ¼Ð¸", PluginCommandType.ACREATIVE)
        ),
        "acreative.sithead.other" to listOf(
            entry("/sithead <Ñ†ÐµÐ»ÑŒ> [Ð¸Ð³Ñ€Ð¾Ðº]", "ÐŸÐ¾ÑÐ°Ð´Ð¸Ñ‚ÑŒ Ð¸Ð³Ñ€Ð¾ÐºÐ° Ð½Ð° Ð³Ð¾Ð»Ð¾Ð²Ñƒ Ð´Ñ€ÑƒÐ³Ð¾Ð³Ð¾", PluginCommandType.SITHEAD)
        ),
        "acreative.freeze.other" to listOf(
            entry("/freeze <Ð¸Ð³Ñ€Ð¾Ðº>", "Ð—Ð°Ð¼Ð¾Ñ€Ð¾Ð·Ð¸Ñ‚ÑŒ Ð´Ñ€ÑƒÐ³Ð¾Ð³Ð¾ Ð¸Ð³Ñ€Ð¾ÐºÐ°", PluginCommandType.FREEZE)
        ),
        "acreative.effects.other" to listOf(
            entry("/effects <ÑÑ„Ñ„ÐµÐºÑ‚> [ÑƒÑ€Ð¾Ð²ÐµÐ½ÑŒ] [Ð¸Ð³Ñ€Ð¾Ðº]", "Ð’Ñ‹Ð´Ð°Ñ‚ÑŒ Ð¿Ð¾ÑÑ‚Ð¾ÑÐ½Ð½Ñ‹Ð¹ ÑÑ„Ñ„ÐµÐºÑ‚ Ð´Ñ€ÑƒÐ³Ð¾Ð¼Ñƒ Ð¸Ð³Ñ€Ð¾ÐºÑƒ", PluginCommandType.EFFECTS)
        ),
        "acreative.paint.moderation" to listOf(
            entry("/paint ban <Ð¸Ð³Ñ€Ð¾Ðº> [Ð¿Ñ€Ð¸Ñ‡Ð¸Ð½Ð°]", "Ð’Ñ‹Ð´Ð°Ñ‚ÑŒ Ð¸Ð»Ð¸ ÑÐ½ÑÑ‚ÑŒ Ð±Ð°Ð½ Ð½Ð° Ñ€Ð¸ÑÐ¾Ð²Ð°Ð½Ð¸Ðµ", PluginCommandType.PAINT),
            entry("/paint banlist", "ÐžÑ‚ÐºÑ€Ñ‹Ñ‚ÑŒ ÑÐ¿Ð¸ÑÐ¾Ðº Ð±Ð°Ð½Ð¾Ð² Ñ€Ð¸ÑÐ¾Ð²Ð°Ð½Ð¸Ñ", PluginCommandType.PAINT)
        ),
        "acreative.decorationbanners.moderation" to listOf(
            entry("/banner ban", "Ð’Ñ‹Ð´Ð°Ñ‚ÑŒ Ð¸Ð»Ð¸ ÑÐ½ÑÑ‚ÑŒ Ð±Ð°Ð½ Ñ ÑƒÐ·Ð¾Ñ€Ð° Ð² Ñ€ÑƒÐºÐµ", PluginCommandType.BANNER),
            entry("/banner banlist", "ÐžÑ‚ÐºÑ€Ñ‹Ñ‚ÑŒ ÑÐ¿Ð¸ÑÐ¾Ðº Ð·Ð°Ð±Ð°Ð½ÐµÐ½Ð½Ñ‹Ñ… ÑƒÐ·Ð¾Ñ€Ð¾Ð²", PluginCommandType.BANNER),
            entry("/banner banuser <Ð¸Ð³Ñ€Ð¾Ðº> [Ð¿Ñ€Ð¸Ñ‡Ð¸Ð½Ð°]", "Ð’Ñ‹Ð´Ð°Ñ‚ÑŒ Ð¸Ð»Ð¸ ÑÐ½ÑÑ‚ÑŒ Ð±Ð°Ð½ Ñ Ð¸Ð³Ñ€Ð¾ÐºÐ°", PluginCommandType.BANNER),
            entry("/banner banuserlist", "ÐžÑ‚ÐºÑ€Ñ‹Ñ‚ÑŒ ÑÐ¿Ð¸ÑÐ¾Ðº Ð·Ð°Ð±Ð°Ð½ÐµÐ½Ð½Ñ‹Ñ… Ð¸Ð³Ñ€Ð¾ÐºÐ¾Ð²", PluginCommandType.BANNER),
            entry("/decorationbanners [Ð¸Ð³Ñ€Ð¾Ðº] [-m]", "ÐžÑ‚ÐºÑ€Ñ‹Ñ‚ÑŒ Ð³Ð°Ð»ÐµÑ€ÐµÑŽ Ñ„Ð»Ð°Ð³Ð¾Ð² Ð² Ñ€ÐµÐ¶Ð¸Ð¼Ðµ Ð¼Ð¾Ð´ÐµÑ€Ð°Ñ†Ð¸Ð¸", PluginCommandType.DECORATIONBANNERS)
        ),
        "acreative.grab" to listOf(
            entry("/grab <Ð¸Ð³Ñ€Ð¾Ðº> [-force]", "Ð¡Ñ…Ð²Ð°Ñ‚Ð¸Ñ‚ÑŒ Ð¸Ð³Ñ€Ð¾ÐºÐ° Ð¿ÐµÑ€ÐµÐ´ ÑÐ¾Ð±Ð¾Ð¹", PluginCommandType.GRAB)
        ),
        "acreative.slap" to listOf(
            entry("/slap", "ÐŸÐµÑ€ÐµÐºÐ»ÑŽÑ‡Ð¸Ñ‚ÑŒ Ñ€ÐµÐ¶Ð¸Ð¼ Ð¿Ð¾Ñ‰ÐµÑ‡Ð¸Ð½", PluginCommandType.SLAP)
        ),
        "acreative.jar" to listOf(
            entry("/jar <Ð¸Ð³Ñ€Ð¾Ðº> [-const]", "Ð’Ñ‹Ð´Ð°Ñ‚ÑŒ Ð±Ð°Ð½ÐºÑƒ Ð´Ð»Ñ Ð¿Ð¾Ð¸Ð¼ÐºÐ¸ Ð¸Ð³Ñ€Ð¾ÐºÐ°", PluginCommandType.JAR)
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
            appendLine("<#FFD700><st>                      </st><<#FFE68A><b> ÐŸÐ¾Ð»ÐµÐ·Ð½Ñ‹Ðµ ÐºÐ¾Ð¼Ð°Ð½Ð´Ñ‹ </b><#FFD700>><st>                      </st>")
            appendLine("<#EDC800>Ð”Ð¾ÑÑ‚ÑƒÐ¿Ð½Ð¾ Ð´Ð»Ñ ${roleLabel(currentPage.role)}")
            currentPage.entries.forEach { entry ->
                appendLine("<#C7A300> â— <#FFE68A>${escapeMiniMessage(entry.usage)} <#EDC800>- <#FFF3E0>${escapeMiniMessage(entry.description)}")
            }
            appendLine("<#FFD700><st>                           </st> <st>                    </st> <st>          </st>")
            append("<#FFD700>â– <#FFE68A>Ð¡Ñ‚Ñ€Ð°Ð½Ð¸Ñ†Ð°: <#FFF3E0>$currentPageNumber/$totalPages <#FFD700>â†’ ( ${buildNavigation(currentPageNumber, totalPages)} <#FFD700>)")
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
            <#FFD700><st>                      </st><<#FFE68A><b> ÐŸÐ¾Ð»ÐµÐ·Ð½Ñ‹Ðµ ÐºÐ¾Ð¼Ð°Ð½Ð´Ñ‹ </b><#FFD700>><st>                      </st>
            <#EDC800>Ð”Ð»Ñ Ð²Ð°Ñ ÑÐµÐ¹Ñ‡Ð°Ñ Ð½ÐµÑ‚ Ð´Ð¾ÑÑ‚ÑƒÐ¿Ð½Ñ‹Ñ… ÐºÐ¾Ð¼Ð°Ð½Ð´
            <#FFD700><st>                           </st> <st>                    </st> <st>          </st>
            <#FFD700>â– <#FFE68A>Ð¡Ñ‚Ñ€Ð°Ð½Ð¸Ñ†Ð°: <#FFF3E0>1/1
        """.trimIndent()
    }

    private fun escapeMiniMessage(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("<", "\\<")
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
        const val PLAYER_HELP_PREFIX = "<#8C8C8C><b>É¢á´€á´á´‡Ê€</b>"
    }
}

