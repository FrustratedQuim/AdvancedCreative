package com.ratger.acreative.utils

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose
import me.tofaa.entitylib.meta.EntityMeta

object EntityLibPoseCompatibility {

    private const val ENTITY_POSE_INDEX: Byte = 6
    private val DIRECT_POSE_PROTOCOLS = setOf(768, 769, 770, 771, 772, 773, 774)

    fun setPose(entityMeta: EntityMeta, pose: EntityPose) {
        if (requiresDirectPoseWrite()) {
            // Entity base tracked-data layout still keeps POSE at index 6 for 1.21.2-1.21.11,
            // but EntityLib 2.4.11 only whitelists protocol 767 and older for pose offsets.
            entityMeta.setIndex(ENTITY_POSE_INDEX, EntityDataTypes.ENTITY_POSE, pose)
            return
        }

        entityMeta.pose = pose
    }

    private fun requiresDirectPoseWrite(): Boolean {
        val protocolVersion = PacketEvents.getAPI().serverManager.version.protocolVersion
        return protocolVersion in DIRECT_POSE_PROTOCOLS
    }
}
