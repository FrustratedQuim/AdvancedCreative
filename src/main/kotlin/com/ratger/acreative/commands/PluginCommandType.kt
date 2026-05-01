package com.ratger.acreative.commands

import com.ratger.acreative.core.ManagedSystem

enum class PluginCommandType(
    val id: String,
    val cooldownKey: String = id,
    val permissionNode: String? = "advancedcreative.$id",
    val managedSystem: ManagedSystem? = null
) {
    AHELP("ahelp", "help"),
    SIT("sit"),
    LAY("lay"),
    CRAWL("crawl"),
    HIDE("hide"),
    SNEEZE("sneeze"),
    GLIDE("glide"),
    GRAVITY("gravity"),
    RESIZE("resize"),
    STRENGTH("strength"),
    HEALTH("health"),
    FREEZE("freeze"),
    GLOW("glow"),
    SPIT("spit"),
    PISS("piss"),
    DISGUISE("disguise"),
    EFFECTS("effects"),
    PAINT("paint", managedSystem = ManagedSystem.PAINT),
    JAR("jar"),
    GRAB("grab"),
    SLAP("slap"),
    SITHEAD("sithead"),
    ITEMDB("itemdb"),
    BANNER("banner", permissionNode = "advancedcreative.decorationbanners", managedSystem = ManagedSystem.DECORATION_BANNERS),
    DECORATIONBANNERS("decorationbanners", managedSystem = ManagedSystem.DECORATION_BANNERS),
    MYFLAGS("myflags", permissionNode = "advancedcreative.decorationbanners", managedSystem = ManagedSystem.DECORATION_BANNERS),
    DECORATIONHEADS("decorationheads", managedSystem = ManagedSystem.DECORATION_HEADS),
    BANNEREDIT("banneredit", permissionNode = "advancedcreative.decorationbanners", managedSystem = ManagedSystem.DECORATION_BANNERS),
    EDIT("edit", managedSystem = ManagedSystem.EDIT),
    APPLY("apply", permissionNode = null),
    ACREATIVE("acreative");

    companion object {
        private val byId = entries.associateBy { it.id }

        fun fromId(id: String): PluginCommandType? = byId[id.lowercase()]
    }
}
