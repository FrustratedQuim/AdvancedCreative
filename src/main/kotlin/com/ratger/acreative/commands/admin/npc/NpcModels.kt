package com.ratger.acreative.commands.admin.npc

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

data class NpcLocation(
    val worldName: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float
)

data class NpcSkin(
    val textureValue: String,
    val textureSignature: String?
)

data class NpcEquipment(
    val helmet: ItemStack?,
    val chestplate: ItemStack?,
    val leggings: ItemStack?,
    val boots: ItemStack?,
    val mainHand: ItemStack?,
    val offHand: ItemStack?
) {
    fun copyDeep(): NpcEquipment = NpcEquipment(
        helmet = helmet?.clone(),
        chestplate = chestplate?.clone(),
        leggings = leggings?.clone(),
        boots = boots?.clone(),
        mainHand = mainHand?.clone(),
        offHand = offHand?.clone()
    )

    companion object {
        val EMPTY = NpcEquipment(
            helmet = null,
            chestplate = null,
            leggings = null,
            boots = null,
            mainHand = null,
            offHand = null
        )
    }
}

data class NpcProfile(
    val name: String,
    val location: NpcLocation,
    val visualNick: String,
    val skin: NpcSkin?,
    val equipment: NpcEquipment
) {
    fun effectiveVisualNick(): String = visualNick.ifBlank { name }

    fun copyDeep(): NpcProfile = copy(
        location = location.copy(),
        visualNick = visualNick,
        skin = skin?.copy(),
        equipment = equipment.copyDeep()
    )
}

data class NpcNickDisplaySettings(
    val verticalOffset: Double,
    val visibilityRadius: Double,
    val viewRange: Float,
    val isSeeThrough: Boolean
)

enum class NpcInteractionType {
    LEFT_CLICK,
    RIGHT_CLICK
}

data class NpcInteractionContext(
    val player: Player,
    val profile: NpcProfile,
    val interactionType: NpcInteractionType
)

fun interface NpcInteractionHandler {
    fun handle(context: NpcInteractionContext)
}
