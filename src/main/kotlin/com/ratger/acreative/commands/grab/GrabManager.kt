package com.ratger.acreative.commands.grab

import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.MessageKey
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import java.util.UUID
import com.ratger.acreative.utils.PlayerStateManager.PlayerStateType

class GrabManager(private val hooker: FunctionHooker) {

    private data class PlayerFlightState(
        val allowFlight: Boolean,
        val isFlying: Boolean,
        val walkSpeed: Float,
        val flySpeed: Float
    )

    private data class GrabSession(
        val holderId: UUID,
        val targetId: UUID,
        var distance: Double,
        val targetFlightState: PlayerFlightState,
        val taskId: Int,
        var darknessCooldownTicks: Int = 0,
        var darknessApplied: Boolean = false
    )

    private val sessionByHolder = mutableMapOf<UUID, GrabSession>()
    private val holderByTarget = mutableMapOf<UUID, UUID>()

    fun handleGrabCommand(holder: Player, args: Array<out String>) {
        val targetName = args.getOrNull(0)
        val forcePull = args.drop(1).any { it.equals("-force", ignoreCase = true) }

        if (targetName == null) {
            if (sessionByHolder.containsKey(holder.uniqueId)) {
                releaseSession(holder.uniqueId)
                return
            }
            hooker.messageManager.sendChat(holder, MessageKey.USAGE_GRAB)
            return
        }

        val target = Bukkit.getPlayer(targetName)
        if (target == null) {
            hooker.messageManager.sendChat(holder, MessageKey.ERROR_UNKNOWN_PLAYER)
            return
        }

        if (target == holder) {
            hooker.messageManager.sendChat(holder, MessageKey.ERROR_GRAB_SELF)
            return
        }

        if (sessionByHolder[holder.uniqueId]?.targetId == target.uniqueId) {
            releaseSession(holder.uniqueId)
            return
        }

        if (holder.world != target.world || holder.location.distance(target.location) > MAX_START_DISTANCE) {
            hooker.messageManager.sendChat(holder, MessageKey.ERROR_GRAB_TOO_FAR)
            return
        }

        val currentHolder = holderByTarget[target.uniqueId]
        if (currentHolder != null && currentHolder != holder.uniqueId) {
            hooker.messageManager.sendChat(holder, MessageKey.ERROR_GRAB_TARGET_BUSY)
            return
        }

        releaseSession(holder.uniqueId)
        startSession(holder, target, forcePull)
    }

    fun releaseSession(holderId: UUID) {
        val session = sessionByHolder.remove(holderId) ?: return
        holderByTarget.remove(session.targetId)
        hooker.tickScheduler.cancel(session.taskId)

        val holder = Bukkit.getPlayer(session.holderId)
        val target = Bukkit.getPlayer(session.targetId)

        target?.let {
            hooker.effectsManager.removeInternalEffect(GRAB_EFFECT_OWNER, it, PotionEffectType.DARKNESS)
            hooker.playerStateManager.deactivateState(it, PlayerStateType.GRABBED)
            restorePlayerState(it, session.targetFlightState)
            hooker.playerStateManager.refreshPlayerPose(it)
        }

        holder?.let {
            hooker.playerStateManager.deactivateState(it, PlayerStateType.GRABBING)
            hooker.playerStateManager.restorePlayerInventory(it)
            hooker.playerStateManager.refreshPlayerPose(it)
        }
    }

    fun releaseAll() {
        sessionByHolder.keys.toList().forEach { releaseSession(it) }
    }

    fun releaseForPlayer(player: Player) {
        val holderId = player.uniqueId
        if (sessionByHolder.containsKey(holderId)) {
            releaseSession(holderId)
            return
        }
        holderByTarget[holderId]?.let { releaseSession(it) }
    }

    fun isGrabbed(player: Player): Boolean {
        val playerId = player.uniqueId
        return sessionByHolder.containsKey(playerId) || holderByTarget.containsKey(playerId)
    }

    fun handleHolderAttack(holder: Player, attackedPlayer: Player): Boolean {
        val session = sessionByHolder[holder.uniqueId] ?: return false
        if (attackedPlayer.uniqueId != session.targetId) return false

        val launchDirection = holder.location.direction.clone().normalize().apply {
            y += 0.5
            multiply(1.0)
        }
        releaseSession(holder.uniqueId)
        hooker.tickScheduler.runLater(1L) {
            if (attackedPlayer.isOnline) {
                attackedPlayer.velocity = launchDirection
            }
        }
        return true
    }

    fun onHolderHotbarScroll(holder: Player, previousSlot: Int, newSlot: Int) {
        val session = sessionByHolder[holder.uniqueId] ?: return
        val delta = hotbarScrollDelta(previousSlot, newSlot)
        if (delta != 0) {
            val scrollStep = dynamicScrollStep(session.distance)
            session.distance = (session.distance - (delta * scrollStep)).coerceIn(MIN_DISTANCE, MAX_DISTANCE)
        }
    }

    fun enforceGrabbedFlight(player: Player): Boolean {
        if (!isGrabbedTarget(player.uniqueId)) return false
        player.allowFlight = true
        player.isFlying = true
        return true
    }

    fun blockGrabbedGlide(player: Player, isTryingToGlide: Boolean): Boolean {
        if (!isGrabbedTarget(player.uniqueId)) return false
        player.isGliding = false
        return isTryingToGlide
    }

    fun blockGrabbedCommand(player: Player): Boolean = isGrabbedTarget(player.uniqueId)

    fun blockGrabbedInteraction(player: Player, action: Action): Boolean {
        if (!isGrabbedTarget(player.uniqueId)) return false
        return action == Action.RIGHT_CLICK_AIR ||
            action == Action.RIGHT_CLICK_BLOCK ||
            action == Action.LEFT_CLICK_AIR ||
            action == Action.LEFT_CLICK_BLOCK
    }

    fun blockGrabbedEntityInteraction(player: Player): Boolean = isGrabbedTarget(player.uniqueId)

    fun blockGrabbedInteractAtEntity(player: Player): Boolean = isGrabbedTarget(player.uniqueId)

    fun blockGrabbedBlockBreak(player: Player): Boolean = isGrabbedTarget(player.uniqueId)

    fun blockGrabbedDamage(player: Player): Boolean = isGrabbedTarget(player.uniqueId)

    fun cleanupSessionsForPlayer(playerId: UUID) {
        releaseSession(playerId)
        holderByTarget[playerId]?.let { releaseSession(it) }
    }

    private fun startSession(holder: Player, target: Player, forcePull: Boolean) {
        clearGrabConflictingState(target)
        clearGrabConflictingState(holder)
        if (target.gameMode == GameMode.SPECTATOR) {
            target.gameMode = GameMode.CREATIVE
        }

        hooker.playerStateManager.activateState(holder, PlayerStateType.GRABBING)
        hooker.playerStateManager.activateState(target, PlayerStateType.GRABBED)
        val targetFlightState = capturePlayerState(target)

        hooker.playerStateManager.savePlayerInventory(holder)

        clearInventory(holder)

        hooker.effectsManager.clearEffects(target, sendMessage = false)

        target.walkSpeed = 0f
        target.flySpeed = 0f
        target.allowFlight = true
        target.isFlying = true
        target.fallDistance = 0f

        val distance = if (forcePull) {
            MIN_DISTANCE
        } else {
            holder.eyeLocation.distance(target.location).coerceIn(MIN_DISTANCE, MAX_DISTANCE)
        }

        var taskId = 0
        taskId = hooker.tickScheduler.runRepeating(0L, 1L) {
            val liveHolder = Bukkit.getPlayer(holder.uniqueId)
            val liveTarget = Bukkit.getPlayer(target.uniqueId)
            val session = sessionByHolder[holder.uniqueId]
            if (liveHolder == null || liveTarget == null || session == null) {
                releaseSession(holder.uniqueId)
                return@runRepeating
            }

            if (!liveHolder.isOnline || !liveTarget.isOnline || liveHolder.isDead || liveTarget.isDead) {
                releaseSession(holder.uniqueId)
                return@runRepeating
            }

            if (liveHolder.world != liveTarget.world) {
                releaseSession(holder.uniqueId)
                return@runRepeating
            }

            updateTargetVelocity(liveHolder, liveTarget, session.distance)
            updateBlindness(liveHolder, liveTarget, session)

            liveTarget.allowFlight = true
            liveTarget.isFlying = true
            liveTarget.fallDistance = 0f

            if (liveHolder.ticksLived % PARTICLE_PERIOD_TICKS == 0) {
                val particleLocation = liveTarget.location.clone().add(0.0, 0.8, 0.0)
                liveTarget.world.spawnParticle(Particle.SMOKE, particleLocation, 20, 0.3, 0.0, 0.3, 0.0)
            }
        }

        sessionByHolder[holder.uniqueId] = GrabSession(
            holderId = holder.uniqueId,
            targetId = target.uniqueId,
            distance = distance,
            targetFlightState = targetFlightState,
            taskId = taskId
        )
        holderByTarget[target.uniqueId] = holder.uniqueId

        target.playSound(target.location, Sound.ENTITY_WARDEN_AGITATED, 1.0f, 1.0f)
        hooker.messageManager.sendChat(holder, MessageKey.INFO_GRAB_STARTED, mapOf("target" to target.name))
    }

    private fun clearGrabConflictingState(target: Player) {
        hooker.jarManagerOrNull()?.releaseForPlayer(target, waitForScaleRestore = false)
        hooker.utils.unsetAllPoses(target, true)
        hooker.utils.unsetAllStates(target)
        hooker.utils.checkDisguiseDisable(target)
        hooker.utils.checkCustomEffectsDisable(target)
        hooker.utils.checkSlapUnslap(target)
    }

    private fun capturePlayerState(player: Player): PlayerFlightState {
        return PlayerFlightState(
            allowFlight = player.allowFlight,
            isFlying = player.isFlying,
            walkSpeed = player.walkSpeed,
            flySpeed = player.flySpeed
        )
    }

    private fun restorePlayerState(player: Player, state: PlayerFlightState) {
        player.allowFlight = state.allowFlight
        player.isFlying = state.isFlying
        player.walkSpeed = state.walkSpeed
        player.flySpeed = state.flySpeed
        player.fallDistance = 0f
    }

    private fun clearInventory(player: Player) {
        player.inventory.setItemInMainHand(null)
        player.inventory.setItemInOffHand(null)
        player.inventory.armorContents = arrayOf(null, null, null, null)
    }

    private fun updateTargetVelocity(holder: Player, target: Player, distance: Double) {
        val eye = holder.eyeLocation
        val direction = eye.direction.normalize()
        val anchor = eye.clone().add(direction.multiply(distance))
        val desiredPosition = anchor.add(0.0, target.height * 0.5 - 1.2, 0.0)
        val currentPosition = target.location.clone().add(0.0, target.height * 0.5, 0.0)

        val delta = desiredPosition.toVector().subtract(currentPosition.toVector())
        if (delta.lengthSquared() <= DELTA_EPSILON_SQUARED) {
            target.velocity = Vector(0, 0, 0)
            return
        }

        val deltaLength = delta.length()
        val followFactor = BASE_FOLLOW_FACTOR + (deltaLength * DISTANCE_FOLLOW_SCALE).coerceAtMost(MAX_EXTRA_FOLLOW_FACTOR)
        val velocity = delta.multiply(followFactor)
        val maxVelocity = (BASE_MAX_VELOCITY + deltaLength * DISTANCE_MAX_VELOCITY_SCALE).coerceAtMost(HARD_MAX_VELOCITY)
        if (velocity.lengthSquared() > maxVelocity * maxVelocity) {
            velocity.normalize().multiply(maxVelocity)
        }
        target.velocity = velocity
    }

    private fun faceTargetToHolderHead(target: Player, holder: Player) {
        val holderHead = holder.eyeLocation.toVector()
        val targetEyes = target.eyeLocation.toVector()
        val direction = holderHead.subtract(targetEyes).normalize()
        if (direction.lengthSquared() <= 0.0001) return

        val yaw = Math.toDegrees(kotlin.math.atan2(-direction.x, direction.z)).toFloat()
        val pitch = Math.toDegrees(kotlin.math.asin(-direction.y)).toFloat().coerceIn(-90f, 90f)
        target.setRotation(yaw, pitch)
    }

    private fun updateBlindness(holder: Player, target: Player, session: GrabSession) {
        val distance = holder.location.distance(target.location)
        if (distance <= DARKNESS_DISTANCE) {
            faceTargetToHolderHead(target, holder)
            if (!session.darknessApplied || session.darknessCooldownTicks <= 0 || !target.hasPotionEffect(PotionEffectType.DARKNESS)) {
                hooker.effectsManager.applyInternalEffect(
                    GRAB_EFFECT_OWNER,
                    target,
                    PotionEffectType.DARKNESS,
                    DARKNESS_DURATION_TICKS,
                    0
                )
                session.darknessApplied = true
                session.darknessCooldownTicks = DARKNESS_REFRESH_TICKS
            } else {
                session.darknessCooldownTicks--
            }
            return
        }
        session.darknessCooldownTicks = 0
        session.darknessApplied = false
        hooker.effectsManager.removeInternalEffect(GRAB_EFFECT_OWNER, target, PotionEffectType.DARKNESS)
    }

    private fun isGrabbedTarget(playerId: UUID): Boolean = holderByTarget.containsKey(playerId)

    private fun hotbarScrollDelta(previousSlot: Int, newSlot: Int): Int {
        val step = ((newSlot - previousSlot + 9) % 9)
        if (step == 0) return 0
        return if (step <= 4) step else step - 9
    }

    private fun dynamicScrollStep(distance: Double): Double {
        if (distance <= 10.0) return 1.0
        return (distance / 10.0).coerceAtMost(8.0)
    }

    companion object {
        private const val MAX_START_DISTANCE = 75.0
        private const val MIN_DISTANCE = 1.0
        private const val MAX_DISTANCE = 100.0
        private const val BASE_FOLLOW_FACTOR = 0.24
        private const val DISTANCE_FOLLOW_SCALE = 0.03
        private const val MAX_EXTRA_FOLLOW_FACTOR = 0.7
        private const val BASE_MAX_VELOCITY = 1.05
        private const val DISTANCE_MAX_VELOCITY_SCALE = 0.09
        private const val HARD_MAX_VELOCITY = 4.2
        private const val DELTA_EPSILON_SQUARED = 0.0009
        private const val DARKNESS_DISTANCE = 5.0
        private const val DARKNESS_DURATION_TICKS = 200
        private const val DARKNESS_REFRESH_TICKS = 20
        private const val PARTICLE_PERIOD_TICKS = 2
        private const val GRAB_EFFECT_OWNER = "grab"
    }
}
