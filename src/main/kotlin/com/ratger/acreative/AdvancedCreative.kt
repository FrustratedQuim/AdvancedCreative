package com.ratger.acreative

import com.github.retrooper.packetevents.PacketEvents
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import me.tofaa.entitylib.EntityLib
import org.bukkit.plugin.java.JavaPlugin
import com.ratger.acreative.core.FunctionHooker
import me.tofaa.entitylib.APIConfig
import me.tofaa.entitylib.spigot.SpigotEntityLibPlatform

class AdvancedCreative : JavaPlugin() {

    lateinit var functionHooker: FunctionHooker

    override fun onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this))
        PacketEvents.getAPI().load()
    }

    override fun onEnable() {
        PacketEvents.getAPI().init()

        val platform = SpigotEntityLibPlatform(this)
        val settings = APIConfig(PacketEvents.getAPI())
            .tickTickables()
            .usePlatformLogger()
        EntityLib.init(platform, settings)

        functionHooker = FunctionHooker(this)
        functionHooker.init()
    }

    override fun onDisable() {
        functionHooker.shutdown()
        PacketEvents.getAPI().terminate()
    }
}
