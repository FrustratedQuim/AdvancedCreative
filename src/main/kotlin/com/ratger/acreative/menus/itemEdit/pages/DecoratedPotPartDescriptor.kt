package com.ratger.acreative.menus.itemEdit.pages

import com.ratger.acreative.itemedit.trim.DecoratedPotSide

enum class DecoratedPotPartDescriptor(
    val rootSlot: Int,
    val side: DecoratedPotSide,
    val partLabel: String,
    val patternTitle: String
) {
    PART_ONE(29, DecoratedPotSide.BACK, "①", "<!i>▍ Ваза → Паттерн #1"),
    PART_TWO(30, DecoratedPotSide.LEFT, "②", "<!i>▍ Ваза → Паттерн #2"),
    PART_THREE(32, DecoratedPotSide.RIGHT, "③", "<!i>▍ Ваза → Паттерн #3"),
    PART_FOUR(33, DecoratedPotSide.FRONT, "④", "<!i>▍ Ваза → Паттерн #4")
}
