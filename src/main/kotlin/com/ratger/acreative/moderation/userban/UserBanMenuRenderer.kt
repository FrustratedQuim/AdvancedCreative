package com.ratger.acreative.moderation.userban

import com.destroystokyo.paper.profile.ProfileProperty
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.common.MenuUiSupport
import com.ratger.acreative.menus.edit.meta.MiniMessageParser
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.plugin.Plugin
import ru.violence.coreapi.bukkit.api.menu.Menu
import ru.violence.coreapi.bukkit.api.menu.MenuRows
import ru.violence.coreapi.bukkit.api.menu.button.Button
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class UserBanMenuRenderer(
    private val plugin: Plugin,
    private val parser: MiniMessageParser,
    private val sharedButtonFactory: MenuButtonFactory,
    private val title: String
) {
    fun render(
        player: org.bukkit.entity.Player,
        pageResult: UserBanPageResult<UserBanEntry>,
        onEntry: (UserBanEntry) -> Unit,
        onBack: (() -> Unit)?,
        onForward: (() -> Unit)?,
        currentMenu: Menu? = null
    ) {
        val interactive = mutableSetOf<Int>()
        if (onBack != null) interactive += 48
        if (onForward != null) interactive += 50
        interactive += contentSlots(pageResult.entries.size)

        val menuTitle = titleWithPages(title, pageResult.page, pageResult.totalPages)
        val menu = currentMenu ?: buildMenu(menuTitle, interactive)
        if (currentMenu != null) {
            menu.title = parser.parse("<!i>$menuTitle")
            menu.setClickListener { event ->
                if (event.rawSlot in 0 until MenuRows.SIX.size) event.rawSlot in interactive else false
            }
            menu.setDragListener { event -> event.rawSlots.none { it in 0 until MenuRows.SIX.size } }
        }

        clearTopArea(menu)
        fillFooter(menu)
        if (onBack != null) menu.setButton(48, sharedButtonFactory.backButton { onBack() })
        if (onForward != null) menu.setButton(50, sharedButtonFactory.forwardButton { onForward() })
        pageResult.entries.forEachIndexed { index, entry ->
            menu.setButton(index, bannedUserButton(entry) { onEntry(entry) })
        }
        if (currentMenu == null) menu.open(player)
    }

    private fun buildMenu(title: String, interactiveTopSlots: Set<Int>): Menu = MenuUiSupport.buildMenu(
        plugin = plugin,
        parser = parser,
        title = "<!i>$title",
        rows = MenuRows.SIX,
        menuTopRange = 0 until MenuRows.SIX.size,
        interactiveTopSlots = interactiveTopSlots,
        allowPlayerInventoryClicks = false,
        blockShiftClickFromPlayerInventory = false
    )

    private fun bannedUserButton(entry: UserBanEntry, action: () -> Unit): Button {
        val lore = buildList {
            add("<!i><#FFD700>▍ <#FFE68A>Дата: <#FFF3E0>${formatDate(entry.bannedAtEpochMillis)}")
            entry.reason?.takeIf { it.isNotBlank() }?.let {
                add("<!i><#FFD700>▍ <#FFE68A>Пометка: <#FFF3E0>${escapeMiniMessage(it)}")
            }
            add("")
            add("<!i><#FFD700>Нажмите, <#FFE68A>чтобы разбанить")
        }
        return sharedButtonFactory.itemAsIsButton(decorateItem(createProfiledHead(entry.playerName, entry.profileSnapshot), entry.playerName, lore)) { action() }
    }

    private fun decorateItem(item: ItemStack, title: String, lore: List<String>): ItemStack {
        item.editMeta { meta ->
            meta.displayName(parser.parse("<!i><#FFD700>${escapeMiniMessage(title)}"))
            meta.lore(lore.map(parser::parse))
            meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
        }
        return item
    }

    private fun createProfiledHead(name: String, snapshot: UserProfileSnapshot?): ItemStack {
        val head = ItemStack(Material.PLAYER_HEAD)
        val skull = head.itemMeta as? SkullMeta ?: return head
        if (snapshot != null) {
            @Suppress("DEPRECATION")
            val profile = Bukkit.createProfile(null as java.util.UUID?, name)
            profile.setProperty(ProfileProperty("textures", snapshot.value, snapshot.signature))
            skull.playerProfile = profile
        }
        head.itemMeta = skull
        return head
    }

    private fun fillFooter(menu: Menu) {
        menu.setButton(45, sharedButtonFactory.blackFillerButton())
        menu.setButton(53, sharedButtonFactory.blackFillerButton())
        for (slot in 46..52) menu.setButton(slot, sharedButtonFactory.grayFillerButton())
    }

    private fun clearTopArea(menu: Menu) {
        for (slot in 0 until 45) menu.setButton(slot, sharedButtonFactory.itemAsIsButton(ItemStack(Material.AIR)) { })
    }

    private fun contentSlots(count: Int): Set<Int> = (0 until count.coerceAtMost(45)).toSet()

    private fun titleWithPages(baseTitle: String, page: Int, totalPages: Int): String =
        if (totalPages >= 2) "$baseTitle [$page/$totalPages]" else baseTitle

    private fun formatDate(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(epochMillis).atZone(zoneId).format(DATE_FORMATTER)

    private fun escapeMiniMessage(input: String): String = input
        .replace("§", "§\u200B")
        .replace("<", "\\<")
        .replace(">", "\\>")

    private companion object {
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MM.dd.yyyy", Locale.ROOT)
    }
}
