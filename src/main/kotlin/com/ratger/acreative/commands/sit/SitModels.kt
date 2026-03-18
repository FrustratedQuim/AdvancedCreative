package com.ratger.acreative.commands.sit

import org.bukkit.block.Block
import java.util.UUID

enum class SitStyle {
    BASIC,
    STAIRS,
    SLAB,
    HEAD
}

data class SitSession(
    val armorStandId: UUID,
    val block: Block?,
    val style: SitStyle
)
