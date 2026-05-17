package com.ratger.acreative.commands.paint.rendering

import com.ratger.acreative.commands.paint.model.PaintSession
import java.util.UUID

class ViewerManager(
    private val entityVisualFactory: EntityVisualFactory
) {

    fun addViewer(session: PaintSession, viewerId: UUID) {
        if (session.viewers.contains(viewerId)) return
        session.viewers.add(viewerId)

        session.canvasCells.values.forEach { cell ->
            entityVisualFactory.addViewer(cell, viewerId)
        }
    }

    fun removeViewer(session: PaintSession, viewerId: UUID) {
        if (!session.viewers.contains(viewerId)) return
        session.viewers.remove(viewerId)

        session.canvasCells.values.forEach { cell ->
            entityVisualFactory.removeViewer(cell, viewerId)
        }
    }
}
