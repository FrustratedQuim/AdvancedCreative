package com.ratger.acreative.menus.edit

import com.ratger.acreative.menus.edit.head.HeadTextureSource
import com.ratger.acreative.menus.edit.effects.visual.VisualEffectContextKey
import com.ratger.acreative.menus.edit.effects.visual.VisualEffectDraft
import com.ratger.acreative.menus.edit.text.TextStylePalette
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
    var headTextureSource: HeadTextureSource? = null,
    var headTextureValueInputBook: ItemStack? = null,
    var headTextureVirtualValue: String? = null,
    var nameColorFocusIndex: Int = 0,
    var loreColorFocusIndex: Int = 0,
    val orderedNameColors: MutableList<String> = mutableListOf(),
    val orderedLoreColors: MutableList<String> = mutableListOf(),
    var nameShadowKey: String = TextStylePalette.ORDINARY_SHADOW_KEY,
    var loreShadowKey: String = TextStylePalette.ORDINARY_SHADOW_KEY,
    var usesVanillaNameBase: Boolean = false,
    var textStyleStateInitialized: Boolean = false,
    var rawMiniMessageNameInput: String? = null,
    val virtualLoreRawLines: MutableList<String> = mutableListOf(),
    var loreRawFocusIndex: Int = 0,
    var rawTextStyleStateInitialized: Boolean = false,
    var simpleThrowableApplied: Boolean = false,
    var simpleEdibleApplied: Boolean = false,
    var simpleHeadEquippableApplied: Boolean = false,
    var visualEffectContext: VisualEffectContextKey? = null,
    var visualEffectDraft: VisualEffectDraft = VisualEffectDraft(),
    var visualEffectLastTypePage: Int = 0
)
