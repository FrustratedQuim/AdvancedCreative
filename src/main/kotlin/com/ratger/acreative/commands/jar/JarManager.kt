package com.ratger.acreative.commands.jar

import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.MessageKey
import com.ratger.acreative.utils.PlayerStateManager.PlayerStateType
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.GameMode
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Vector
import java.util.UUID

class JarManager(private val hooker: FunctionHooker) {

    private val displayFactory = JarDisplayFactory(hooker)
    private val sessions = JarSessionRegistry()
    private val miniMessage = MiniMessage.miniMessage()

    private val keyMarker = NamespacedKey(hooker.plugin, "jar_marker")
    private val keyTargetUuid = NamespacedKey(hooker.plugin, "jar_target_uuid")
    private val keyTargetName = NamespacedKey(hooker.plugin, "jar_target_name")
    private val keyConst = NamespacedKey(hooker.plugin, "jar_const")
    private val releaseInProgress = mutableSetOf<UUID>()
    private val pendingReleaseCallbacks = mutableMapOf<UUID, MutableList<() -> Unit>>()
    private val movementBypassTargets = mutableSetOf<UUID>()

    fun handleJarCommand(owner: Player, args: Array<out String>) {
        val targetName = args.getOrNull(0)
        if (targetName == null) {
            hooker.messageManager.sendChat(owner, MessageKey.USAGE_JAR)
            return
        }

        val target = Bukkit.getPlayer(targetName)
        if (target == null) {
            hooker.messageManager.sendChat(owner, MessageKey.ERROR_UNKNOWN_PLAYER)
            return
        }

        if (target.uniqueId == owner.uniqueId) {
            hooker.messageManager.sendChat(owner, MessageKey.ERROR_GRAB_SELF)
            return
        }

        val existing = sessions.getByTarget(target.uniqueId)
        if (existing != null) {
            if (existing.ownerUuid == owner.uniqueId) {
                releaseSession(target.uniqueId)
            } else {
                hooker.messageManager.sendChat(owner, MessageKey.ERROR_JAR_TARGET_BUSY)
            }
            return
        }

        val freeHotbarSlot = owner.inventory.firstEmptyHotbarSlot()
        if (freeHotbarSlot == null) {
            hooker.messageManager.sendChat(owner, MessageKey.ERROR_JAR_HAND_NOT_EMPTY)
            return
        }

        val constFlag = args.drop(1).any { it.equals("-const", ignoreCase = true) }
        owner.inventory.setItem(freeHotbarSlot, createJarItem(target.uniqueId, target.name, constFlag))
        owner.inventory.heldItemSlot = freeHotbarSlot
        hooker.messageManager.sendChat(owner, MessageKey.INFO_JAR_ITEM_GIVEN, mapOf("target" to target.name))
    }

    fun handleJarBlockPlace(event: BlockPlaceEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        val player = event.player
        val item = event.itemInHand
        val data = readJarData(item) ?: return

        event.isCancelled = true

        if (!hasJarPermission(player)) {
            hooker.permissionManager.sendPermissionDenied(player, "jar")
            return
        }

        val plannedJarBlock = event.blockPlaced
        val supportBlock = plannedJarBlock.getRelative(0, -1, 0)
        if (!supportBlock.type.isSolid) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_JAR_NO_SUPPORT)
            return
        }

        val target = Bukkit.getPlayer(data.targetUuid)
        if (target == null || !target.isOnline) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_UNKNOWN_PLAYER)
            return
        }

        if (sessions.hasTarget(target.uniqueId)) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_JAR_TARGET_BUSY)
            return
        }

        if (!consumeMainHandJar(player)) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_JAR_HAND_NOT_EMPTY)
            return
        }

        startSession(
            owner = player,
            target = target,
            constFlag = data.constFlag,
            supportBlockLocation = supportBlock.location,
            plannedJarBlockLocation = plannedJarBlock.location,
            visualOrigin = plannedJarBlock.location.toCenterLocation().add(0.0, -0.5, 0.0),
            jailedAnchor = plannedJarBlock.location.toCenterLocation().add(0.0, -0.5, 0.0)
        )

        playJarPlacementEffects(plannedJarBlock.location)
    }

    fun handleSupportBreak(event: BlockBreakEvent) {
        val session = sessions.getBySupportBlock(event.block.location) ?: return
        if (session.constFlag) {
            return
        }
        releaseSession(session.targetUuid, cause = ReleaseCause.SUPPORT_BROKEN)
    }

    fun isJarred(player: Player): Boolean = sessions.hasTarget(player.uniqueId)

    fun blockJarredCommand(player: Player): Boolean = isJarred(player)

    fun enforceJarredFlight(player: Player): Boolean {
        if (!isJarred(player)) return false
        player.allowFlight = true
        player.isFlying = true
        return true
    }

    fun blockJarredGlide(player: Player, tryingGlide: Boolean): Boolean {
        if (!isJarred(player)) return false
        player.isGliding = false
        return tryingGlide
    }

    fun blockJarredMove(player: Player): Boolean {
        if (!isJarred(player)) return false
        return player.uniqueId !in movementBypassTargets
    }

    fun blockJarredInteraction(player: Player, action: Action): Boolean {
        if (!isJarred(player)) return false
        return action == Action.RIGHT_CLICK_AIR ||
            action == Action.RIGHT_CLICK_BLOCK ||
            action == Action.LEFT_CLICK_AIR ||
            action == Action.LEFT_CLICK_BLOCK
    }

    fun blockJarredInteraction(player: Player): Boolean = isJarred(player)

    fun cleanupSessionsForPlayer(playerId: UUID) {
        releaseSession(playerId, cause = ReleaseCause.CLEANUP, waitForScaleRestore = false)
    }

    fun releaseForPlayer(
        player: Player,
        waitForScaleRestore: Boolean = true,
        onReleased: (() -> Unit)? = null
    ) {
        releaseSession(
            targetUuid = player.uniqueId,
            cause = ReleaseCause.CONFLICTING_STATE,
            waitForScaleRestore = waitForScaleRestore,
            onReleased = onReleased
        )
    }

    fun releaseAll() {
        sessions.allSessions().forEach {
            releaseSession(it.targetUuid, waitForScaleRestore = false)
        }
    }

    fun onViewerJoin(viewer: Player) {
        sessions.allSessions().forEach { session ->
            val target = Bukkit.getPlayer(session.targetUuid)
            if (viewer.world != session.visualOrigin.world) return@forEach

            if (target != null && hooker.utils.isHiddenFromPlayer(viewer, target)) {
                if (session.rootAnchorEntity.isValid) {
                    viewer.hideEntity(hooker.plugin, session.rootAnchorEntity)
                }
                session.displayEntities
                    .filter { it.isValid }
                    .forEach { viewer.hideEntity(hooker.plugin, it) }
            } else {
                if (session.rootAnchorEntity.isValid) {
                    viewer.showEntity(hooker.plugin, session.rootAnchorEntity)
                }
                session.displayEntities
                    .filter { it.isValid }
                    .forEach { viewer.showEntity(hooker.plugin, it) }
            }
        }
    }

    private fun startSession(
        owner: Player,
        target: Player,
        constFlag: Boolean,
        supportBlockLocation: Location,
        plannedJarBlockLocation: Location,
        visualOrigin: Location,
        jailedAnchor: Location
    ) {
        clearJarConflictingState(target)
        if (target.gameMode == GameMode.SPECTATOR) {
            target.gameMode = GameMode.CREATIVE
        }
        hooker.playerStateManager.activateState(target, PlayerStateType.JARRED)

        val savedState = capturePlayerState(target)
        val displayGroup = displayFactory.createDisplayParts(target.uniqueId, visualOrigin)

        target.allowFlight = true
        target.isFlying = true
        target.walkSpeed = 0f
        target.flySpeed = 0f
        target.fallDistance = 0f
        applyJarScaleSmooth(target, savedState.scaleBase)
        target.teleport(jailedAnchor)

        var taskId = 0
        taskId = hooker.tickScheduler.runRepeating(0L, 1L) {
            val liveTarget = Bukkit.getPlayer(target.uniqueId)
            if (liveTarget == null || !liveTarget.isOnline || liveTarget.isDead) {
                releaseSession(target.uniqueId)
                return@runRepeating
            }

            val session = sessions.getByTarget(target.uniqueId)
            if (session == null) {
                releaseSession(target.uniqueId)
                return@runRepeating
            }

            liveTarget.allowFlight = true
            liveTarget.isFlying = true
            liveTarget.fallDistance = 0f
            liveTarget.velocity = Vector(0, 0, 0)

            if (liveTarget.location.distanceSquared(session.jailedAnchor) > ANCHOR_EPSILON_SQUARED) {
                liveTarget.teleport(session.jailedAnchor)
            }
        }

        sessions.upsert(
            JarSession(
                targetUuid = target.uniqueId,
                ownerUuid = owner.uniqueId,
                constFlag = constFlag,
                supportBlockLocation = supportBlockLocation,
                plannedJarBlockLocation = plannedJarBlockLocation,
                visualOrigin = visualOrigin,
                jailedAnchor = jailedAnchor,
                rootAnchorEntity = displayGroup.rootAnchor,
                displayEntities = displayGroup.parts,
                savedTargetState = savedState,
                taskId = taskId
            )
        )

        hooker.messageManager.sendChat(owner, MessageKey.INFO_JAR_APPLIED, mapOf("target" to target.name))
    }

    private fun releaseSession(
        targetUuid: UUID,
        cause: ReleaseCause = ReleaseCause.GENERIC,
        waitForScaleRestore: Boolean = true,
        onReleased: (() -> Unit)? = null
    ) {
        if (!releaseInProgress.add(targetUuid)) {
            if (onReleased != null) {
                pendingReleaseCallbacks
                    .computeIfAbsent(targetUuid) { mutableListOf() }
                    .add(onReleased)
            }
            return
        }
        val session = sessions.getByTarget(targetUuid)
        if (session == null) {
            completeRelease(targetUuid, onReleased)
            return
        }

        hooker.tickScheduler.cancel(session.taskId)
        session.displayEntities.forEach { it.remove() }
        session.rootAnchorEntity.remove()
        playJarBreakEffects(session.plannedJarBlockLocation)

        val target = Bukkit.getPlayer(targetUuid)
        if (target == null || !target.isOnline) {
            sessions.removeByTarget(targetUuid)
            completeRelease(targetUuid, onReleased)
            return
        }

        if (!waitForScaleRestore) {
            restorePlayerState(target, session.savedTargetState, restoreScale = true)
            hooker.playerStateManager.deactivateState(target, PlayerStateType.JARRED)
            sessions.removeByTarget(targetUuid)
            if (cause.isLaunchAllowed) {
                disableFlightAndLaunch(target)
            }
            completeRelease(targetUuid, onReleased)
            return
        }

        restorePlayerState(target, session.savedTargetState, restoreScale = false)
        if (cause.isLaunchAllowed) {
            movementBypassTargets.add(targetUuid)
            disableFlightAndLaunch(target)
        }
        applyScaleRestoreSmooth(target, session.savedTargetState.scaleBase) {
            hooker.playerStateManager.deactivateState(target, PlayerStateType.JARRED)
            sessions.removeByTarget(targetUuid)
            completeRelease(targetUuid, onReleased)
        }
    }

    private fun completeRelease(targetUuid: UUID, onReleased: (() -> Unit)?) {
        releaseInProgress.remove(targetUuid)
        movementBypassTargets.remove(targetUuid)
        onReleased?.invoke()
        val callbacks = pendingReleaseCallbacks.remove(targetUuid).orEmpty()
        callbacks.forEach { it.invoke() }
    }

    private fun clearJarConflictingState(target: Player) {
        hooker.utils.unsetAllPoses(target, true)
        hooker.utils.unsetAllStates(target)
        hooker.utils.checkDisguiseDisable(target)
        hooker.utils.checkCustomEffectsDisable(target)
        hooker.utils.checkSlapUnslap(target)
    }

    private fun capturePlayerState(player: Player): JarPlayerState {
        val scaleBase = player.getAttribute(Attribute.SCALE)?.baseValue ?: 1.0
        return JarPlayerState(
            allowFlight = player.allowFlight,
            isFlying = player.isFlying,
            walkSpeed = player.walkSpeed,
            flySpeed = player.flySpeed,
            scaleBase = scaleBase
        )
    }

    private fun restorePlayerState(
        player: Player,
        state: JarPlayerState,
        restoreScale: Boolean = true
    ) {
        player.allowFlight = state.allowFlight
        player.isFlying = state.isFlying
        player.walkSpeed = state.walkSpeed
        player.flySpeed = state.flySpeed
        player.fallDistance = 0f
        if (restoreScale) {
            player.getAttribute(Attribute.SCALE)?.baseValue = state.scaleBase
        }
    }

    private fun applyJarScaleSmooth(player: Player, sourceScale: Double) {
        val targetScale = sourceScale * SCALE_MULTIPLIER
        val resizeManager = hooker.resizeManagerOrNull()
        if (resizeManager == null) {
            player.getAttribute(Attribute.SCALE)?.baseValue = targetScale
            return
        }
        resizeManager.smoothTransitionScale(player, targetScale)
    }

    private fun applyScaleRestoreSmooth(player: Player, targetScale: Double, onComplete: () -> Unit) {
        val resizeManager = hooker.resizeManagerOrNull()
        if (resizeManager == null) {
            player.getAttribute(Attribute.SCALE)?.baseValue = targetScale
            onComplete()
            return
        }
        resizeManager.smoothTransitionScale(player, targetScale, onComplete)
    }

    private fun createJarItem(targetUuid: UUID, targetName: String, constFlag: Boolean): ItemStack {
        val item = ItemStack(Material.DECORATED_POT)
        item.editMeta { meta ->
            meta.displayName(miniMessage.deserialize("<!i><shadow:#000000:1><gradient:#FF02CD:#FFF000>Банка с</gradient> <#00FF40>$targetName</shadow>"))
        }
        item.editPersistentDataContainer { pdc ->
            pdc.set(keyMarker, PersistentDataType.INTEGER, 1)
            pdc.set(keyTargetUuid, PersistentDataType.STRING, targetUuid.toString())
            pdc.set(keyTargetName, PersistentDataType.STRING, targetName)
            pdc.set(keyConst, PersistentDataType.STRING, constFlag.toString())
        }
        return item
    }

    private fun readJarData(item: ItemStack?): JarItemData? {
        if (item == null || item.type != Material.DECORATED_POT) return null
        val pdc = item.persistentDataContainer
        if (pdc.get(keyMarker, PersistentDataType.INTEGER) != 1) return null

        val target = pdc.get(keyTargetUuid, PersistentDataType.STRING)?.let {
            runCatching { UUID.fromString(it) }.getOrNull()
        } ?: return null
        val targetName = pdc.get(keyTargetName, PersistentDataType.STRING) ?: return null
        val constFlag = pdc.get(keyConst, PersistentDataType.STRING)?.toBoolean() ?: false

        return JarItemData(target, targetName, constFlag)
    }

    private fun consumeMainHandJar(player: Player): Boolean {
        val hand = player.inventory.itemInMainHand
        if (readJarData(hand) == null) return false

        if (hand.amount > 1) {
            hand.amount = hand.amount - 1
            player.inventory.setItemInMainHand(hand)
        } else {
            player.inventory.setItemInMainHand(ItemStack(Material.AIR))
        }
        return true
    }

    private data class JarItemData(
        val targetUuid: UUID,
        val targetName: String,
        val constFlag: Boolean
    )

    private enum class ReleaseCause(val isLaunchAllowed: Boolean) {
        GENERIC(true),
        SUPPORT_BROKEN(true),
        CONFLICTING_STATE(false),
        CLEANUP(false)
    }

    private fun hasJarPermission(player: Player): Boolean {
        val permissionNode = hooker.permissionManager.getPermissionNodeForCommand("jar")
        return player.hasPermission(permissionNode)
    }

    private fun disableFlightAndLaunch(player: Player) {
        player.allowFlight = false
        player.isFlying = false
        if (!hasEnoughHeadroom(player.location, 5)) return

        val velocity = player.velocity
        player.velocity = Vector(velocity.x, LAUNCH_UP_VELOCITY, velocity.z)
    }

    private fun hasEnoughHeadroom(location: Location, requiredAirBlocks: Int): Boolean {
        val world = location.world ?: return false
        val baseX = location.blockX
        val baseY = location.blockY
        val baseZ = location.blockZ

        for (offset in 1..requiredAirBlocks) {
            if (!world.getBlockAt(baseX, baseY + offset, baseZ).isPassable) {
                return false
            }
        }
        return true
    }

    private fun playJarPlacementEffects(plannedJarBlockLocation: Location) {
        val world = plannedJarBlockLocation.world ?: return
        val effectLocation = plannedJarBlockLocation.toCenterLocation()
        world.playSound(effectLocation, Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 1.0f, 1.0f)
        world.spawnParticle(
            Particle.ITEM,
            effectLocation,
            35,
            0.3,
            0.5,
            0.3,
            0.0,
            ItemStack(Material.SNOWBALL)
        )
    }

    private fun playJarBreakEffects(plannedJarBlockLocation: Location) {
        val world = plannedJarBlockLocation.world ?: return
        val effectLocation = plannedJarBlockLocation.toCenterLocation()
        world.playSound(effectLocation, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.0f, 1.0f)
        world.spawnParticle(
            Particle.ITEM,
            effectLocation,
            35,
            0.3,
            0.5,
            0.3,
            0.0,
            ItemStack(Material.SNOWBALL)
        )
    }

    companion object {
        private const val SCALE_MULTIPLIER = 0.45
        private const val ANCHOR_EPSILON_SQUARED = 0.0004
        private const val LAUNCH_UP_VELOCITY = 0.75
    }
}

private fun org.bukkit.inventory.PlayerInventory.firstEmptyHotbarSlot(): Int? {
    for (slot in 0..8) {
        if (getItem(slot).isNullOrAir()) return slot
    }
    return null
}

private fun ItemStack?.isNullOrAir(): Boolean = this == null || this.type == Material.AIR
