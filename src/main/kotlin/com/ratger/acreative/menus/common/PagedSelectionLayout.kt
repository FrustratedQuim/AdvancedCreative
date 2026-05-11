package com.ratger.acreative.menus.common

object PagedSelectionLayout {
    val blackSlots: Set<Int> = setOf(0, 8, 9, 17, 18, 26, 27, 35, 36, 44)
    val graySlots: Set<Int> = setOf(
        1, 2, 3, 4, 5, 6, 7,
        37, 38, 39, 40, 41, 42, 43
    )
    val workSlots: List<Int> = listOf(
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34
    )

    const val backSlot: Int = 18
    const val forwardSlot: Int = 26
}
