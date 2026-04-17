package com.ratger.acreative.menus.decorationheads.support

import com.ratger.acreative.core.TickScheduler
import ru.violence.coreapi.bukkit.api.menu.Menu
import ru.violence.coreapi.bukkit.api.menu.button.Button

class TemporaryMenuButtonOverrideSupport(
    private val tickScheduler: TickScheduler
) {
    fun replaceSlotTemporarily(
        menu: Menu,
        slot: Int,
        temporaryButton: Button,
        restoreAfterTicks: Long,
        restoreButton: () -> Button
    ) {
        menu.setButton(slot, temporaryButton)
        tickScheduler.runLater(restoreAfterTicks.coerceAtLeast(1L)) {
            menu.setButton(slot, restoreButton())
        }
    }
}
