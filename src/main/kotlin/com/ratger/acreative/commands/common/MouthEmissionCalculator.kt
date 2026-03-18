package com.ratger.acreative.commands.common

import com.ratger.acreative.core.FunctionHooker
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.util.Vector

    class MouthEmissionCalculator(private val hooker: FunctionHooker) {

    data class EmissionContext(
        val origin: org.bukkit.Location,
        val direction: Vector,
        val scale: Double
    )

    fun calculate(player: Player): EmissionContext {
        val isLaying = hooker.utils.isLaying(player)
        val pitch = if (isLaying) -90f else player.location.pitch.coerceIn(-90f, 90f)
        val scale = player.getAttribute(Attribute.GENERIC_SCALE)?.value ?: 1.0

        val yOffsetBase = if (isLaying) 1.2f else 1.9f - ((pitch + 90f) / 180f) * (1.9f - 1.2f)
        val yOffset = yOffsetBase * scale

        val forwardOffsetBase = if (isLaying) 0f else if (pitch <= 0) {
            -0.1f + ((pitch + 90f) / 90f) * (0.6f + 0.1f)
        } else {
            0.6f - (pitch / 90f) * (0.6f - 0.2f)
        }
        val forwardOffset = forwardOffsetBase * scale

        val direction = if (isLaying) Vector(0.0, 1.0, 0.0) else player.location.direction.clone()
        val horizontalDirection = if (isLaying) {
            Vector(0.0, 0.0, 0.0)
        } else {
            Vector(direction.x, 0.0, direction.z).normalize().multiply(forwardOffset)
        }

        val origin = player.location.clone()
            .add(horizontalDirection)
            .add(0.0, yOffset, 0.0)

            return EmissionContext(origin, direction, scale)
    }

}
