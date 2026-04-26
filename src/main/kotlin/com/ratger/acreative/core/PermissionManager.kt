package com.ratger.acreative.core

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player

class PermissionManager(private val hooker: FunctionHooker) {

    data class Role(val key: String, val display: String)

    private val commandToRole = mutableMapOf<String, String>()
    private val commandToNode = mutableMapOf<String, String>()
    private val roles = mutableMapOf<String, Role>()
    private var messageKey: MessageKey = MessageKey.PERMISSION_REQUIRED

    init {
        reload()
    }

    fun reload() {
        commandToRole.clear()
        commandToNode.clear()
        roles.clear()

        val root = hooker.configManager.config
        val permissions = root.getConfigurationSection("permissions")

        messageKey = MessageKey.PERMISSION_REQUIRED

        val rolesSec = permissions?.getConfigurationSection("roles")
        rolesSec?.let { sec ->
            sec.getKeys(false).forEach { roleKey ->
                val display = sec.getString("$roleKey.display") ?: roleKey
                roles[roleKey.lowercase()] = Role(roleKey.lowercase(), display)
            }
        }

        val commandsSec = permissions?.getConfigurationSection("commands")
        commandsSec?.let { sec ->
            loadCommandPermissions(sec)
        }
    }

    private fun loadCommandPermissions(section: ConfigurationSection, prefix: String = "") {
        section.getKeys(false).forEach { key ->
            val commandKey = if (prefix.isEmpty()) key else "$prefix.$key"
            when (val raw = section.get(key)) {
                is String -> {
                    commandToRole[commandKey.lowercase()] = raw.lowercase()
                }
                is ConfigurationSection -> {
                    val role = raw.getString("role")
                    val node = raw.getString("node")
                    if (role != null || node != null) {
                        role?.let { commandToRole[commandKey.lowercase()] = it.lowercase() }
                        node?.let { commandToNode[commandKey.lowercase()] = it }
                    } else {
                        loadCommandPermissions(raw, commandKey)
                    }
                }
            }
        }
    }

    fun getRequiredRoleForCommand(command: String): Role? {
        val roleKey = commandToRole[command.lowercase()] ?: return null
        return roles[roleKey]
    }

    fun sendPermissionDenied(player: Player, command: String) {
        val role = getRequiredRoleForCommand(command)
        if (role != null) {
            hooker.messageManager.sendChat(
                player,
                messageKey,
                variables = mapOf("role_display" to role.display)
            )
        } else {
            hooker.messageManager.sendChat(player, MessageKey.PERMISSION_UNKNOWN)
        }
    }

    fun getPermissionNodeForCommand(command: String): String {
        return commandToNode[command.lowercase()] ?: "advancedcreative.$command"
    }
}
