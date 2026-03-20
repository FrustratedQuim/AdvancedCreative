package com.ratger.acreative.commands.jar

import org.bukkit.Location
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.ItemDisplay
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

internal data class JarPlayerState(
    val allowFlight: Boolean,
    val isFlying: Boolean,
    val walkSpeed: Float,
    val flySpeed: Float,
    val scaleBase: Double
)

internal data class JarSession(
    val targetUuid: UUID,
    val ownerUuid: UUID,
    val constFlag: Boolean,
    val supportBlockLocation: Location,
    val plannedJarBlockLocation: Location,
    val visualOrigin: Location,
    val jailedAnchor: Location,
    val rootAnchorEntity: BlockDisplay,
    val displayEntities: MutableList<ItemDisplay>,
    val savedTargetState: JarPlayerState,
    val taskId: Int
)

internal class JarSessionRegistry {
    private val sessionByTargetUuid = ConcurrentHashMap<UUID, JarSession>()
    private val sessionBySupportBlockLocation = ConcurrentHashMap<BlockKey, UUID>()

    fun getByTarget(targetUuid: UUID): JarSession? = sessionByTargetUuid[targetUuid]

    fun getBySupportBlock(location: Location): JarSession? {
        val targetUuid = sessionBySupportBlockLocation[toKey(location)] ?: return null
        return sessionByTargetUuid[targetUuid]
    }

    fun hasTarget(targetUuid: UUID): Boolean = sessionByTargetUuid.containsKey(targetUuid)

    fun upsert(session: JarSession) {
        sessionByTargetUuid[session.targetUuid] = session
        sessionBySupportBlockLocation[toKey(session.supportBlockLocation)] = session.targetUuid
    }

    fun removeByTarget(targetUuid: UUID): JarSession? {
        val session = sessionByTargetUuid.remove(targetUuid) ?: return null
        sessionBySupportBlockLocation.remove(toKey(session.supportBlockLocation))
        return session
    }

    fun allSessions(): List<JarSession> = sessionByTargetUuid.values.toList()

    private fun toKey(location: Location): BlockKey {
        val world = location.world?.uid ?: throw IllegalStateException("Missing world in location")
        return BlockKey(world, location.blockX, location.blockY, location.blockZ)
    }

    private data class BlockKey(val worldUuid: UUID, val x: Int, val y: Int, val z: Int)
}
