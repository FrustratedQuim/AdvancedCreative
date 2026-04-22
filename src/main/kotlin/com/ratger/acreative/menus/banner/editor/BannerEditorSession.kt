package com.ratger.acreative.menus.banner.editor

import org.bukkit.DyeColor
import org.bukkit.block.banner.PatternType
import org.bukkit.inventory.ItemStack
import java.util.UUID

data class BannerEditorSession(
    val playerId: UUID,
    val originalMainHandSlot: Int,
    var editableBanner: ItemStack?,
    var openedFromMainMenu: Boolean,
    var isInternalTransition: Boolean = false,
    var pickerPage: Int = 0,
    var selectedPatternType: PatternType? = null,
    var selectedColor: DyeColor? = null
)
