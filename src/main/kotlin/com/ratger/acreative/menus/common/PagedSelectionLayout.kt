package com.ratger.acreative.menus.common

object PagedSelectionLayout {
    const val CONTENT_SIZE: Int = 45
    val contentSlots: IntRange = 0 until CONTENT_SIZE

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

    const val BACK_SLOT: Int = 18
    const val FORWARD_SLOT: Int = 26
    const val FOOTER_START_SLOT: Int = 45
    const val FOOTER_END_SLOT: Int = 53
    val footerControlSlots: IntRange = (FOOTER_START_SLOT + 1) until FOOTER_END_SLOT

    val mirroredBackSlots: Set<Int> = setOf(BACK_SLOT, 27)
    val mirroredForwardSlots: Set<Int> = setOf(FORWARD_SLOT, 35)
    val footerCornerSlots: Set<Int> = setOf(FOOTER_START_SLOT, FOOTER_END_SLOT)
}
