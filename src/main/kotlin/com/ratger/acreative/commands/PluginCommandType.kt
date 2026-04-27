package com.ratger.acreative.commands

enum class PluginCommandType(
    val id: String,
    val cooldownKey: String = id,
    val permissionNode: String? = "advancedcreative.$id"
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
    JAR("jar"),
    GRAB("grab"),
    SLAP("slap"),
    SITHEAD("sithead"),
    ITEMDB("itemdb"),
    BANNER("banner", permissionNode = "advancedcreative.decorationbanners"),
    DECORATIONBANNERS("decorationbanners"),
    MYFLAGS("myflags", permissionNode = "advancedcreative.decorationbanners"),
    DECORATIONHEADS("decorationheads"),
    BANNEREDIT("banneredit", permissionNode = "advancedcreative.decorationbanners"),
    EDIT("edit"),
    APPLY("apply", permissionNode = null),
    ACREATIVE("acreative");

    companion object {
        private val byId = entries.associateBy { it.id }

        fun fromId(id: String): PluginCommandType? = byId[id.lowercase()]
    }
}
