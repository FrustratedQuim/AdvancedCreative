package com.ratger.acreative.menus.banner.storage

import org.bukkit.inventory.ItemStack
import java.util.UUID

data class BannerStorageSession(
    val playerId: UUID,
    var openedFromMainMenu: Boolean,
    var page: Int = 1,
    var editMode: Boolean = false,
    var isInternalTransition: Boolean = false,
    val layout: MutableMap<Int, ItemStack> = mutableMapOf()
)
