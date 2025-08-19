package com.ratger.acreative.utils

import de.oliver.fancynpcs.api.FancyNpcsPlugin
import de.oliver.fancynpcs.api.Npc
import de.oliver.fancynpcs.api.NpcData
import de.oliver.fancynpcs.api.utils.NpcEquipmentSlot
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player

class EntityManager {
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

    fun createNpc(player: Player, location: Location, yaw: Float): Npc {

        val npcData = NpcData(player.name, player.uniqueId, location).apply {

            location.yaw = yaw
            isCollidable = false
            setSkin(player.uniqueId.toString())

            val poseAttr = FancyNpcsPlugin.get().attributeManager.getAttributeByName(EntityType.PLAYER, "pose")
            attributes[poseAttr] = "sleeping"

            player.inventory.helmet?.clone()?.let { equipment[NpcEquipmentSlot.HEAD] = it }
            player.inventory.chestplate?.clone()?.let { equipment[NpcEquipmentSlot.CHEST] = it }
            player.inventory.leggings?.clone()?.let { equipment[NpcEquipmentSlot.LEGS] = it }
            player.inventory.boots?.clone()?.let { equipment[NpcEquipmentSlot.FEET] = it }
            equipment[NpcEquipmentSlot.MAINHAND] = player.inventory.itemInMainHand.clone()
            equipment[NpcEquipmentSlot.OFFHAND] = player.inventory.itemInOffHand.clone()
        }

        val npc = FancyNpcsPlugin.get().npcAdapter.apply(npcData).apply {
            isSaveToFile = false
            FancyNpcsPlugin.get().npcManager.registerNpc(this)
            create()
            spawnForAll()
        }

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "npc displayname ${npc.data.name} @none")
        npc.updateForAll()

        npc.entityId

        return npc
    }
}