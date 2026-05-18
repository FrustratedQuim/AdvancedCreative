package com.ratger.acreative.commands.disguise.model

import com.ratger.acreative.commands.disguise.DisguiseCapabilities
import com.ratger.acreative.commands.disguise.DisguiseEntityFactory
import com.ratger.acreative.commands.disguise.DisguiseRenderProfile
import com.ratger.acreative.utils.ViewerTeamPacketSupport
import me.tofaa.entitylib.wrapper.WrapperEntity
import org.bukkit.entity.EntityType
import com.github.retrooper.packetevents.protocol.player.Equipment as PacketEquipment

data class DisguiseData(
    val entity: WrapperEntity,
    val type: EntityType,
    val identityKey: String,
    val showSelf: Boolean,
    val showNick: Boolean,
    val capabilities: DisguiseCapabilities,
    val nameMode: DisguiseEntityFactory.NameMode,
    val viewerTeam: ViewerTeamPacketSupport.Definition? = null,
    val renderProfile: DisguiseRenderProfile = DisguiseRenderProfile(),
    var equipment: List<PacketEquipment> = emptyList()
)
