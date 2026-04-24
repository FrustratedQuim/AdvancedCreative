package com.ratger.acreative.commands.disguise

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook
import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.MessageKey
import com.ratger.acreative.utils.PlayerStateManager.PlayerStateType
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import me.tofaa.entitylib.wrapper.WrapperEntity
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import com.github.retrooper.packetevents.protocol.player.Equipment as PacketEquipment
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot as PacketEquipmentSlot

data class DisguiseData(
    val entity: WrapperEntity,
    val type: EntityType,
    val showSelf: Boolean,
    val showNick: Boolean,
    val equipment: List<PacketEquipment> = emptyList()
)

class DisguiseManager(private val hooker: FunctionHooker) {
    data class CacheSnapshot(
        val disguisedPlayers: Int,
        val viewerRelations: Int,
        val queuedViewerRelations: Int,
        val pendingViewers: Int,
        val rememberedNames: Int,
        val rememberedGlowStates: Int
    )

    val disguisedPlayers = ConcurrentHashMap<Player, DisguiseData>()
    private val tasks = ConcurrentHashMap<Player, Int>()
    private val activeViewers = ConcurrentHashMap<Player, MutableSet<UUID>>()
    private val queuedInitViewers = ConcurrentHashMap<Player, MutableSet<UUID>>()
    private val viewerPendingUntilTick = ConcurrentHashMap<UUID, Long>()
    private val lastCustomName = ConcurrentHashMap<Player, Component>()
    private val lastGlowingState = ConcurrentHashMap<Player, Boolean>()

    private val pendingInitDelayTicks = 10L

    val restrictedEntities = setOf(
        EntityType.WITHER,
        EntityType.ENDER_DRAGON,
        EntityType.GIANT,
        EntityType.WARDEN
    )
    private val noSwingAnimationEntities = setOf(
        EntityType.AREA_EFFECT_CLOUD,
        EntityType.OAK_BOAT,
        EntityType.OAK_CHEST_BOAT,
        EntityType.MINECART,
        EntityType.SPAWNER_MINECART,
        EntityType.HOPPER_MINECART,
        EntityType.FURNACE_MINECART,
        EntityType.COMMAND_BLOCK_MINECART,
        EntityType.TNT_MINECART,
        EntityType.END_CRYSTAL,
        EntityType.ITEM_FRAME,
        EntityType.GLOW_ITEM_FRAME,
        EntityType.WIND_CHARGE,
        EntityType.WITHER_SKULL,
        EntityType.DRAGON_FIREBALL,
        EntityType.FIREBALL,
        EntityType.SMALL_FIREBALL,
        EntityType.EYE_OF_ENDER,
        EntityType.LEASH_KNOT,
        EntityType.OMINOUS_ITEM_SPAWNER,
        EntityType.SHULKER_BULLET
    )

    private fun getDisplayName(player: Player): Component {
        val team = player.scoreboard.getEntryTeam(player.name)
        return team?.let {
            Component.join(JoinConfiguration.noSeparators(), it.prefix(), Component.text(player.name), it.suffix())
        } ?: Component.text(player.name)
    }

    private fun getNearbyPlayers(player: Player, location: Location, showSelf: Boolean): List<Player> {
        return location.getNearbyPlayers(100.0)
            .filter { !hooker.utils.isHiddenFromPlayer(it, player) && (showSelf || it != player) }
    }

    private fun getEntityType(type: String): EntityType? {
        return try {
            EntityType.valueOf(type.uppercase())
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun getCurrentTick(): Long = Bukkit.getCurrentTick().toLong()

    private fun isViewerPending(viewerId: UUID): Boolean {
        pruneExpiredViewerPending()
        val untilTick = viewerPendingUntilTick[viewerId] ?: return false
        return untilTick > getCurrentTick()
    }

    private fun markViewerPending(viewerId: UUID, ticks: Long = pendingInitDelayTicks) {
        viewerPendingUntilTick[viewerId] = getCurrentTick() + ticks
    }

    fun onViewerJoin(viewer: Player) {
        cleanupViewerFromAllDisguises(viewer.uniqueId)
        markViewerPending(viewer.uniqueId)
    }

    fun onViewerDisconnect(viewerId: UUID) {
        viewerPendingUntilTick.remove(viewerId)
        cleanupViewerFromAllDisguises(viewerId)
    }

    fun onViewerWorldOrRespawn(viewer: Player) {
        cleanupViewerFromAllDisguises(viewer.uniqueId)
        markViewerPending(viewer.uniqueId)
    }

    private fun shouldViewerSeeDisguise(owner: Player, viewer: Player, showSelf: Boolean): Boolean {
        if (!viewer.isOnline || !owner.isOnline) return false
        if (owner.world != viewer.world) return false
        if (!showSelf && viewer.uniqueId == owner.uniqueId) return false
        if (hooker.utils.isHiddenFromPlayer(viewer, owner)) return false
        if (owner.location.distanceSquared(viewer.location) > 100.0 * 100.0) return false
        return true
    }

    private fun cleanupViewerForDisguise(owner: Player, data: DisguiseData, viewerId: UUID) {
        activeViewers[owner]?.remove(viewerId)
        queuedInitViewers[owner]?.remove(viewerId)
        data.entity.removeViewer(viewerId)
    }

    private fun cleanupViewerFromAllDisguises(viewerId: UUID) {
        disguisedPlayers.forEach { (owner, data) ->
            cleanupViewerForDisguise(owner, data, viewerId)
        }
    }

    private fun queueViewerInit(owner: Player, viewerId: UUID) {
        queuedInitViewers.computeIfAbsent(owner) { mutableSetOf() }.add(viewerId)
    }

    private fun initializeViewer(owner: Player, data: DisguiseData, viewer: Player): Boolean {
        if (!shouldViewerSeeDisguise(owner, viewer, data.showSelf)) return false
        if (isViewerPending(viewer.uniqueId)) return false

        data.entity.addViewer(viewer.uniqueId)

        if (data.showNick) {
            data.entity.entityMeta.customName = getDisplayName(owner)
            data.entity.entityMeta.isCustomNameVisible = true
        } else {
            data.entity.entityMeta.customName = null
            data.entity.entityMeta.isCustomNameVisible = false
        }
        data.entity.entityMeta.isGlowing = hooker.utils.isGlowing(owner)

        if (data.equipment.isNotEmpty()) {
            val equipPacket = WrapperPlayServerEntityEquipment(data.entity.entityId, data.equipment)
            PacketEvents.getAPI().playerManager.sendPacket(viewer, equipPacket)
        }

        val headLookPacket = WrapperPlayServerEntityHeadLook(data.entity.entityId, owner.location.yaw)
        PacketEvents.getAPI().playerManager.sendPacket(viewer, headLookPacket)

        activeViewers.computeIfAbsent(owner) { mutableSetOf() }.add(viewer.uniqueId)
        queuedInitViewers[owner]?.remove(viewer.uniqueId)
        return true
    }

    fun disguisePlayer(player: Player, type: String?, flags: List<String>) {
        if (type == null) {
            if (disguisedPlayers.containsKey(player)) {
                undisguisePlayer(player)
                return
            }
            hooker.messageManager.sendChat(player, MessageKey.USAGE_DISGUISE)
            return
        }

        if (type == "off" || type.equals("player", ignoreCase = true)) {
            undisguisePlayer(player)
            return
        }

        val entityType = getEntityType(type)
        if (entityType == null) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_DISGUISE_TYPE)
            return
        }

        val parsedFlags = DisguiseFlags.parse(flags)
        if (parsedFlags.requiresNickPermission && !player.hasPermission("advancedcreative.disguise.nick")) {
            hooker.permissionManager.sendPermissionDenied(player, "disguise.nick")
            return
        }

        val newShowSelf = parsedFlags.showSelf
        val newShowNick = parsedFlags.showNick
        if (disguisedPlayers.containsKey(player)) {
            val currentDisguise = disguisedPlayers[player]!!
            if (currentDisguise.type == entityType && currentDisguise.showSelf == newShowSelf && currentDisguise.showNick == newShowNick) {
                undisguisePlayer(player)
                return
            }
            undisguisePlayer(player, true)
        }

        if (!player.hasPermission("advancedcreative.disguise.full") && entityType in restrictedEntities) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_DISGUISE_TYPE)
            return
        }

        if (hooker.configManager.getBlockedDisguises().contains(entityType)) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_DISGUISE_BLOCKED)
            return
        }

        hooker.playerStateManager.activateState(player, PlayerStateType.DISGUISED)
        hooker.utils.unsetAllStates(player)
        hooker.playerStateManager.savePlayerInventory(player)

        val equipmentList = mutableListOf<PacketEquipment>()
        val inventory = player.inventory
        val mainHand = inventory.itemInMainHand
        if (mainHand.type != Material.AIR) {
            val packetItem = SpigotConversionUtil.fromBukkitItemStack(mainHand)
            equipmentList.add(PacketEquipment(PacketEquipmentSlot.MAIN_HAND, packetItem))
        }
        val offHand = inventory.itemInOffHand
        if (offHand.type != Material.AIR) {
            val packetItem = SpigotConversionUtil.fromBukkitItemStack(offHand)
            equipmentList.add(PacketEquipment(PacketEquipmentSlot.OFF_HAND, packetItem))
        }
        inventory.armorContents.forEachIndexed { index, item ->
            if (item != null && item.type != Material.AIR) {
                val slot = when (index) {
                    0 -> PacketEquipmentSlot.BOOTS
                    1 -> PacketEquipmentSlot.LEGGINGS
                    2 -> PacketEquipmentSlot.CHEST_PLATE
                    3 -> PacketEquipmentSlot.HELMET
                    else -> return@forEachIndexed
                }
                val packetItem = SpigotConversionUtil.fromBukkitItemStack(item)
                equipmentList.add(PacketEquipment(slot, packetItem))
            }
        }
        inventory.setItemInMainHand(null)
        inventory.setItemInOffHand(null)
        inventory.armorContents = arrayOfNulls(4)

        val entity = WrapperEntity(entityType.toPacketEventsType())
        val meta = entity.entityMeta
        if (newShowNick) {
            meta.customName = getDisplayName(player)
            meta.isCustomNameVisible = true
        } else {
            meta.customName = null
            meta.isCustomNameVisible = false
        }
        meta.isGlowing = hooker.utils.isGlowing(player)

        val data = DisguiseData(entity, entityType, newShowSelf, newShowNick, equipmentList)

        val playerLoc = player.location
        val loc = com.github.retrooper.packetevents.protocol.world.Location(
            playerLoc.x, playerLoc.y, playerLoc.z, playerLoc.yaw, playerLoc.pitch
        )

        entity.spawn(loc)

        player.isInvisible = true
        player.isGlowing = false
        disguisedPlayers[player] = data
        activeViewers[player] = mutableSetOf()
        queuedInitViewers[player] = mutableSetOf()
        getNearbyPlayers(player, playerLoc, data.showSelf).forEach { queueViewerInit(player, it.uniqueId) }

        scheduleUpdateTask(player)
        hooker.messageManager.sendChat(player, MessageKey.SUCCESS_DISGUISE)
    }

    fun cacheSnapshot(): CacheSnapshot = CacheSnapshot(
        disguisedPlayers = disguisedPlayers.size,
        viewerRelations = activeViewers.values.sumOf { it.size },
        queuedViewerRelations = queuedInitViewers.values.sumOf { it.size },
        pendingViewers = activePendingViewerCount(),
        rememberedNames = lastCustomName.size,
        rememberedGlowStates = lastGlowingState.size
    )

    private fun activePendingViewerCount(): Int {
        pruneExpiredViewerPending()
        return viewerPendingUntilTick.size
    }

    private fun pruneExpiredViewerPending(currentTick: Long = getCurrentTick()) {
        viewerPendingUntilTick.entries.removeIf { (_, untilTick) -> untilTick <= currentTick }
    }

    fun undisguisePlayer(player: Player, silent: Boolean = false) {
        disguisedPlayers[player]?.let { data ->
            data.entity.remove()
            player.isInvisible = false
            if (hooker.utils.isGlowing(player)) player.isGlowing = true
            hooker.playerStateManager.restorePlayerInventory(player)
            hooker.playerStateManager.deactivateState(player, PlayerStateType.DISGUISED)
            disguisedPlayers.remove(player)
            tasks.remove(player)?.let { hooker.tickScheduler.cancel(it) }
            activeViewers.remove(player)
            queuedInitViewers.remove(player)
            lastCustomName.remove(player)
            lastGlowingState.remove(player)
            if (!silent) {
                hooker.messageManager.sendChat(player, MessageKey.SUCCESS_DISGUISE_REMOVED)
            }
        }
    }

    fun updateDisguiseForPlayer(disguisedPlayer: Player, viewer: Player) {
        val data = disguisedPlayers[disguisedPlayer] ?: return
        if (!shouldViewerSeeDisguise(disguisedPlayer, viewer, data.showSelf)) {
            cleanupViewerForDisguise(disguisedPlayer, data, viewer.uniqueId)
            return
        }
        queueViewerInit(disguisedPlayer, viewer.uniqueId)
    }

    fun updateEntityGlowing(player: Player, isGlowing: Boolean) {
        val data = disguisedPlayers[player] ?: return
        data.entity.entityMeta.isGlowing = isGlowing
        lastGlowingState[player] = isGlowing
    }

    fun sendSwingAnimation(player: Player) {
        val data = disguisedPlayers[player] ?: return
        if (data.type in noSwingAnimationEntities) return
        val packet = WrapperPlayServerEntityAnimation(
            data.entity.entityId,
            WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM
        )
        val viewerIds = activeViewers[player] ?: emptySet()
        viewerIds.forEach { viewerId ->
            Bukkit.getPlayer(viewerId)?.let { viewer ->
                PacketEvents.getAPI().playerManager.sendPacket(viewer, packet)
            }
        }
    }

    fun recreateDisguise(player: Player, to: Location?) {
        val data = disguisedPlayers[player] ?: return
        val targetLoc = to ?: player.location

        val desiredViewers = getNearbyPlayers(player, targetLoc, data.showSelf)
        val desiredIds = desiredViewers.map { it.uniqueId }.toSet()
        val active = activeViewers.computeIfAbsent(player) { mutableSetOf() }
        val queued = queuedInitViewers.computeIfAbsent(player) { mutableSetOf() }

        val stale = (active + queued).filter { it !in desiredIds }
        stale.forEach { cleanupViewerForDisguise(player, data, it) }

        desiredViewers.forEach { viewer ->
            if (viewer.uniqueId !in active && viewer.uniqueId !in queued) {
                queueViewerInit(player, viewer.uniqueId)
            }
        }

        queued.toSet().forEach { viewerId ->
            val viewer = Bukkit.getPlayer(viewerId) ?: return@forEach
            initializeViewer(player, data, viewer)
        }

        val loc = com.github.retrooper.packetevents.protocol.world.Location(
            targetLoc.x, targetLoc.y, targetLoc.z, targetLoc.yaw, targetLoc.pitch
        )
        if (active.isNotEmpty()) {
            data.entity.teleport(loc)
            val headLookPacket = WrapperPlayServerEntityHeadLook(data.entity.entityId, targetLoc.yaw)
            active.forEach { viewerId ->
                val viewer = Bukkit.getPlayer(viewerId) ?: return@forEach
                PacketEvents.getAPI().playerManager.sendPacket(viewer, headLookPacket)
                if (data.equipment.isNotEmpty()) {
                    val equipPacket = WrapperPlayServerEntityEquipment(data.entity.entityId, data.equipment)
                    PacketEvents.getAPI().playerManager.sendPacket(viewer, equipPacket)
                }
            }
        }
    }

    fun updateMainHandEquipment(player: Player) {
        val data = disguisedPlayers[player] ?: return
        val bukkitItem: ItemStack = hooker.playerStateManager.getCurrentSavedMainHandItem(player) ?: ItemStack(Material.AIR)
        val packetItem = SpigotConversionUtil.fromBukkitItemStack(bukkitItem)

        val newEquipment = data.equipment.filter { it.slot != PacketEquipmentSlot.MAIN_HAND }.toMutableList()
        newEquipment.add(PacketEquipment(PacketEquipmentSlot.MAIN_HAND, packetItem))

        disguisedPlayers[player] = DisguiseData(data.entity, data.type, data.showSelf, data.showNick, newEquipment)

        val equipPacket = WrapperPlayServerEntityEquipment(data.entity.entityId, newEquipment)
        val viewerIds = activeViewers[player] ?: emptySet()
        viewerIds.forEach { uid ->
            Bukkit.getPlayer(uid)?.let { v ->
                PacketEvents.getAPI().playerManager.sendPacket(v, equipPacket)
            }
        }
    }

    private fun scheduleUpdateTask(player: Player) {
        val taskId = hooker.tickScheduler.runRepeating(0L, 2L) {
            if (!player.isOnline || !disguisedPlayers.containsKey(player)) {
                tasks.remove(player)?.let { hooker.tickScheduler.cancel(it) }
                return@runRepeating
            }
            val currentData = disguisedPlayers[player] ?: return@runRepeating
            val playerLoc = player.location

            if (currentData.showNick) {
                val newName = getDisplayName(player)
                if (lastCustomName[player] != newName) {
                    currentData.entity.entityMeta.customName = newName
                    currentData.entity.entityMeta.isCustomNameVisible = true
                    lastCustomName[player] = newName
                }
            } else if (lastCustomName.remove(player) != null || currentData.entity.entityMeta.isCustomNameVisible) {
                currentData.entity.entityMeta.customName = null
                currentData.entity.entityMeta.isCustomNameVisible = false
            }
            val newGlow = hooker.utils.isGlowing(player)
            if (lastGlowingState[player] != newGlow) {
                currentData.entity.entityMeta.isGlowing = newGlow
                lastGlowingState[player] = newGlow
            }

            val desiredViewers = getNearbyPlayers(player, playerLoc, currentData.showSelf)
            val desiredIds = desiredViewers.map { it.uniqueId }.toSet()
            val active = activeViewers.computeIfAbsent(player) { mutableSetOf() }
            val queued = queuedInitViewers.computeIfAbsent(player) { mutableSetOf() }

            val toRemove = (active + queued).filter { it !in desiredIds }
            toRemove.forEach { cleanupViewerForDisguise(player, currentData, it) }

            desiredViewers.forEach { viewer ->
                if (viewer.uniqueId !in active && viewer.uniqueId !in queued) {
                    queueViewerInit(player, viewer.uniqueId)
                }
            }

            queued.toSet().forEach { viewerId ->
                val viewer = Bukkit.getPlayer(viewerId) ?: run {
                    cleanupViewerForDisguise(player, currentData, viewerId)
                    return@forEach
                }
                if (!shouldViewerSeeDisguise(player, viewer, currentData.showSelf)) {
                    cleanupViewerForDisguise(player, currentData, viewerId)
                    return@forEach
                }
                initializeViewer(player, currentData, viewer)
            }

            val loc = com.github.retrooper.packetevents.protocol.world.Location(
                playerLoc.x, playerLoc.y, playerLoc.z, playerLoc.yaw, playerLoc.pitch
            )
            if (active.isNotEmpty()) {
                currentData.entity.teleport(loc)
                val headLookPacket = WrapperPlayServerEntityHeadLook(currentData.entity.entityId, playerLoc.yaw)
                active.forEach { viewerId ->
                    val viewer = Bukkit.getPlayer(viewerId) ?: return@forEach
                    PacketEvents.getAPI().playerManager.sendPacket(viewer, headLookPacket)
                }
            }
        }
        tasks[player] = taskId
    }

    private data class DisguiseFlags(
        val showSelf: Boolean,
        val showNick: Boolean,
        val requiresNickPermission: Boolean
    ) {
        companion object {
            fun parse(flags: List<String>): DisguiseFlags {
                var showSelf: Boolean? = null
                var showNick: Boolean? = null
                var requiresNickPermission = false

                flags.forEach { rawFlag ->
                    when (rawFlag.lowercase()) {
                        "-self" -> if (showSelf == null) showSelf = true
                        "-noself" -> if (showSelf == null) showSelf = false
                        "-withnick" -> {
                            if (showNick == null) showNick = true
                            requiresNickPermission = true
                        }
                        "-nonick" -> {
                            if (showNick == null) showNick = false
                            requiresNickPermission = true
                        }
                    }
                }

                return DisguiseFlags(
                    showSelf = showSelf ?: true,
                    showNick = showNick ?: true,
                    requiresNickPermission = requiresNickPermission
                )
            }
        }
    }

    private fun EntityType.toPacketEventsType(): com.github.retrooper.packetevents.protocol.entity.type.EntityType {
        return EntityTypes.getByName("minecraft:${this.name.lowercase()}")
    }
}
