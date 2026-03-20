package com.ratger.acreative.commands

enum class PluginCommandType(val id: String, val cooldownKey: String = id) {
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
    ITEMDB("itemdb");

    companion object {
        private val byId = entries.associateBy { it.id }

        fun fromId(id: String): PluginCommandType? = byId[id.lowercase()]
    }
}
