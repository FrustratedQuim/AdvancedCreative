package com.ratger.acreative.commands.disguise

data class DisguiseRenderProfile(
    val locationOffset: DisguiseLocationOffset = DisguiseLocationOffset.NONE,
    val yawOffset: Float = 0f,
    val mirrorsMainHandIntoBlockState: Boolean = false,
    val respawnsOnMirroredBlockStateChange: Boolean = false,
    val keepsPrimedTntLooping: Boolean = false,
    val motionStabilization: DisguiseMotionStabilization = DisguiseMotionStabilization.NONE
)

data class DisguiseLocationOffset(
    val x: Double = 0.0,
    val y: Double = 0.0,
    val z: Double = 0.0
) {
    companion object {
        val NONE = DisguiseLocationOffset()
    }
}

data class DisguiseMotionStabilization(
    val disablesGravity: Boolean = false,
    val sendsZeroVelocity: Boolean = false,
    val sendsZeroProjectilePower: Boolean = false,
    val followUpTicks: Long = 0L
) {
    val requiresCorrection: Boolean
        get() = sendsZeroVelocity || sendsZeroProjectilePower

    companion object {
        val NONE = DisguiseMotionStabilization()
    }
}
