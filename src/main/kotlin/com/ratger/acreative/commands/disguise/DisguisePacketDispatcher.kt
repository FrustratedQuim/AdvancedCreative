package com.ratger.acreative.commands.disguise

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.item.ItemStack as PacketItemStack
import com.github.retrooper.packetevents.util.Vector3d
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMove
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMoveAndRotation
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRotation
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityStatus
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerProjectilePower
import me.tofaa.entitylib.wrapper.WrapperEntity
import me.tofaa.entitylib.wrapper.WrapperLivingEntity
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.Optional
import kotlin.math.abs
import com.github.retrooper.packetevents.protocol.player.Equipment as PacketEquipment
import com.github.retrooper.packetevents.protocol.world.Location as PacketLocation

class DisguisePacketDispatcher {
    data class SharedFlagsState(
        val glowing: Boolean,
        val sneaking: Boolean
    ) {
        fun toMask(): Byte {
            var mask = 0
            if (sneaking) mask = mask or CROUCHING_BIT
            if (glowing) mask = mask or GLOWING_BIT
            return mask.toByte()
        }
    }

    data class LocationSnapshot(
        val x: Double,
        val y: Double,
        val z: Double,
        val yaw: Float,
        val pitch: Float,
        val onGround: Boolean
    ) {
        fun toPacketLocation(): PacketLocation = PacketLocation(x, y, z, yaw, pitch)

        fun hasPositionChangedFrom(other: LocationSnapshot): Boolean {
            return abs(x - other.x) > POSITION_EPSILON ||
                abs(y - other.y) > POSITION_EPSILON ||
                abs(z - other.z) > POSITION_EPSILON ||
                onGround != other.onGround
        }

        fun hasRotationChangedFrom(other: LocationSnapshot): Boolean {
            return normalizedAngleDiff(yaw, other.yaw) > ROTATION_EPSILON ||
                normalizedAngleDiff(pitch, other.pitch) > ROTATION_EPSILON
        }

        fun hasHeadYawChangedFrom(other: LocationSnapshot): Boolean {
            return normalizedAngleDiff(yaw, other.yaw) > ROTATION_EPSILON
        }

        fun canUseRelativeMoveTo(other: LocationSnapshot): Boolean {
            return abs(other.x - x) <= MAX_RELATIVE_DELTA &&
                abs(other.y - y) <= MAX_RELATIVE_DELTA &&
                abs(other.z - z) <= MAX_RELATIVE_DELTA
        }

        companion object {
            fun from(location: Location, onGround: Boolean): LocationSnapshot {
                return LocationSnapshot(
                    x = location.x,
                    y = location.y,
                    z = location.z,
                    yaw = location.yaw,
                    pitch = location.pitch,
                    onGround = onGround
                )
            }
        }
    }

    fun sendCustomName(viewers: Collection<Player>, entityId: Int, customName: Component?) {
        if (viewers.isEmpty()) return
        sendMetadata(
            viewers = viewers,
            entityId = entityId,
            metadata = mutableListOf(
                EntityData(
                    CUSTOM_NAME_INDEX,
                    EntityDataTypes.OPTIONAL_ADV_COMPONENT,
                    Optional.ofNullable(customName)
                ),
                EntityData(
                    CUSTOM_NAME_VISIBLE_INDEX,
                    EntityDataTypes.BOOLEAN,
                    customName != null
                )
            )
        )
    }

    fun sendSharedFlags(viewers: Collection<Player>, entityId: Int, state: SharedFlagsState) {
        if (viewers.isEmpty()) return
        sendMetadata(
            viewers = viewers,
            entityId = entityId,
            metadata = mutableListOf<EntityData<*>>(
                EntityData(
                    SHARED_FLAGS_INDEX,
                    EntityDataTypes.BYTE,
                    state.toMask()
                )
            )
        )
    }

    fun sendEquipment(viewers: Collection<Player>, entityId: Int, equipment: List<PacketEquipment>) {
        if (viewers.isEmpty()) return
        sendPacket(viewers, WrapperPlayServerEntityEquipment(entityId, equipment))
    }

    fun sendPrimaryItemMetadata(viewers: Collection<Player>, entityId: Int, item: PacketItemStack) {
        if (viewers.isEmpty()) return
        sendMetadata(
            viewers = viewers,
            entityId = entityId,
            metadata = mutableListOf<EntityData<*>>(
                EntityData(
                    PRIMARY_ITEM_METADATA_INDEX,
                    EntityDataTypes.ITEMSTACK,
                    item
                )
            )
        )
    }

    fun sendMovement(
        viewers: Collection<Player>,
        entity: WrapperEntity,
        previous: LocationSnapshot?,
        current: LocationSnapshot
    ) {
        if (viewers.isEmpty()) {
            entity.setLocation(current.toPacketLocation())
            return
        }

        val packet = if (previous == null) {
            WrapperPlayServerEntityTeleport(entity.entityId, current.toPacketLocation(), current.onGround)
        } else {
            val positionChanged = previous.hasPositionChangedFrom(current)
            val rotationChanged = previous.hasRotationChangedFrom(current)
            when {
                !positionChanged && !rotationChanged -> null
                !positionChanged -> WrapperPlayServerEntityRotation(
                    entity.entityId,
                    current.yaw,
                    current.pitch,
                    current.onGround
                )

                rotationChanged && previous.canUseRelativeMoveTo(current) ->
                    WrapperPlayServerEntityRelativeMoveAndRotation(
                        entity.entityId,
                        current.x - previous.x,
                        current.y - previous.y,
                        current.z - previous.z,
                        current.yaw,
                        current.pitch,
                        current.onGround
                    )

                previous.canUseRelativeMoveTo(current) ->
                    WrapperPlayServerEntityRelativeMove(
                        entity.entityId,
                        current.x - previous.x,
                        current.y - previous.y,
                        current.z - previous.z,
                        current.onGround
                    )

                else -> WrapperPlayServerEntityTeleport(entity.entityId, current.toPacketLocation(), current.onGround)
            }
        }

        packet?.let { sendPacket(viewers, it) }
        entity.setLocation(current.toPacketLocation())
    }

    fun sendHeadRotation(viewers: Collection<Player>, entityId: Int, yaw: Float) {
        if (viewers.isEmpty()) return
        sendPacket(viewers, WrapperPlayServerEntityHeadLook(entityId, yaw))
    }

    fun sendVelocity(viewers: Collection<Player>, entityId: Int, velocity: Vector3d) {
        if (viewers.isEmpty()) return
        sendPacket(viewers, WrapperPlayServerEntityVelocity(entityId, velocity))
    }

    fun sendProjectilePower(viewers: Collection<Player>, entityId: Int, power: Double) {
        if (viewers.isEmpty()) return
        sendPacket(viewers, WrapperPlayServerProjectilePower(entityId, power))
    }

    fun sendTeleport(viewers: Collection<Player>, entityId: Int, location: LocationSnapshot) {
        if (viewers.isEmpty()) return
        sendPacket(viewers, WrapperPlayServerEntityTeleport(entityId, location.toPacketLocation(), location.onGround))
    }

    fun sendEntityStatus(viewers: Collection<Player>, entityId: Int, status: Int) {
        if (viewers.isEmpty()) return
        sendPacket(viewers, WrapperPlayServerEntityStatus(entityId, status))
    }

    fun playAttackAnimation(entity: WrapperEntity, animation: DisguiseAttackAnimation) {
        when (animation) {
            DisguiseAttackAnimation.None -> return
            is DisguiseAttackAnimation.EntityStatus -> {
                entity.sendPacketToViewers(WrapperPlayServerEntityStatus(entity.entityId, animation.status))
            }
            DisguiseAttackAnimation.SwingMainHand -> playSwing(entity)
        }
    }

    private fun playSwing(entity: WrapperEntity) {
        val living = entity as? WrapperLivingEntity
        if (living != null) {
            living.swingMainHand()
            return
        }

        entity.sendPacketToViewers(
            WrapperPlayServerEntityAnimation(
                entity.entityId,
                WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM
            )
        )
    }

    fun applyVelocity(entity: WrapperEntity, velocity: Vector3d) {
        entity.velocity = velocity
    }

    fun sendMetadata(viewers: Collection<Player>, entityId: Int, metadata: List<EntityData<*>>) {
        sendPacket(viewers, WrapperPlayServerEntityMetadata(entityId, metadata))
    }

    private fun sendPacket(viewers: Collection<Player>, packet: PacketWrapper<*>) {
        viewers.forEach { viewer ->
            PacketEvents.getAPI().playerManager.sendPacket(viewer, packet)
        }
    }

    private companion object {
        const val SHARED_FLAGS_INDEX = 0
        const val CUSTOM_NAME_INDEX = 2
        const val CUSTOM_NAME_VISIBLE_INDEX = 3
        const val PRIMARY_ITEM_METADATA_INDEX = 8

        const val CROUCHING_BIT = 0x02
        const val GLOWING_BIT = 0x40

        const val MAX_RELATIVE_DELTA = 7.999
        const val POSITION_EPSILON = 0.0001
        const val ROTATION_EPSILON = 0.05f

        fun normalizedAngleDiff(left: Float, right: Float): Float {
            var diff = (left - right) % 360f
            if (diff < -180f) diff += 360f
            if (diff > 180f) diff -= 360f
            return abs(diff)
        }
    }
}
