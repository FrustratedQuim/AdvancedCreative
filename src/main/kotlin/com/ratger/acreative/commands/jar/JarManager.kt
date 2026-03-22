package com.ratger.acreative.commands.jar

import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.MessageKey
import com.ratger.acreative.utils.PlayerStateManager.PlayerStateType
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
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
    private val keyOwnerUuid = NamespacedKey(hooker.plugin, "jar_owner_uuid")
    private val keyTargetUuid = NamespacedKey(hooker.plugin, "jar_target_uuid")
    private val keyTargetName = NamespacedKey(hooker.plugin, "jar_target_name")
    private val keyConst = NamespacedKey(hooker.plugin, "jar_const")

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
                releaseSession(target.uniqueId, notifyOwner = true)
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
        owner.inventory.setItem(freeHotbarSlot, createJarItem(owner.uniqueId, target.uniqueId, target.name, constFlag))
        owner.inventory.heldItemSlot = freeHotbarSlot
        hooker.messageManager.sendChat(owner, MessageKey.INFO_JAR_ITEM_GIVEN, mapOf("target" to target.name))
    }

    fun handleJarBlockPlace(event: BlockPlaceEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        val player = event.player
        val item = event.itemInHand
        val data = readJarData(item) ?: return

        event.isCancelled = true

        if (player.uniqueId != data.ownerUuid) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_JAR_OWNER_MISMATCH)
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
    }

    fun handleSupportBreak(event: BlockBreakEvent) {
        val session = sessions.getBySupportBlock(event.block.location) ?: return
        if (session.constFlag) {
            return
        }
        releaseSession(session.targetUuid)
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

    fun blockJarredMove(player: Player): Boolean = isJarred(player)

    fun blockJarredInteraction(player: Player, action: Action): Boolean {
        if (!isJarred(player)) return false
        return action == Action.RIGHT_CLICK_AIR ||
            action == Action.RIGHT_CLICK_BLOCK ||
            action == Action.LEFT_CLICK_AIR ||
            action == Action.LEFT_CLICK_BLOCK
    }

    fun blockJarredInteraction(player: Player): Boolean = isJarred(player)

    fun cleanupSessionsForPlayer(playerId: UUID) {
        releaseSession(playerId)
    }

    fun releaseForPlayer(player: Player) {
        releaseSession(player.uniqueId)
    }

    fun releaseAll() {
        sessions.allSessions().forEach { releaseSession(it.targetUuid) }
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

        val targetScale = target.getAttribute(Attribute.GENERIC_SCALE)
        target.allowFlight = true
        target.isFlying = true
        target.walkSpeed = 0f
        target.flySpeed = 0f
        target.fallDistance = 0f
        targetScale?.baseValue = savedState.scaleBase * SCALE_MULTIPLIER
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
        hooker.messageManager.sendChat(target, MessageKey.INFO_JAR_TARGET_APPLIED)
    }

    private fun releaseSession(targetUuid: UUID, notifyOwner: Boolean = false) {
        val session = sessions.removeByTarget(targetUuid) ?: return
        hooker.tickScheduler.cancel(session.taskId)
        session.displayEntities.forEach { it.remove() }
        session.rootAnchorEntity.remove()

        val target = Bukkit.getPlayer(targetUuid)
        if (target != null) {
            restorePlayerState(target, session.savedTargetState)
            hooker.playerStateManager.deactivateState(target, PlayerStateType.JARRED)
            hooker.messageManager.sendChat(target, MessageKey.INFO_JAR_TARGET_RELEASED)
        }

        Bukkit.getPlayer(session.ownerUuid)?.takeIf { notifyOwner }?.let { owner ->
            val targetName = target?.name ?: session.targetUuid.toString()
            hooker.messageManager.sendChat(owner, MessageKey.INFO_JAR_RELEASED, mapOf("target" to targetName))
        }
    }

    private fun clearJarConflictingState(target: Player) {
        hooker.utils.unsetAllPoses(target, true)
        hooker.utils.unsetAllStates(target)
        hooker.utils.checkDisguiseDisable(target)
        hooker.utils.checkCustomEffectsDisable(target)
        hooker.utils.checkSlapUnslap(target)
    }

    private fun capturePlayerState(player: Player): JarPlayerState {
        val scaleBase = player.getAttribute(Attribute.GENERIC_SCALE)?.baseValue ?: 1.0
        return JarPlayerState(
            allowFlight = player.allowFlight,
            isFlying = player.isFlying,
            walkSpeed = player.walkSpeed,
            flySpeed = player.flySpeed,
            scaleBase = scaleBase
        )
    }

    private fun restorePlayerState(player: Player, state: JarPlayerState) {
        player.allowFlight = state.allowFlight
        player.isFlying = state.isFlying
        player.walkSpeed = state.walkSpeed
        player.flySpeed = state.flySpeed
        player.fallDistance = 0f
        player.getAttribute(Attribute.GENERIC_SCALE)?.baseValue = state.scaleBase
    }

    private fun createJarItem(ownerUuid: UUID, targetUuid: UUID, targetName: String, constFlag: Boolean): ItemStack {
        val item = ItemStack(Material.DECORATED_POT)
        val meta = item.itemMeta
        meta.displayName(miniMessage.deserialize("<!i><gradient:#FF02CD:#FFF000>Банка с</gradient> <#00FF40>$targetName"))

        val pdc = meta.persistentDataContainer
        pdc.set(keyMarker, PersistentDataType.INTEGER, 1)
        pdc.set(keyOwnerUuid, PersistentDataType.STRING, ownerUuid.toString())
        pdc.set(keyTargetUuid, PersistentDataType.STRING, targetUuid.toString())
        pdc.set(keyTargetName, PersistentDataType.STRING, targetName)
        pdc.set(keyConst, PersistentDataType.STRING, constFlag.toString())

        item.itemMeta = meta
        return item
    }

    private fun readJarData(item: ItemStack?): JarItemData? {
        if (item == null || item.type != Material.DECORATED_POT) return null
        val meta = item.itemMeta ?: return null
        val pdc = meta.persistentDataContainer
        if (pdc.get(keyMarker, PersistentDataType.INTEGER) != 1) return null

        val owner = pdc.get(keyOwnerUuid, PersistentDataType.STRING)?.let {
            runCatching { UUID.fromString(it) }.getOrNull()
        } ?: return null
        val target = pdc.get(keyTargetUuid, PersistentDataType.STRING)?.let {
            runCatching { UUID.fromString(it) }.getOrNull()
        } ?: return null
        val targetName = pdc.get(keyTargetName, PersistentDataType.STRING) ?: return null
        val constFlag = pdc.get(keyConst, PersistentDataType.STRING)?.toBoolean() ?: false

        return JarItemData(owner, target, targetName, constFlag)
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
        val ownerUuid: UUID,
        val targetUuid: UUID,
        val targetName: String,
        val constFlag: Boolean
    )

    companion object {
        private const val SCALE_MULTIPLIER = 0.45
        private const val ANCHOR_EPSILON_SQUARED = 0.0004
    }
}

private fun org.bukkit.inventory.PlayerInventory.firstEmptyHotbarSlot(): Int? {
    for (slot in 0..8) {
        if (getItem(slot).isNullOrAir()) return slot
    }
    return null
}

private fun ItemStack?.isNullOrAir(): Boolean = this == null || this.type == Material.AIR
