package com.ratger.acreative.commands.disguise.model

data class DisguiseFlags(
    val showSelf: Boolean,
    val showNick: Boolean,
    val requiresNickPermission: Boolean
) {
    companion object {
        val KNOWN_FLAGS = setOf("-self", "-noself", "-withnick", "-nonick")

        fun parse(flags: List<String>): DisguiseFlags {
            var showSelf: Boolean? = null
            var showNick: Boolean? = null
            var requiresNickPermission = false

            flags.forEach { rawFlag ->
                when (rawFlag.lowercase()) {
                    "-self" -> if (showSelf == null) showSelf = true
                    "-noself" -> if (showSelf == null) showSelf = false
                    "-withnick" -> {
                        if (showNick == null) showNick = true
                        requiresNickPermission = true
                    }

                    "-nonick" -> {
                        if (showNick == null) showNick = false
                        requiresNickPermission = true
                    }
                }
            }

            return DisguiseFlags(
                showSelf = showSelf ?: true,
                showNick = showNick ?: true,
                requiresNickPermission = requiresNickPermission
            )
        }

        fun isKnown(rawValue: String): Boolean {
            return rawValue.lowercase() in KNOWN_FLAGS
        }
    }
}
