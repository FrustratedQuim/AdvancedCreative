@file:Suppress("DEPRECATION", "removal")

package com.ratger.acreative.commands.disguise

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.PacketContainer
import com.comphenix.protocol.wrappers.WrappedChatComponent
import com.comphenix.protocol.wrappers.WrappedDataValue
import com.comphenix.protocol.wrappers.WrappedDataWatcher.Registry
import com.comphenix.protocol.wrappers.EnumWrappers.ItemSlot
import com.comphenix.protocol.wrappers.Pair as ProtocolPair
import com.ratger.acreative.core.FunctionHooker
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.concurrent.ConcurrentHashMap

data class DisguiseData(
    val entityId: Int,
    val uuid: UUID,
    val type: EntityType,
    val equipment: List<ProtocolPair<ItemSlot, ItemStack>>,
    val showSelf: Boolean
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

    private fun createMetadataPacket(player: Player): WrappedDataValue {
        val nameJson = GsonComponentSerializer.gson().serialize(getDisplayName(player))
        return WrappedDataValue(
            2,
            Registry.getChatComponentSerializer(true),
            Optional.of(WrappedChatComponent.fromJson(nameJson).handle)
        )
    }

    private fun sendPacketToPlayers(packet: Any, players: List<Player>, playerName: String, packetType: String) {
        players.forEach {
            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(it, packet as PacketContainer)
            } catch (e: Exception) {
                hooker.plugin.logger.warning("Failed to send $packetType packet to ${it.name} for $playerName: ${e.message}")
            }
        }
    }

    private fun createSpawnPacket(data: DisguiseData, location: Location): Any {
        val spawnPacket = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SPAWN_ENTITY)
        spawnPacket.integers.write(0, data.entityId)
        spawnPacket.uuiDs.write(0, data.uuid)
        spawnPacket.entityTypeModifier.write(0, data.type)
        spawnPacket.doubles
            .write(0, location.x)
            .write(1, location.y)
            .write(2, location.z)
        return spawnPacket
    }

    private fun createMetadataPacket(entityId: Int, player: Player, isGlowing: Boolean): Any {
        val metadataPacket = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_METADATA)
        metadataPacket.integers.write(0, entityId)
        val dataValues = mutableListOf(
            createMetadataPacket(player),
            WrappedDataValue(3, Registry.get(java.lang.Boolean::class.java), true)
        )
        if (isGlowing) {
            dataValues.add(WrappedDataValue(0, Registry.get(java.lang.Byte::class.java), 0x40.toByte()))
        }
        metadataPacket.dataValueCollectionModifier.write(0, dataValues)
        return metadataPacket
    }

    private fun createEquipmentPacket(entityId: Int, equipment: List<ProtocolPair<ItemSlot, ItemStack>>): Any {
        val equipPacket = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT)
        equipPacket.integers.write(0, entityId)
        equipPacket.slotStackPairLists.write(0, equipment)
        return equipPacket
    }

    private fun createUpdatePackets(data: DisguiseData, player: Player): Pair<Any, Any> {
        val yaw = (player.location.yaw * 256 / 360).toInt().toByte()
        val pitch = (player.location.pitch * 256 / 360).toInt().toByte()

        val headRotationPacket = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_HEAD_ROTATION)
        headRotationPacket.integers.write(0, data.entityId)
        headRotationPacket.bytes.write(0, yaw)

        val teleportPacket = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_TELEPORT)
        teleportPacket.integers.write(0, data.entityId)
        teleportPacket.doubles
            .write(0, player.location.x)
            .write(1, player.location.y)
            .write(2, player.location.z)
        teleportPacket.bytes.write(0, yaw).write(1, pitch)
        teleportPacket.booleans.write(0, true)

        return headRotationPacket to teleportPacket
    }

    private fun scheduleUpdateTask(player: Player, data: DisguiseData) {
        val taskId = Bukkit.getScheduler().runTaskTimer(hooker.plugin, Runnable {
            if (!player.isOnline || !disguisedPlayers.containsKey(player)) {
                tasks.remove(player)?.let { Bukkit.getScheduler().cancelTask(it) }
                return@Runnable
            }
            val (headRotationPacket, teleportPacket) = createUpdatePackets(data, player)
            val nearbyPlayers = getNearbyPlayers(player, player.location, data.showSelf)
            sendPacketToPlayers(headRotationPacket, nearbyPlayers, player.name, "ENTITY_HEAD_ROTATION")
            sendPacketToPlayers(teleportPacket, nearbyPlayers, player.name, "ENTITY_TELEPORT")
        }, 0L, 1L).taskId
        tasks[player] = taskId
    }

    fun disguisePlayer(player: Player, type: String?, flag: String?) {
        if (type.isNullOrEmpty() || type.equals("off", true) || type.equals("player", true)) {
            if (disguisedPlayers.containsKey(player)) {
                undisguisePlayer(player)
            } else {
                hooker.messageManager.sendMiniMessage(player, key = "usage-disguise")
            }
            return
        }

        val entityType = try {
            EntityType.valueOf(type.uppercase())
        } catch (_: IllegalArgumentException) {
            hooker.messageManager.sendMiniMessage(player, key = "error-disguise-unknown")
            return
        }

        if (disguisedPlayers.containsKey(player)) {
            val currentDisguise = disguisedPlayers[player]!!.type
            if (currentDisguise == entityType) {
                undisguisePlayer(player)
                return
            } else {
                undisguisePlayer(player, silent = true)
            }
        }

        if (hooker.configManager.getBlockedDisguises().contains(entityType)) {
            hooker.messageManager.sendMiniMessage(player, key = "error-disguise-disabled")
            return
        }

        if (restrictedEntities.contains(entityType) && !player.hasPermission("advancedcreative.disguise.full")) {
            hooker.messageManager.sendMiniMessage(player, key = "permission-horizon")
            return
        }

        hooker.utils.unsetAllPoses(player)
        hooker.playerStateManager.savePlayerInventory(player)

        val equipment = if (entityType.isAlive) {
            val inv = player.inventory
            listOfNotNull(
                inv.helmet?.clone()?.let { ProtocolPair(ItemSlot.HEAD, it) },
                inv.chestplate?.clone()?.let { ProtocolPair(ItemSlot.CHEST, it) },
                inv.leggings?.clone()?.let { ProtocolPair(ItemSlot.LEGS, it) },
                inv.boots?.clone()?.let { ProtocolPair(ItemSlot.FEET, it) },
                inv.itemInMainHand.takeIf { !it.type.isAir }?.clone()?.let { ProtocolPair(ItemSlot.MAINHAND, it) },
                inv.itemInOffHand.takeIf { !it.type.isAir }?.clone()?.let { ProtocolPair(ItemSlot.OFFHAND, it) }
            )
        } else emptyList()

        player.inventory.armorContents = arrayOf(null, null, null, null)
        player.inventory.setItemInOffHand(null)
        player.inventory.setItem(player.inventory.heldItemSlot, null)

        val entityId = Random().nextInt(1000000) + 10000
        val entityUUID = UUID.randomUUID()
        val showSelf = flag != "-noself"
        val data = DisguiseData(entityId, entityUUID, entityType, equipment, showSelf)

        val nearbyPlayers = getNearbyPlayers(player, player.location, showSelf)
        sendPacketToPlayers(createSpawnPacket(data, player.location), nearbyPlayers, player.name, "SPAWN_ENTITY")
        sendPacketToPlayers(createMetadataPacket(entityId, player, hooker.utils.isGlowing(player)), nearbyPlayers, player.name, "ENTITY_METADATA")
        if (equipment.isNotEmpty()) {
            sendPacketToPlayers(createEquipmentPacket(entityId, equipment), nearbyPlayers, player.name, "ENTITY_EQUIPMENT")
        }

        player.isInvisible = true
        disguisedPlayers[player] = data
        player.isGlowing = false
        scheduleUpdateTask(player, data)
        hooker.messageManager.sendMiniMessage(player, key = "success-disguise")
    }

    fun undisguisePlayer(player: Player, silent: Boolean = false) {
        disguisedPlayers[player]?.let { data ->
            val destroyPacket = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_DESTROY)
            destroyPacket.intLists.write(0, listOf(data.entityId))
            sendPacketToPlayers(destroyPacket, getNearbyPlayers(player, player.location, data.showSelf), player.name, "ENTITY_DESTROY")

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

            val (headRotationPacket, teleportPacket) = createUpdatePackets(data, disguisedPlayer)
            sendPacketToPlayers(createSpawnPacket(data, disguisedPlayer.location), listOf(viewer), disguisedPlayer.name, "SPAWN_ENTITY")
            sendPacketToPlayers(headRotationPacket, listOf(viewer), disguisedPlayer.name, "ENTITY_HEAD_ROTATION")
            sendPacketToPlayers(teleportPacket, listOf(viewer), disguisedPlayer.name, "ENTITY_TELEPORT")
            sendPacketToPlayers(createMetadataPacket(data.entityId, disguisedPlayer, hooker.utils.isGlowing(disguisedPlayer)), listOf(viewer), disguisedPlayer.name, "ENTITY_METADATA")
            if (data.equipment.isNotEmpty()) {
                sendPacketToPlayers(createEquipmentPacket(data.entityId, data.equipment), listOf(viewer), disguisedPlayer.name, "ENTITY_EQUIPMENT")
            }

            Bukkit.getScheduler().runTaskLater(hooker.plugin, Runnable {
                if (!viewer.isOnline || !disguisedPlayer.isOnline) return@Runnable
                sendPacketToPlayers(headRotationPacket, listOf(viewer), disguisedPlayer.name, "ENTITY_HEAD_ROTATION")
                sendPacketToPlayers(teleportPacket, listOf(viewer), disguisedPlayer.name, "ENTITY_TELEPORT")
            }, 2L)
        }
    }

    fun updateEntityGlowing(player: Player, isGlowing: Boolean) {
        disguisedPlayers[player]?.let { data ->
            val metadataPacket = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_METADATA)
            metadataPacket.integers.write(0, data.entityId)
            val dataValues = mutableListOf(
                WrappedDataValue(0, Registry.get(java.lang.Byte::class.java), if (isGlowing) 0x40.toByte() else 0x00.toByte()),
                createMetadataPacket(player),
                WrappedDataValue(3, Registry.get(java.lang.Boolean::class.java), true)
            )
            metadataPacket.dataValueCollectionModifier.write(0, dataValues)
            sendPacketToPlayers(metadataPacket, getNearbyPlayers(player, player.location, data.showSelf), player.name, "ENTITY_METADATA")
        }
    }

    fun updateMainHandEquipment(player: Player) {
        disguisedPlayers[player]?.let { data ->
            val state = hooker.playerStateManager.savedItems[player.uniqueId] ?: return
            val currentItem = state.hotbarItems[state.currentHotbarSlot]?.clone() ?: ItemStack(Material.AIR)
            val equipPacket = createEquipmentPacket(data.entityId, listOf(ProtocolPair(ItemSlot.MAINHAND, currentItem)))
            sendPacketToPlayers(equipPacket, getNearbyPlayers(player, player.location, data.showSelf), player.name, "ENTITY_EQUIPMENT")
        }
    }

    fun recreateDisguise(player: Player, newLocation: Location) {
        disguisedPlayers[player]?.let { data ->
            val destroyPacket = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_DESTROY)
            destroyPacket.intLists.write(0, listOf(data.entityId))
            sendPacketToPlayers(destroyPacket, getNearbyPlayers(player, player.location, data.showSelf), player.name, "ENTITY_DESTROY")
            tasks.remove(player)?.let { Bukkit.getScheduler().cancelTask(it) }

            val newEntityId = Random().nextInt(1000000) + 10000
            val newEntityUUID = UUID.randomUUID()
            val newData = DisguiseData(newEntityId, newEntityUUID, data.type, data.equipment, data.showSelf)

            val nearbyPlayers = getNearbyPlayers(player, newLocation, data.showSelf)
            sendPacketToPlayers(createSpawnPacket(newData, newLocation), nearbyPlayers, player.name, "SPAWN_ENTITY")
            sendPacketToPlayers(createMetadataPacket(newEntityId, player, hooker.utils.isGlowing(player)), nearbyPlayers, player.name, "ENTITY_METADATA")
            if (data.equipment.isNotEmpty()) {
                sendPacketToPlayers(createEquipmentPacket(newEntityId, data.equipment), nearbyPlayers, player.name, "ENTITY_EQUIPMENT")
            }

            disguisedPlayers[player] = newData
            scheduleUpdateTask(player, newData)
        }
    }

    fun sendSwingAnimation(player: Player) {
        disguisedPlayers[player]?.let { data ->
            if (data.type in noSwingAnimationEntities) return
            val animationPacket = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ANIMATION)
            animationPacket.integers.write(0, data.entityId).write(1, 0)
            sendPacketToPlayers(animationPacket, getNearbyPlayers(player, player.location, data.showSelf), player.name, "ENTITY_ANIMATION")
        }
    }
}