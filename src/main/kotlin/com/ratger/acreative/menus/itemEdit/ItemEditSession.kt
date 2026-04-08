package com.ratger.acreative.menus.itemEdit

import com.ratger.acreative.itemedit.head.HeadTextureSource
import org.bukkit.inventory.ItemStack
import java.util.UUID

data class ItemEditSession(
    val playerId: UUID,
    val originalMainHandSlot: Int,
    var editableItem: ItemStack,
    var isInternalTransition: Boolean = false,
    var hiddenInfoFocusIndex: Int = 0,
    var vanillaDiscJukeboxComponentInjected: Boolean = false,
    var attributesMaterializedForHide: Boolean = false,
    var headTextureSectionActive: Boolean = false,
    var headTextureLoadingToken: Long? = null,
    var headTextureOpSequence: Long = 0,
    var headTextureSource: HeadTextureSource? = null
)
