package com.ratger.acreative.commands.freeze

import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.MessageKey
import org.bukkit.Bukkit
import org.bukkit.entity.Player

internal class FreezeTargetResolver(private val hooker: FunctionHooker) {

    data class ResolutionResult(val target: Player, val initiator: Player?)

    fun resolve(initiator: Player, targetName: String?): ResolutionResult? {
        if (targetName == null || !initiator.hasPermission("advancedcreative.freeze.other")) {
            return ResolutionResult(target = initiator, initiator = initiator)
        }

        val target = Bukkit.getPlayer(targetName)
        if (target == null) {
            hooker.messageManager.sendChat(initiator, MessageKey.ERROR_UNKNOWN_PLAYER)
            return null
        }

        return ResolutionResult(target = target, initiator = initiator)
    }
}
