package com.ratger.acreative.menus.paint

import com.ratger.acreative.paint.model.PaintSession
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack as BukkitItemStack
import java.util.UUID

interface PaintMenuCallbacks {
    fun isPainting(player: Player): Boolean

    fun session(playerId: UUID): PaintSession?

    fun refreshTools(player: Player, session: PaintSession)

    fun resolveTool(item: BukkitItemStack?): PaintToolDefinition?

    fun clearCanvas(player: Player, session: PaintSession)

    fun removeResizePreview(player: Player, session: PaintSession)
}
