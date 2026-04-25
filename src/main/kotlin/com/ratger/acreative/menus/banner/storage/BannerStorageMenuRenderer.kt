package com.ratger.acreative.menus.banner.storage

import com.ratger.acreative.menus.banner.BannerButtonFactory
import com.ratger.acreative.menus.banner.service.BannerTextSupport
import com.ratger.acreative.menus.common.MenuUiSupport
import com.ratger.acreative.menus.edit.meta.MiniMessageParser
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import ru.violence.coreapi.bukkit.api.menu.Menu
import ru.violence.coreapi.bukkit.api.menu.MenuRows

class BannerStorageMenuRenderer(
    private val plugin: Plugin,
    private val parser: MiniMessageParser,
    private val buttonFactory: BannerButtonFactory
) {
    fun render(
        player: Player,
        session: BannerStorageSession,
        pageItems: Map<Int, org.bukkit.inventory.ItemStack>,
        totalPages: Int,
        limit: BannerStorageService.LimitSnapshot,
        onStoredBanner: (org.bukkit.inventory.ItemStack, ru.violence.coreapi.bukkit.api.menu.event.ClickEvent) -> Unit,
        onInfo: () -> Unit,
        onModeToggle: () -> Unit,
        onBack: (() -> Unit)?,
        onForward: (() -> Unit)?,
        onClose: (Player) -> Unit,
        editClickListener: ((ru.violence.coreapi.bukkit.api.menu.event.ClickEvent) -> Boolean)?
    ) {
        val titleBase = "▍ Хранилище флагов"
        val title = BannerTextSupport.titleWithPages(titleBase, session.page, totalPages)

        val interactiveTop = mutableSetOf<Int>()
        interactiveTop += if (session.editMode) {
            (0 until 45)
        } else {
            pageItems.keys
        }
        interactiveTop += setOf(46, 48, 49, 52)
        if (onForward != null) interactiveTop += 50
        if (onBack != null) interactiveTop += 48

        val menu = MenuUiSupport.buildMenu(
            plugin = plugin,
            parser = parser,
            title = "<!i>$title",
            rows = MenuRows.SIX,
            menuTopRange = 0 until 54,
            interactiveTopSlots = interactiveTop,
            allowPlayerInventoryClicks = true,
            blockShiftClickFromPlayerInventory = !session.editMode,
            onClose = { closeEvent ->
                val closePlayer = closeEvent.player
                onClose(closePlayer)
            }
        )

        if (session.editMode && editClickListener != null) {
            menu.setClickListener(editClickListener)
        }

        if (session.editMode) {
            fillEditFooter(menu)
            pageItems.forEach { (slot, item) ->
                menu.setButton(slot, buttonFactory.storageStoredBannerButton(item) { _ -> })
            }
            menu.setButton(46, buttonFactory.storageEditInfoButton { onInfo() })
            menu.setButton(49, buttonFactory.storageModeButton(true) { onModeToggle() })
        } else {
            fillDefaultFooter(menu)
            pageItems.forEach { (slot, item) ->
                menu.setButton(slot, buttonFactory.storageStoredBannerButton(item) { onStoredBanner(item, it) })
            }
            menu.setButton(46, buttonFactory.storageInfoButton { onInfo() })
            menu.setButton(49, buttonFactory.storageModeButton(false) { onModeToggle() })
        }

        onBack?.let { menu.setButton(48, buttonFactory.backButton { it() }) }
        onForward?.let { menu.setButton(50, buttonFactory.forwardButton { it() }) }
        menu.setButton(52, buttonFactory.storageLimitButton(limit.current, limit.limitText))

        menu.open(player)
    }

    private fun fillDefaultFooter(menu: Menu) {
        menu.setButton(45, buttonFactory.blackFiller())
        menu.setButton(53, buttonFactory.blackFiller())
        for (slot in 46..52) {
            menu.setButton(slot, buttonFactory.grayFiller())
        }
    }

    private fun fillEditFooter(menu: Menu) {
        for (slot in 45..53) {
            menu.setButton(slot, buttonFactory.whiteFillerButton())
        }
    }
}
