package com.ratger.acreative.core

import com.ratger.acreative.AdvancedCreative
import com.ratger.acreative.commands.CommandManager
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.commands.crawl.CrawlManager
import com.ratger.acreative.commands.disguise.DisguiseManager
import com.ratger.acreative.commands.effects.EffectsManager
import com.ratger.acreative.commands.freeze.FreezeManager
import com.ratger.acreative.commands.glide.GlideManager
import com.ratger.acreative.commands.glow.GlowManager
import com.ratger.acreative.commands.gravity.GravityManager
import com.ratger.acreative.commands.grab.GrabManager
import com.ratger.acreative.commands.health.HealthManager
import com.ratger.acreative.commands.hide.HideManager
import com.ratger.acreative.commands.itemdb.ItemdbManager
import com.ratger.acreative.commands.jar.JarManager
import com.ratger.acreative.commands.lay.LayManager
import com.ratger.acreative.commands.piss.PissManager
import com.ratger.acreative.commands.resize.ResizeManager
import com.ratger.acreative.commands.sit.SitManager
import com.ratger.acreative.commands.sit.SitheadManager
import com.ratger.acreative.commands.slap.SlapManager
import com.ratger.acreative.commands.sneeze.SneezeManager
import com.ratger.acreative.commands.spit.SpitManager
import com.ratger.acreative.commands.strength.StrengthManager
import com.ratger.acreative.utils.*

class FunctionHooker(val plugin: AdvancedCreative) {

    lateinit var configManager: ConfigManager
        private set
    lateinit var messageManager: MessageManager
        private set
    lateinit var permissionManager: PermissionManager
        private set
    lateinit var commandManager: CommandManager
        private set
    lateinit var playerStateManager: PlayerStateManager
        private set
    lateinit var entityManager: EntityManager
        private set
    lateinit var sitManager: SitManager
        private set
    lateinit var sitheadManager: SitheadManager
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
    lateinit var grabManager: GrabManager
        private set
    lateinit var jarManager: JarManager
        private set
    lateinit var slapManager: SlapManager
        private set
    lateinit var itemdbManager: ItemdbManager
        private set
    lateinit var utils: Utils
        private set
    lateinit var packetHandler: PacketHandler
        private set
    lateinit var tickScheduler: TickScheduler
        private set

    fun sitManagerOrNull(): SitManager? = if (this::sitManager.isInitialized) sitManager else null
    fun glideManagerOrNull(): GlideManager? = if (this::glideManager.isInitialized) glideManager else null
    fun crawlManagerOrNull(): CrawlManager? = if (this::crawlManager.isInitialized) crawlManager else null
    fun hideManagerOrNull(): HideManager? = if (this::hideManager.isInitialized) hideManager else null
    fun layManagerOrNull(): LayManager? = if (this::layManager.isInitialized) layManager else null
    fun gravityManagerOrNull(): GravityManager? = if (this::gravityManager.isInitialized) gravityManager else null
    fun resizeManagerOrNull(): ResizeManager? = if (this::resizeManager.isInitialized) resizeManager else null
    fun strengthManagerOrNull(): StrengthManager? = if (this::strengthManager.isInitialized) strengthManager else null
    fun healthManagerOrNull(): HealthManager? = if (this::healthManager.isInitialized) healthManager else null
    fun freezeManagerOrNull(): FreezeManager? = if (this::freezeManager.isInitialized) freezeManager else null
    fun glowManagerOrNull(): GlowManager? = if (this::glowManager.isInitialized) glowManager else null
    fun pissManagerOrNull(): PissManager? = if (this::pissManager.isInitialized) pissManager else null
    fun disguiseManagerOrNull(): DisguiseManager? = if (this::disguiseManager.isInitialized) disguiseManager else null
    fun effectsManagerOrNull(): EffectsManager? = if (this::effectsManager.isInitialized) effectsManager else null
    fun slapManagerOrNull(): SlapManager? = if (this::slapManager.isInitialized) slapManager else null
    fun grabManagerOrNull(): GrabManager? = if (this::grabManager.isInitialized) grabManager else null
    fun jarManagerOrNull(): JarManager? = if (this::jarManager.isInitialized) jarManager else null

    fun init() {
        configManager = ConfigManager(this)
        configManager.initConfigs()

        tickScheduler = TickScheduler(plugin)
        tickScheduler.start()

        utils = Utils(this)

        messageManager = MessageManager(this)
        permissionManager = PermissionManager(this)
        playerStateManager = PlayerStateManager(this)
        entityManager = EntityManager(this)
        sitManager = SitManager(this)
        sitheadManager = SitheadManager(this)
        glideManager = GlideManager(this)
        crawlManager = CrawlManager(this)
        hideManager = HideManager(this)
        layManager = LayManager(this)
        gravityManager = GravityManager(this)
        resizeManager = ResizeManager(this)
        strengthManager = StrengthManager(this)
        healthManager = HealthManager(this)
        freezeManager = FreezeManager(this)
        glowManager = GlowManager(this)
        spitManager = SpitManager(this)
        pissManager = PissManager(this)
        disguiseManager = DisguiseManager(this)
        effectsManager = EffectsManager(this)
        grabManager = GrabManager(this)
        jarManager = JarManager(this)
        slapManager = SlapManager(this)
        itemdbManager = ItemdbManager(this)

        playerStateManager.registerDeactivator(PlayerStateManager.PlayerStateType.CRAWLING) { crawlManager.uncrawlPlayer(it) }
        playerStateManager.registerDeactivator(PlayerStateManager.PlayerStateType.DISGUISED) { disguiseManager.undisguisePlayer(it) }
        playerStateManager.registerDeactivator(PlayerStateManager.PlayerStateType.FROZEN) { freezeManager.unfreezePlayer(it) }
        playerStateManager.registerDeactivator(PlayerStateManager.PlayerStateType.GLIDING) { glideManager.unglidePlayer(it) }
        playerStateManager.registerDeactivator(PlayerStateManager.PlayerStateType.PISSING) { pissManager.stopPiss(it) }
        playerStateManager.registerDeactivator(PlayerStateManager.PlayerStateType.LAYING) { layManager.unlayPlayer(it) }
        playerStateManager.registerDeactivator(PlayerStateManager.PlayerStateType.SITTING) { sitManager.unsitPlayer(it) }
        playerStateManager.registerDeactivator(PlayerStateManager.PlayerStateType.GRABBING) { grabManager.releaseForPlayer(it) }
        playerStateManager.registerDeactivator(PlayerStateManager.PlayerStateType.GRABBED) { grabManager.releaseForPlayer(it) }
        playerStateManager.registerDeactivator(PlayerStateManager.PlayerStateType.JARRED) { jarManager.releaseForPlayer(it) }
        playerStateManager.registerDeactivator(PlayerStateManager.PlayerStateType.CUSTOM_SIZE) { resizeManager.removeEffect(it) }

        sneezeManager = SneezeManager(this)
        packetHandler = PacketHandler(this)

        commandManager = CommandManager(this)
        for (commandType in PluginCommandType.entries) {
            plugin.getCommand(commandType.id)?.apply {
                setExecutor(commandManager)
                tabCompleter = commandManager
            }
        }

        plugin.server.pluginManager.registerEvents(EventHandler(this), plugin)

        sitManager.startArmorStandChecker()
        crawlManager.startBarrierUpdater()
        layManager.startArmorStandChecker()
        packetHandler.register()
    }

    fun shutdown() {
        if (this::utils.isInitialized) {
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
            utils.stopAllGlows()
            utils.stopAllPiss()
            utils.stopAllDisguises()
            utils.stopAllCustomEffects()
            utils.stopAllGrabs()
            utils.stopAllJars()
            utils.stopAllSlaps()
        }
        if (this::messageManager.isInitialized) {
            messageManager.clearAllTasks()
        }
        if (this::tickScheduler.isInitialized) {
            tickScheduler.shutdown()
        }
        if (this::packetHandler.isInitialized) {
            packetHandler.unregister()
        }
    }
}
