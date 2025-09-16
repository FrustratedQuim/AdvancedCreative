package com.ratger.acreative.utils

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.player.Equipment
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot
import com.github.retrooper.packetevents.protocol.player.TextureProperty
import com.github.retrooper.packetevents.protocol.player.UserProfile
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate
import com.ratger.acreative.core.FunctionHooker
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import me.tofaa.entitylib.meta.types.PlayerMeta
import me.tofaa.entitylib.wrapper.WrapperEntity
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket
import net.minecraft.world.scores.PlayerTeam
import net.minecraft.world.scores.Scoreboard
import net.minecraft.world.scores.Team
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import java.util.*
import com.github.retrooper.packetevents.protocol.world.Location as PacketLocation

class EntityManager(
    private val hooker: FunctionHooker
) {

    fun createArmorStand(location: Location, yaw: Float): ArmorStand {
        return location.world.spawn(location, ArmorStand::class.java) { stand ->
            stand.setGravity(false)
            stand.setBasePlate(false)
            stand.isInvisible = true
            stand.isSmall = true
            stand.isMarker = true
            stand.isInvulnerable = true
            stand.isSilent = true
            stand.isCollidable = false
            stand.isCustomNameVisible = false
            stand.setRotation(yaw, 0f)
        }
    }

    fun createPlayerNPC(player: Player, location: Location, yaw: Float, isGlowing: Boolean): Triple<WrapperEntity, List<Equipment>, String> {

        val npcUUID = UUID.randomUUID()
        val entity = WrapperEntity(npcUUID, EntityTypes.PLAYER)
        val playerMeta = entity.entityMeta as PlayerMeta

        playerMeta.pose = EntityPose.SLEEPING
        playerMeta.isGlowing = isGlowing

        val craftPlayer = player as CraftPlayer
        val gameProfile = craftPlayer.profile
        val textureProperties = mutableListOf<TextureProperty>()
        gameProfile.properties.get("textures").forEach { property ->
            textureProperties.add(TextureProperty("textures", property.value, property.signature ?: ""))
        }

        Bukkit.getOnlinePlayers().forEach { viewer ->
            if (!hooker.utils.isHiddenFromPlayer(viewer, player)) {
                entity.addViewer(viewer.uniqueId)
            }
        }

        val profileName = "NPC_${npcUUID.toString().substring(0,8)}"
        val userProfile = UserProfile(npcUUID, profileName, textureProperties)

        val teamName = "hide_${npcUUID.toString().substring(0,8)}"
        val scoreboard = Scoreboard()
        val team = PlayerTeam(scoreboard, teamName)
        team.playerPrefix = net.minecraft.network.chat.Component.empty()
        team.playerSuffix = net.minecraft.network.chat.Component.empty()
        team.nameTagVisibility = Team.Visibility.NEVER
        team.collisionRule = Team.CollisionRule.NEVER
        team.players.add(profileName)
        val createPacket = ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, true)
        Bukkit.getOnlinePlayers().forEach { viewer ->
            if (!hooker.utils.isHiddenFromPlayer(viewer, player)) {
                (viewer as CraftPlayer).handle.connection.send(createPacket)
            }
        }

        val playerInfoUpdate = WrapperPlayServerPlayerInfoUpdate(
            WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER,
            WrapperPlayServerPlayerInfoUpdate.PlayerInfo(userProfile)
        )
        Bukkit.getOnlinePlayers().forEach { viewer ->
            if (!hooker.utils.isHiddenFromPlayer(viewer, player)) {
                PacketEvents.getAPI().playerManager.sendPacket(viewer, playerInfoUpdate)
            }
        }

        val equipment = mutableListOf<Equipment>()
        player.inventory.helmet?.clone()?.let {
            equipment.add(
                Equipment(
                    EquipmentSlot.HELMET,
                    SpigotConversionUtil.fromBukkitItemStack(it)
                )
            )
        }
        player.inventory.chestplate?.clone()?.let {
            equipment.add(
                Equipment(
                    EquipmentSlot.CHEST_PLATE,
                    SpigotConversionUtil.fromBukkitItemStack(it)
                )
            )
        }
        player.inventory.leggings?.clone()?.let {
            equipment.add(
                Equipment(
                    EquipmentSlot.LEGGINGS,
                    SpigotConversionUtil.fromBukkitItemStack(it)
                )
            )
        }
        player.inventory.boots?.clone()?.let {
            equipment.add(
                Equipment(
                    EquipmentSlot.BOOTS,
                    SpigotConversionUtil.fromBukkitItemStack(it)
                )
            )
        }
        player.inventory.itemInMainHand.clone().let {
            equipment.add(
                Equipment(
                    EquipmentSlot.MAIN_HAND,
                    SpigotConversionUtil.fromBukkitItemStack(it)
                )
            )
        }
        player.inventory.itemInOffHand.clone().let {
            equipment.add(
                Equipment(
                    EquipmentSlot.OFF_HAND,
                    SpigotConversionUtil.fromBukkitItemStack(it)
                )
            )
        }
        val equipPacket = WrapperPlayServerEntityEquipment(entity.entityId, equipment)
        Bukkit.getOnlinePlayers().forEach { viewer ->
            if (!hooker.utils.isHiddenFromPlayer(viewer, player)) {
                PacketEvents.getAPI().playerManager.sendPacket(viewer, equipPacket)
            }
        }

        Bukkit.getScheduler().runTaskLater(hooker.plugin, Runnable {
            val subEquipPacket = WrapperPlayServerEntityEquipment(entity.entityId, equipment)
            Bukkit.getOnlinePlayers().forEach { viewer ->
                if (!hooker.utils.isHiddenFromPlayer(viewer, player)) {
                    PacketEvents.getAPI().playerManager.sendPacket(viewer, subEquipPacket)
                }
            }
        }, 1L)

        entity.spawn(PacketLocation(location.x, location.y, location.z, yaw, 0f))

        return Triple(entity, equipment, teamName)
    }

    fun updatePlayerNPCEquipment(npc: WrapperEntity, equipment: List<Equipment>, player: Player) {
        val equipPacket = WrapperPlayServerEntityEquipment(npc.entityId, equipment)
        val viewers = Bukkit.getOnlinePlayers().filter { viewer ->
            !hooker.utils.isHiddenFromPlayer(viewer, player)
        }
        viewers.forEach { viewer ->
            PacketEvents.getAPI().playerManager.sendPacket(viewer, equipPacket)
        }
    }
}