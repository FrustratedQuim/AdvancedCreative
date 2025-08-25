package com.ratger.acreative.commands.disguise

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
import org.bukkit.inventory.ItemStack
import java.util.concurrent.ConcurrentHashMap
import com.github.retrooper.packetevents.protocol.player.Equipment as PacketEquipment
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot as PacketEquipmentSlot

data class DisguiseData(
    val entity: WrapperEntity,
    val type: EntityType,
    val showSelf: Boolean,
    val equipment: List<PacketEquipment> = emptyList()
)

class DisguiseManager(private val hooker: FunctionHooker) {

    val disguisedPlayers = ConcurrentHashMap<Player, DisguiseData>()
    private val tasks = ConcurrentHashMap<Player, Int>()
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

    fun disguisePlayer(player: Player, type: String?, flag: String?) {
        if (type == null) {
            if (disguisedPlayers.containsKey(player)) {
                undisguisePlayer(player)
                return
            }
            hooker.messageManager.sendMiniMessage(player, key = "usage-disguise")
            return
        }

        if (type == "off" || type.equals("player", ignoreCase = true)) {
            undisguisePlayer(player)
            return
        }

        val entityType = getEntityType(type)
        if (entityType == null) {
            hooker.messageManager.sendMiniMessage(player, key = "error-disguise-type")
            return
        }

        val newShowSelf = flag != "-noself"
        if (disguisedPlayers.containsKey(player)) {
            val currentDisguise = disguisedPlayers[player]!!
            if (currentDisguise.type == entityType && currentDisguise.showSelf == newShowSelf) {
                undisguisePlayer(player)
                return
            }
            undisguisePlayer(player, true)
        }

        if (!player.hasPermission("advancedcreative.disguise.full") && entityType in restrictedEntities) {
            hooker.messageManager.sendMiniMessage(player, key = "error-disguise-type")
            return
        }

        if (hooker.configManager.getBlockedDisguises().contains(entityType)) {
            hooker.messageManager.sendMiniMessage(player, key = "error-disguise-blocked")
            return
        }

        hooker.utils.checkAndRemovePose(player)
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
        meta.customName = getDisplayName(player)
        meta.isCustomNameVisible = true
        if (hooker.utils.isGlowing(player)) {
            meta.isGlowing = true
        }

        val showSelf = newShowSelf
        val data = DisguiseData(entity, entityType, showSelf, equipmentList)

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
        hooker.messageManager.sendMiniMessage(player, key = "success-disguise")
    }

    fun undisguisePlayer(player: Player, silent: Boolean = false) {
        disguisedPlayers[player]?.let { data ->
            data.entity.remove()
            player.isInvisible = false
            if (hooker.utils.isGlowing(player)) player.isGlowing = true
            hooker.playerStateManager.restorePlayerInventory(player)
            disguisedPlayers.remove(player)
            tasks.remove(player)?.let { Bukkit.getScheduler().cancelTask(it) }
            if (!silent) {
                hooker.messageManager.sendMiniMessage(player, key = "success-disguise-removed")
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
            data.entity.entityMeta.customName = getDisplayName(disguisedPlayer)
            data.entity.entityMeta.isCustomNameVisible = true
            if (hooker.utils.isGlowing(disguisedPlayer)) {
                data.entity.entityMeta.isGlowing = true
            }
            if (data.equipment.isNotEmpty()) {
                val equipPacket = WrapperPlayServerEntityEquipment(data.entity.entityId, data.equipment)
                PacketEvents.getAPI().playerManager.sendPacket(viewer, equipPacket)
            }
        }
    }

    fun updateEntityGlowing(player: Player, isGlowing: Boolean) {
        disguisedPlayers[player]?.let { data ->
            data.entity.entityMeta.isGlowing = isGlowing
            val viewers = getNearbyPlayers(player, player.location, data.showSelf)
            viewers.forEach { data.entity.addViewer(it.uniqueId) }
        }
    }

    fun updateMainHandEquipment(player: Player) {
        disguisedPlayers[player]?.let { data ->
            val state = hooker.playerStateManager.savedItems[player.uniqueId] ?: return
            val currentItem = state.hotbarItems[state.currentHotbarSlot]?.clone() ?: ItemStack(Material.AIR)
            val packetItem = SpigotConversionUtil.fromBukkitItemStack(currentItem)
            val newEquipment = data.equipment.filter { it.slot != PacketEquipmentSlot.MAIN_HAND }.toMutableList()
            newEquipment.add(PacketEquipment(PacketEquipmentSlot.MAIN_HAND, packetItem))
            val equipPacket = WrapperPlayServerEntityEquipment(data.entity.entityId, newEquipment)
            val viewers = getNearbyPlayers(player, player.location, data.showSelf)
            viewers.forEach { viewer ->
                PacketEvents.getAPI().playerManager.sendPacket(viewer, equipPacket)
            }
            disguisedPlayers[player] = data.copy(equipment = newEquipment)
        }
    }

    fun recreateDisguise(player: Player, newLocation: Location) {
        disguisedPlayers[player]?.let { data ->
            data.entity.remove()
            tasks.remove(player)?.let { Bukkit.getScheduler().cancelTask(it) }

            val newEntity = WrapperEntity(data.type.toPacketEventsType())
            newEntity.entityMeta.customName = getDisplayName(player)
            newEntity.entityMeta.isCustomNameVisible = true
            if (hooker.utils.isGlowing(player)) {
                newEntity.entityMeta.isGlowing = true
            }

            val newData = DisguiseData(newEntity, data.type, data.showSelf, data.equipment)
            val loc = com.github.retrooper.packetevents.protocol.world.Location(
                newLocation.x, newLocation.y, newLocation.z, newLocation.yaw, newLocation.pitch
            )

            val viewers = getNearbyPlayers(player, newLocation, data.showSelf)
            viewers.forEach { newEntity.addViewer(it.uniqueId) }
            newEntity.spawn(loc)
            if (data.equipment.isNotEmpty()) {
                val equipPacket = WrapperPlayServerEntityEquipment(newEntity.entityId, data.equipment)
                viewers.forEach { viewer ->
                    PacketEvents.getAPI().playerManager.sendPacket(viewer, equipPacket)
                }
            }

            disguisedPlayers[player] = newData
            scheduleUpdateTask(player)
        }
    }

    fun sendSwingAnimation(player: Player) {
        disguisedPlayers[player]?.let { data ->
            if (data.type in noSwingAnimationEntities) return
            val animationPacket = WrapperPlayServerEntityAnimation(data.entity.entityId, WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM)
            val viewers = getNearbyPlayers(player, player.location, data.showSelf)
            viewers.forEach { viewer ->
                PacketEvents.getAPI().playerManager.sendPacket(viewer, animationPacket)
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
            currentData.entity.teleport(loc)
            currentData.entity.entityMeta.customName = getDisplayName(player)
            currentData.entity.entityMeta.isCustomNameVisible = true
            if (hooker.utils.isGlowing(player)) {
                currentData.entity.entityMeta.isGlowing = true
            }
            val viewers = getNearbyPlayers(player, player.location, currentData.showSelf)
            viewers.forEach { currentData.entity.addViewer(it.uniqueId) }
            if (currentData.equipment.isNotEmpty()) {
                val equipPacket = WrapperPlayServerEntityEquipment(currentData.entity.entityId, currentData.equipment)
                viewers.forEach { viewer ->
                    PacketEvents.getAPI().playerManager.sendPacket(viewer, equipPacket)
                }
            }
            val headLookPacket = WrapperPlayServerEntityHeadLook(currentData.entity.entityId, playerLoc.yaw)
            viewers.forEach { viewer ->
                PacketEvents.getAPI().playerManager.sendPacket(viewer, headLookPacket)
            }
        }, 0L, 2L).taskId
        tasks[player] = taskId
    }

    private fun EntityType.toPacketEventsType(): com.github.retrooper.packetevents.protocol.entity.type.EntityType {
        return EntityTypes.getByName("minecraft:${this.name.lowercase()}")
    }
}