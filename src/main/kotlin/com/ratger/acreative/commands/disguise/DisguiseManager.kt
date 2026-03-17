package com.ratger.acreative.commands.disguise

import com.ratger.acreative.core.MessageKey
import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook
import com.ratger.acreative.core.FunctionHooker
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import me.tofaa.entitylib.wrapper.WrapperEntity
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import com.ratger.acreative.utils.PlayerStateManager.PlayerStateType
import org.bukkit.inventory.ItemStack
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

    val disguisedPlayers = ConcurrentHashMap<Player, DisguiseData>()
    private val tasks = ConcurrentHashMap<Player, Int>()
    private val lastViewers = ConcurrentHashMap<Player, MutableSet<java.util.UUID>>()
    private val lastCustomName = ConcurrentHashMap<Player, Component>()
    private val lastGlowingState = ConcurrentHashMap<Player, Boolean>()

    val restrictedEntities = setOf(
        EntityType.WITHER,
        EntityType.ENDER_DRAGON,
        EntityType.GIANT,
        EntityType.WARDEN
    )
    private val noSwingAnimationEntities = setOf(
        EntityType.AREA_EFFECT_CLOUD,
        EntityType.BOAT,
        EntityType.CHEST_BOAT,
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
        if (hooker.utils.isGlowing(player)) {
            meta.isGlowing = true
        }

        val showSelf = newShowSelf
        val data = DisguiseData(entity, entityType, showSelf, newShowNick, equipmentList)

        val playerLoc = player.location
        val loc = com.github.retrooper.packetevents.protocol.world.Location(
            playerLoc.x, playerLoc.y, playerLoc.z, playerLoc.yaw, playerLoc.pitch
        )

        val viewers = getNearbyPlayers(player, playerLoc, showSelf)
        viewers.forEach { entity.addViewer(it.uniqueId) }
        entity.spawn(loc)
        if (equipmentList.isNotEmpty()) {
            val equipPacket = WrapperPlayServerEntityEquipment(entity.entityId, equipmentList)
            viewers.forEach { viewer ->
                PacketEvents.getAPI().playerManager.sendPacket(viewer, equipPacket)
            }
        }

        player.isInvisible = true
        disguisedPlayers[player] = data
        player.isGlowing = false
        scheduleUpdateTask(player)
        hooker.messageManager.sendChat(player, MessageKey.SUCCESS_DISGUISE)
    }

    fun undisguisePlayer(player: Player, silent: Boolean = false) {
        disguisedPlayers[player]?.let { data ->
            data.entity.remove()
            player.isInvisible = false
            if (hooker.utils.isGlowing(player)) player.isGlowing = true
            hooker.playerStateManager.restorePlayerInventory(player)
            hooker.playerStateManager.deactivateState(player, PlayerStateType.DISGUISED)
            disguisedPlayers.remove(player)
            tasks.remove(player)?.let { Bukkit.getScheduler().cancelTask(it) }
            lastViewers.remove(player)
            lastCustomName.remove(player)
            lastGlowingState.remove(player)
            if (!silent) {
                hooker.messageManager.sendChat(player, MessageKey.SUCCESS_DISGUISE_REMOVED)
            }
        }
    }

    fun updateDisguiseForPlayer(disguisedPlayer: Player, viewer: Player) {
        disguisedPlayers[disguisedPlayer]?.let { data ->
            if (!data.showSelf && viewer == disguisedPlayer) return
            val playerLoc = disguisedPlayer.location
            val loc = com.github.retrooper.packetevents.protocol.world.Location(
                playerLoc.x, playerLoc.y, playerLoc.z, playerLoc.yaw, playerLoc.pitch
            )
            data.entity.addViewer(viewer.uniqueId)
            data.entity.teleport(loc)
            if (data.showNick) {
                data.entity.entityMeta.customName = getDisplayName(disguisedPlayer)
                data.entity.entityMeta.isCustomNameVisible = true
            } else {
                data.entity.entityMeta.customName = null
                data.entity.entityMeta.isCustomNameVisible = false
            }
            if (hooker.utils.isGlowing(disguisedPlayer)) {
                data.entity.entityMeta.isGlowing = true
            }
            if (data.equipment.isNotEmpty()) {
                val equipPacket = WrapperPlayServerEntityEquipment(data.entity.entityId, data.equipment)
                PacketEvents.getAPI().playerManager.sendPacket(viewer, equipPacket)
            }
            // track as known viewer
            lastViewers.computeIfAbsent(disguisedPlayer) { mutableSetOf() }.add(viewer.uniqueId)
        }
    }

    fun updateEntityGlowing(player: Player, isGlowing: Boolean) {
        disguisedPlayers[player]?.let { data ->
            data.entity.entityMeta.isGlowing = isGlowing
            val viewers = getNearbyPlayers(player, player.location, data.showSelf)
            viewers.forEach { data.entity.addViewer(it.uniqueId) }
        }
    }

    fun sendSwingAnimation(player: Player) {
        val data = disguisedPlayers[player] ?: return
        if (data.type in noSwingAnimationEntities) return
        val packet = WrapperPlayServerEntityAnimation(
            data.entity.entityId,
            WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM
        )
        val viewers = getNearbyPlayers(player, player.location, data.showSelf)
        viewers.forEach { viewer ->
            PacketEvents.getAPI().playerManager.sendPacket(viewer, packet)
        }
    }

    fun recreateDisguise(player: Player, to: Location?) {
        val data = disguisedPlayers[player] ?: return
        val targetLoc = to ?: player.location
        val loc = com.github.retrooper.packetevents.protocol.world.Location(
            targetLoc.x, targetLoc.y, targetLoc.z, targetLoc.yaw, targetLoc.pitch
        )
        data.entity.teleport(loc)
        val viewers = getNearbyPlayers(player, targetLoc, data.showSelf)
        viewers.forEach { data.entity.addViewer(it.uniqueId) }
        // Re-send head look to keep rotation in sync
        val headLookPacket = WrapperPlayServerEntityHeadLook(data.entity.entityId, targetLoc.yaw)
        viewers.forEach { viewer ->
            PacketEvents.getAPI().playerManager.sendPacket(viewer, headLookPacket)
        }
        // Ensure equipment visible after teleport
        if (data.equipment.isNotEmpty()) {
            val equipPacket = WrapperPlayServerEntityEquipment(data.entity.entityId, data.equipment)
            viewers.forEach { viewer ->
                PacketEvents.getAPI().playerManager.sendPacket(viewer, equipPacket)
            }
        }
        lastViewers[player] = viewers.map { it.uniqueId }.toMutableSet()
    }

    fun updateMainHandEquipment(player: Player) {
        val data = disguisedPlayers[player] ?: return
        val state = hooker.playerStateManager.savedItems[player.uniqueId] ?: return
        val heldSlot = state.currentHotbarSlot
        val bukkitItem: ItemStack = state.hotbarItems[heldSlot] ?: ItemStack(Material.AIR)
        val packetItem = SpigotConversionUtil.fromBukkitItemStack(bukkitItem)

        val newEquipment = data.equipment.filter { it.slot != PacketEquipmentSlot.MAIN_HAND }.toMutableList()
        newEquipment.add(PacketEquipment(PacketEquipmentSlot.MAIN_HAND, packetItem))

        // Replace data in map with updated equipment list
        disguisedPlayers[player] = DisguiseData(data.entity, data.type, data.showSelf, data.showNick, newEquipment)

        val equipPacket = WrapperPlayServerEntityEquipment(data.entity.entityId, newEquipment)
        val viewerIds = lastViewers[player] ?: getNearbyPlayers(player, player.location, data.showSelf)
            .map { it.uniqueId }.toMutableSet().also { lastViewers[player] = it }
        viewerIds.forEach { uid ->
            Bukkit.getPlayer(uid)?.let { v ->
                PacketEvents.getAPI().playerManager.sendPacket(v, equipPacket)
            }
        }
    }

    private fun scheduleUpdateTask(player: Player) {
        val taskId = Bukkit.getScheduler().runTaskTimer(hooker.plugin, Runnable {
            if (!player.isOnline || !disguisedPlayers.containsKey(player)) {
                tasks.remove(player)?.let { Bukkit.getScheduler().cancelTask(it) }
                return@Runnable
            }
            val currentData = disguisedPlayers[player] ?: return@Runnable
            val playerLoc = player.location
            val loc = com.github.retrooper.packetevents.protocol.world.Location(
                playerLoc.x, playerLoc.y, playerLoc.z, playerLoc.yaw, playerLoc.pitch
            )
            // Always teleport entity to player's current position
            currentData.entity.teleport(loc)

            // Update meta only on change
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

            // Viewers management
            val viewers = getNearbyPlayers(player, player.location, currentData.showSelf)
            val known = lastViewers.computeIfAbsent(player) { mutableSetOf() }
            val currentSet = viewers.map { it.uniqueId }.toMutableSet()
            viewers.forEach { currentData.entity.addViewer(it.uniqueId) }
            if (currentData.equipment.isNotEmpty()) {
                val newViewers = currentSet.filter { it !in known }
                if (newViewers.isNotEmpty()) {
                    val equipPacket = WrapperPlayServerEntityEquipment(currentData.entity.entityId, currentData.equipment)
                    newViewers.forEach { uid ->
                        val v = Bukkit.getPlayer(uid) ?: return@forEach
                        PacketEvents.getAPI().playerManager.sendPacket(v, equipPacket)
                    }
                }
            }
            // Update head look for all current viewers
            val headLookPacket = WrapperPlayServerEntityHeadLook(currentData.entity.entityId, playerLoc.yaw)
            viewers.forEach { viewer ->
                PacketEvents.getAPI().playerManager.sendPacket(viewer, headLookPacket)
            }
            // Save current viewers snapshot
            lastViewers[player] = currentSet
        }, 0L, 2L).taskId
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