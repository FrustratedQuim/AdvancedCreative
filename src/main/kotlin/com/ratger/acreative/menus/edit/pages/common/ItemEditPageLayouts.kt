package com.ratger.acreative.menus.edit.pages.common

import ru.violence.coreapi.bukkit.api.menu.MenuRows

object ItemEditPageLayouts {
    val standardEditorBlackSlots = setOf(0, 8, 9, 17, 18, 26, 27, 35, 36, 44, 12, 14)

    val pagedList = PagedListLayout(
        menuSize = 45,
        rows = MenuRows.FIVE,
        blackSlots = setOf(0, 8, 9, 17, 18, 26, 27, 35, 36, 44),
        graySlots = setOf(
            1, 2, 3, 4, 5, 6, 7,
            10, 16,
            19, 25,
            28, 34,
            37, 38, 39, 40, 41, 42, 43
        ),
        workSlots = listOf(11, 12, 13, 14, 15, 20, 21, 22, 23, 24, 29, 30, 31, 32, 33),
        backSlot = 18,
        forwardSlot = 26,
        addCommandSlot = 38,
        addMenuSlot = 40,
        clearSlot = 42
    )
}

data class PagedListLayout(
    val menuSize: Int,
    val rows: MenuRows,
    val blackSlots: Set<Int>,
    val graySlots: Set<Int>,
    val workSlots: List<Int>,
    val backSlot: Int,
    val forwardSlot: Int,
    val addCommandSlot: Int,
    val addMenuSlot: Int,
    val clearSlot: Int
) {
    val interactiveTopSlots: Set<Int>
        get() = setOf(backSlot, forwardSlot, addCommandSlot, addMenuSlot, clearSlot) + workSlots
}
