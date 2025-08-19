package com.ratger.acreative.core

import com.ratger.acreative.AdvancedCreative
import com.ratger.acreative.commands.CommandManager
import com.ratger.acreative.commands.bind.BindManager
import com.ratger.acreative.commands.crawl.CrawlManager
import com.ratger.acreative.commands.disguise.DisguiseManager
import com.ratger.acreative.commands.effects.EffectsManager
import com.ratger.acreative.commands.freeze.FreezeManager
import com.ratger.acreative.commands.glide.GlideManager
import com.ratger.acreative.commands.glow.GlowManager
import com.ratger.acreative.commands.gravity.GravityManager
import com.ratger.acreative.commands.health.HealthManager
import com.ratger.acreative.commands.hide.HideManager
import com.ratger.acreative.commands.lay.LayManager
import com.ratger.acreative.commands.piss.PissManager
import com.ratger.acreative.commands.resize.ResizeManager
import com.ratger.acreative.commands.sit.SitManager
import com.ratger.acreative.commands.slap.SlapManager
import com.ratger.acreative.commands.sneeze.SneezeManager
import com.ratger.acreative.commands.spit.SpitManager
import com.ratger.acreative.commands.strength.StrengthManager
import com.ratger.acreative.utils.EntityManager
import com.ratger.acreative.utils.EventHandler
import com.ratger.acreative.utils.PlayerStateManager
import com.ratger.acreative.utils.Utils

class FunctionHooker(val plugin: AdvancedCreative) {

    lateinit var configManager: ConfigManager
        private set
    lateinit var messageManager: MessageManager
        private set
    lateinit var commandManager: CommandManager
        private set
    lateinit var playerStateManager: PlayerStateManager
        private set
    lateinit var entityManager: EntityManager
        private set
    lateinit var sitManager: SitManager
        private set
    lateinit var glideManager: GlideManager
        private set
    lateinit var sneezeManager: SneezeManager
        private set
    lateinit var crawlManager: CrawlManager
        private set
    lateinit var hideManager: HideManager
        private set
    lateinit var layManager: LayManager
        private set
    lateinit var gravityManager: GravityManager
        private set
    lateinit var resizeManager: ResizeManager
        private set
    lateinit var strengthManager: StrengthManager
        private set
    lateinit var healthManager: HealthManager
        private set
    lateinit var freezeManager: FreezeManager
        private set
    lateinit var bindManager: BindManager
        private set
    lateinit var glowManager: GlowManager
        private set
    lateinit var spitManager: SpitManager
        private set
    lateinit var pissManager: PissManager
        private set
    lateinit var disguiseManager: DisguiseManager
        private set
    lateinit var effectsManager: EffectsManager
        private set
    lateinit var slapManager: SlapManager
        private set
    lateinit var utils: Utils
        private set

    fun init() {
        configManager = ConfigManager(this)
        configManager.initConfigs()

        messageManager = MessageManager(this, configManager)
        playerStateManager = PlayerStateManager(this)
        entityManager = EntityManager()
        sitManager = SitManager(this)
        glideManager = GlideManager(this)
        sneezeManager = SneezeManager(this)
        crawlManager = CrawlManager(this)
        hideManager = HideManager(this)
        layManager = LayManager(this)
        gravityManager = GravityManager(this)
        resizeManager = ResizeManager(this)
        strengthManager = StrengthManager(this)
        healthManager = HealthManager(this)
        freezeManager = FreezeManager(this)
        bindManager = BindManager(this)
        glowManager = GlowManager(this)
        spitManager = SpitManager(this)
        pissManager = PissManager(this)
        disguiseManager = DisguiseManager(this)
        effectsManager = EffectsManager(this)
        slapManager = SlapManager(this)

        utils = Utils(
            this,
            sitManager,
            glideManager,
            crawlManager,
            hideManager,
            layManager,
            gravityManager,
            resizeManager,
            strengthManager,
            healthManager,
            freezeManager,
            bindManager,
            glowManager,
            pissManager,
            disguiseManager,
            effectsManager,
            slapManager
        )

        commandManager = CommandManager(this)
        val commands = listOf(
            "ahelp",
            "sit",
            "lay",
            "crawl",
            "hide",
            "sneeze",
            "glide",
            "gravity",
            "resize",
            "strength",
            "health",
            "freeze",
            "bind",
            "glow",
            "spit",
            "piss",
            "disguise",
            "effects",
            "slap"
        )
        for (cmd in commands) {
            plugin.getCommand(cmd)?.setExecutor(commandManager)
        }

        plugin.server.pluginManager.registerEvents(EventHandler(this), plugin)

        sitManager.startArmorStandChecker()
        crawlManager.startBarrierUpdater()
        layManager.startArmorStandChecker()
    }

    fun shutdown() {
        utils.stopAllSits()
        utils.stopAllGlides()
        utils.stopAllCrawls()
        utils.stopAllHides()
        utils.stopAllLays()
        utils.stopAllCustomGravity()
        utils.stopAllCustomResize()
        utils.stopAllCustomStrength()
        utils.stopAllCustomHealth()
        utils.stopAllFreezes()
        utils.stopAllBinds()
        utils.stopAllGlows()
        utils.stopAllPiss()
        utils.stopAllDisguises()
        utils.stopAllPiss()
        utils.stopAllCustomEffects()
        utils.stopAllSlaps()
        messageManager.clearAllTasks()
    }
}