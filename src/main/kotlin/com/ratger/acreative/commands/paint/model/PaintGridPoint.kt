package com.ratger.acreative.commands.paint.model

data class PaintGridPoint(
    val x: Int,
    val y: Int
) {
    fun mapId(session: PaintSession): Int {
        return session.canvasCells[this]?.mapId ?: -1
    }
}