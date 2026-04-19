package com.ratger.acreative.core

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.lang.reflect.Method

class AccountLinkRequirementService(
    private val hooker: FunctionHooker
) {
    @Volatile
    private var cachedUserManager: Any? = null

    @Volatile
    private var userManagerLookupAttempted = false

    fun hasRequiredLink(player: Player): Boolean {
        val user = resolveUser(player) ?: return true
        return runCatching {
            val method = user.javaClass.methods.firstOrNull {
                it.name == "hasActiveBotLink" && it.parameterCount == 0
            } ?: return true
            (method.invoke(user) as? Boolean) ?: true
        }.getOrElse {
            hooker.plugin.logger.warning("Failed to check account link for ${player.name}: ${it.message}")
            true
        }
    }

    fun sendLinkRequiredMessage(player: Player) {
        hooker.messageManager.sendChat(player, MessageKey.ACCOUNT_LINK_REQUIRED)
    }

    private fun resolveUser(player: Player): Any? {
        val manager = resolveUserManager() ?: return null
        return invokeUserResolver(manager, player)
    }

    private fun resolveUserManager(): Any? {
        if (userManagerLookupAttempted) {
            return cachedUserManager
        }

        synchronized(this) {
            if (userManagerLookupAttempted) {
                return cachedUserManager
            }

            val candidate = Bukkit.getServicesManager().knownServices.firstNotNullOfOrNull { serviceClass ->
                Bukkit.getServicesManager().load(serviceClass)?.takeIf { provider ->
                    provider.javaClass.methods.any { it.name == "getUser" && it.parameterCount == 1 }
                }
            }

            cachedUserManager = candidate
            userManagerLookupAttempted = true

            if (candidate == null) {
                hooker.plugin.logger.warning("Account link check is disabled: user manager service not found.")
            }

            return candidate
        }
    }

    private fun invokeUserResolver(manager: Any, player: Player): Any? {
        val methods = manager.javaClass.methods.filter { it.name == "getUser" && it.parameterCount == 1 }
        methods.forEach { method ->
            invokeMethod(method, manager, player)?.let { return it }
            invokeMethod(method, manager, player.uniqueId)?.let { return it }
            invokeMethod(method, manager, player.name)?.let { return it }
        }
        return null
    }

    private fun invokeMethod(method: Method, target: Any, argument: Any): Any? {
        val parameterType = method.parameterTypes.firstOrNull() ?: return null
        if (!parameterType.isAssignableFrom(argument.javaClass)) {
            return null
        }
        return runCatching { method.invoke(target, argument) }.getOrNull()
    }
}
