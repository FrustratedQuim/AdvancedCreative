package com.ratger.acreative.commands.admin.npc

import com.ratger.acreative.commands.AhelpPageService
import com.ratger.acreative.core.FunctionHooker
import java.util.Locale
import java.util.UUID

class NpcBuiltInInteractionHandler(
    private val hooker: FunctionHooker
) : NpcInteractionHandler {
    private val ahelpPageService = AhelpPageService(hooker)
    private val cooldownsByPlayerAndProfile = mutableMapOf<CooldownKey, Long>()

    override fun handle(context: NpcInteractionContext) {
        val profileKey = context.profile.name.lowercase(Locale.ROOT)
        if (profileKey != COMMAND_AHELP_PROFILE_KEY) {
            return
        }
        if (!acquireCooldown(context.player.uniqueId, context.profile.name)) {
            return
        }

        sendFirstAhelpPage(context)
    }

    private fun acquireCooldown(playerId: UUID, profileName: String): Boolean {
        val key = CooldownKey(playerId = playerId, profileKey = profileName.trim().lowercase(Locale.ROOT))
        val now = System.currentTimeMillis()
        val expiresAt = cooldownsByPlayerAndProfile[key]
        if (expiresAt != null && expiresAt > now) {
            return false
        }

        cooldownsByPlayerAndProfile.entries.removeIf { it.value <= now }
        cooldownsByPlayerAndProfile[key] = now + INTERACTION_COOLDOWN_MS
        return true
    }

    private fun sendFirstAhelpPage(context: NpcInteractionContext) {
        hooker.messageManager.sendMiniMessageChat(
            context.player,
            ahelpPageService.renderFor(context.player, 1)
        )
    }

    private data class CooldownKey(
        val playerId: UUID,
        val profileKey: String
    )

    private companion object {
        const val INTERACTION_COOLDOWN_MS = 1_000L
        const val COMMAND_AHELP_PROFILE_KEY = "command_ahelp"
    }
}
