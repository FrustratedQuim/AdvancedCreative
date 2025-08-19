package com.ratger.acreative

import org.bukkit.plugin.java.JavaPlugin
import com.ratger.acreative.core.FunctionHooker

class AdvancedCreative : JavaPlugin() {

    lateinit var functionHooker: FunctionHooker

    override fun onEnable() {
        functionHooker = FunctionHooker(this)
        functionHooker.init()
    }

    override fun onDisable() {
        functionHooker.shutdown()
    }
}