package com.ratger.acreative.integration.plotsquared.flags

import com.plotsquared.core.configuration.caption.StaticCaption
import com.plotsquared.core.plot.flag.types.BooleanFlag

class PlotPaintFlag private constructor(value: Boolean) : BooleanFlag<PlotPaintFlag>(
    value,
    StaticCaption.of("Allow using AdvancedCreative paint on this plot")
) {

    override fun flagOf(value: Boolean): PlotPaintFlag {
        return if (value) TRUE else FALSE
    }

    companion object {
        val TRUE = PlotPaintFlag(true)
        val FALSE = PlotPaintFlag(false)
    }
}
