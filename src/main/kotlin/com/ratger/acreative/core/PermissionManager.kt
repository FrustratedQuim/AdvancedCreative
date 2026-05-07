package com.ratger.acreative.core

import com.ratger.acreative.commands.PluginCommandType
import org.bukkit.entity.Player

class PermissionManager(private val hooker: FunctionHooker) {

    data class Role(
        val key: String,
        val display: String,
        val prefix: String,
        val rankPermissions: List<String>,
        val permissions: List<String>
    )

    private val permissionToRole = mutableMapOf<String, String>()
    private val roles = mutableMapOf<String, Role>()
    private val roleOrder = mutableListOf<String>()

    init {
        reload()
    }

    fun reload() {
        permissionToRole.clear()
        roles.clear()
        roleOrder.clear()

        val root = hooker.configManager.config
        val rolesSec = root.getConfigurationSection("roles")
        rolesSec?.let { sec ->
            sec.getKeys(false).forEach { roleKey ->
                val rawDisplay = sec.getString("$roleKey.display") ?: NONE_DISPLAY
                val prefix = sec.getString("$roleKey.prefix")
                    ?: rawDisplay.takeIf { it.contains('<') }
                    ?: ""
                val display = if (rawDisplay.contains('<')) stripMiniMessage(rawDisplay) else rawDisplay
                val rankPermissions = sec.getStringList("$roleKey.rank-permissions")
                val permissions = sec.getStringList("$roleKey.permissions")
                val normalizedRoleKey = roleKey.lowercase()
                roles[normalizedRoleKey] = Role(
                    key = normalizedRoleKey,
                    display = display,
                    prefix = prefix,
                    rankPermissions = rankPermissions,
                    permissions = permissions
                )
                roleOrder += normalizedRoleKey
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

    fun getRole(roleKey: String): Role? = roles[roleKey.lowercase()]

    fun orderedRoles(): List<Role> = roleOrder.mapNotNull { roles[it] }

    fun defaultRoleKey(): String = roleOrder.firstOrNull() ?: "player"

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
        return PluginCommandType.fromId(command)?.permissionNode ?: "acreative.$command"
    }

    private fun normalizePermissionKey(permissionOrCommand: String): String {
        val normalized = permissionOrCommand.lowercase()
        return if ('.' in normalized) {
            if (normalized.startsWith("acreative.")) normalized else "acreative.$normalized"
        } else {
            getPermissionNodeForCommand(normalized).lowercase()
        }
    }

    private fun stripMiniMessage(input: String): String {
        return input
            .replace(Regex("<[^>]+>"), "")
            .trim()
    }

    private companion object {
        const val NONE_DISPLAY = "none"
    }
}

