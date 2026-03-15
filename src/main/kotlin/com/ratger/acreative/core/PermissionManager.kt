package com.ratger.acreative.core

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
            sec.getKeys(false).forEach { command ->
                val fullPath = "permissions.commands.$command"
                val raw = hooker.configManager.config.get(fullPath)
                when (raw) {
                    is String -> {
                        commandToRole[command.lowercase()] = raw.lowercase()
                    }
                    else -> {
                        hooker.configManager.config.getString("$fullPath.role")?.let {
                            commandToRole[command.lowercase()] = it.lowercase()
                        }
                        hooker.configManager.config.getString("$fullPath.node")?.let {
                            commandToNode[command.lowercase()] = it
                        }
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
