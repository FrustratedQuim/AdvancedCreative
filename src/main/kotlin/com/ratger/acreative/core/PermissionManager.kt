package com.ratger.acreative.core

import com.ratger.acreative.commands.PluginCommandType
import org.bukkit.entity.Player

class PermissionManager(private val hooker: FunctionHooker) {

    data class Role(val key: String, val display: String)

    private val permissionToRole = mutableMapOf<String, String>()
    private val roles = mutableMapOf<String, Role>()

    init {
        reload()
    }

    fun reload() {
        permissionToRole.clear()
        roles.clear()

        val root = hooker.configManager.config
        val rolesSec = root.getConfigurationSection("roles")
        rolesSec?.let { sec ->
            sec.getKeys(false).forEach { roleKey ->
                val display = sec.getString("$roleKey.display") ?: NONE_DISPLAY
                val normalizedRoleKey = roleKey.lowercase()
                roles[normalizedRoleKey] = Role(normalizedRoleKey, display)
                sec.getStringList("$roleKey.permissions").forEach { permission ->
                    permissionToRole[permission.lowercase()] = normalizedRoleKey
                }
            }
        }
    }

    fun getRequiredRole(permissionOrCommand: String): Role? {
        val permissionNode = normalizePermissionKey(permissionOrCommand)
        val roleKey = permissionToRole[permissionNode] ?: return null
        return roles[roleKey]
    }

    fun sendPermissionDenied(player: Player, permissionOrCommand: String) {
        val role = getRequiredRole(permissionOrCommand)
        if (role != null && !role.display.equals(NONE_DISPLAY, ignoreCase = true)) {
            hooker.messageManager.sendChat(
                player,
                MessageKey.PERMISSION_REQUIRED,
                variables = mapOf("role_display" to role.display)
            )
        } else {
            hooker.messageManager.sendChat(player, MessageKey.PERMISSION_UNKNOWN)
        }
    }

    fun getPermissionNodeForCommand(command: String): String {
        return PluginCommandType.fromId(command)?.permissionNode ?: "advancedcreative.$command"
    }

    private fun normalizePermissionKey(permissionOrCommand: String): String {
        val normalized = permissionOrCommand.lowercase()
        return if ('.' in normalized) {
            if (normalized.startsWith("advancedcreative.")) normalized else "advancedcreative.$normalized"
        } else {
            getPermissionNodeForCommand(normalized).lowercase()
        }
    }

    private companion object {
        const val NONE_DISPLAY = "none"
    }
}
