package com.ratger.acreative.commands.admin.npc

import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.MessageKey
import org.bukkit.entity.Player

class NpcCommandService(
    private val hooker: FunctionHooker,
    private val npcManager: NpcManager
) {
    fun handle(player: Player, args: List<String>) {
        when (args.firstOrNull()?.lowercase()) {
            "create" -> handleCreate(player, args.drop(1))
            "remove" -> handleRemove(player, args.drop(1))
            "skin" -> handleSkin(player, args.drop(1))
            "nick" -> handleNick(player, args.drop(1))
            "equip" -> handleEquip(player, args.drop(1))
            "tp" -> handleTeleport(player, args.drop(1))
            "tpohere" -> handleTeleportHere(player, args.drop(1))
            else -> hooker.messageManager.sendChat(player, MessageKey.ERROR_UNKNOWN_ARGUMENT)
        }
    }

    fun tabComplete(player: Player, args: List<String>): List<String> {
        return when (args.size) {
            1 -> listOf("create", "remove", "skin", "nick", "equip", "tp", "tpohere")
                .filter { it.startsWith(args[0], ignoreCase = true) }

            2 -> when {
                args[0].equals("create", ignoreCase = true) -> emptyList()
                args[0].equals("remove", ignoreCase = true) ||
                args[0].equals("skin", ignoreCase = true) ||
                    args[0].equals("nick", ignoreCase = true) ||
                    args[0].equals("equip", ignoreCase = true) ||
                    args[0].equals("tp", ignoreCase = true) ||
                    args[0].equals("tpohere", ignoreCase = true) ->
                    npcManager.profileSuggestions(args[1])
                else -> emptyList()
            }

            3 -> when {
                args[0].equals("skin", ignoreCase = true) -> npcManager.onlinePlayerSuggestions(args[2])
                else -> emptyList()
            }

            else -> emptyList()
        }
    }

    private fun handleRemove(player: Player, args: List<String>) {
        val profileName = args.getOrNull(0)
        if (profileName.isNullOrBlank()) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_UNKNOWN_VALUE)
            return
        }
        if (args.size > 1) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_UNKNOWN_ARGUMENT)
            return
        }

        when (val result = npcManager.removeProfile(profileName)) {
            NpcManager.ProfileResult.ProfileNotFound -> hooker.messageManager.sendChat(
                player,
                MessageKey.NPC_PROFILE_NOT_FOUND,
                mapOf("name" to profileName)
            )
            is NpcManager.ProfileResult.Success -> hooker.messageManager.sendChat(
                player,
                MessageKey.NPC_PROFILE_REMOVED,
                mapOf("name" to result.profile.name)
            )
        }
    }

    private fun handleCreate(player: Player, args: List<String>) {
        val profileName = args.getOrNull(0)
        if (profileName.isNullOrBlank()) {
            hooker.messageManager.sendChat(player, MessageKey.EDIT_APPLY_INVALID_VALUE)
            return
        }
        if (args.size > 1) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_UNKNOWN_ARGUMENT)
            return
        }

        when (val result = npcManager.createProfile(player, profileName)) {
            NpcManager.CreateResult.InvalidName -> hooker.messageManager.sendChat(player, MessageKey.EDIT_APPLY_INVALID_VALUE)
            NpcManager.CreateResult.AlreadyExists -> hooker.messageManager.sendChat(
                player,
                MessageKey.NPC_PROFILE_EXISTS,
                mapOf("name" to profileName)
            )
            is NpcManager.CreateResult.Success -> hooker.messageManager.sendChat(
                player,
                MessageKey.NPC_PROFILE_CREATED,
                mapOf("name" to result.profile.name)
            )
        }
    }

    private fun handleSkin(player: Player, args: List<String>) {
        val profileName = args.getOrNull(0)
        val skinSource = args.getOrNull(1)
        if (profileName.isNullOrBlank() || skinSource.isNullOrBlank()) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_UNKNOWN_VALUE)
            return
        }
        if (args.size > 2) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_UNKNOWN_ARGUMENT)
            return
        }

        npcManager.applySkinAsync(profileName, skinSource)
            .whenComplete { result, error ->
                val finalResult = if (error != null || result == null) {
                    hooker.plugin.logger.warning("Failed to resolve npc skin for $profileName from '$skinSource': ${error?.message}")
                    NpcManager.SkinApplyResult.UnknownPlayer
                } else {
                    result
                }
                runSync {
                    if (!player.isOnline) {
                        return@runSync
                    }
                    when (finalResult) {
                        NpcManager.SkinApplyResult.ProfileNotFound -> hooker.messageManager.sendChat(
                            player,
                            MessageKey.NPC_PROFILE_NOT_FOUND,
                            mapOf("name" to profileName)
                        )
                        NpcManager.SkinApplyResult.UnknownPlayer -> hooker.messageManager.sendChat(player, MessageKey.ERROR_UNKNOWN_PLAYER)
                        is NpcManager.SkinApplyResult.Success -> hooker.messageManager.sendChat(
                            player,
                            MessageKey.NPC_SKIN_UPDATED,
                            mapOf("name" to finalResult.profile.name)
                        )
                    }
                }
            }
    }

    private fun handleNick(player: Player, args: List<String>) {
        val profileName = args.getOrNull(0)
        if (profileName.isNullOrBlank()) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_UNKNOWN_VALUE)
            return
        }
        val nickInput = args.drop(1).joinToString(" ").replace("\\n", "\n")
        if (nickInput.isBlank()) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_UNKNOWN_VALUE)
            return
        }

        when (val result = npcManager.updateNick(profileName, nickInput)) {
            NpcManager.ProfileResult.ProfileNotFound -> hooker.messageManager.sendChat(
                player,
                MessageKey.NPC_PROFILE_NOT_FOUND,
                mapOf("name" to profileName)
            )
            is NpcManager.ProfileResult.Success -> hooker.messageManager.sendChat(
                player,
                MessageKey.NPC_NICK_UPDATED,
                mapOf("name" to result.profile.name)
            )
        }
    }

    private fun handleEquip(player: Player, args: List<String>) {
        val profileName = args.getOrNull(0)
        if (profileName.isNullOrBlank()) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_UNKNOWN_VALUE)
            return
        }
        if (args.size > 1) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_UNKNOWN_ARGUMENT)
            return
        }

        when (val result = npcManager.updateEquipment(player, profileName)) {
            NpcManager.ProfileResult.ProfileNotFound -> hooker.messageManager.sendChat(
                player,
                MessageKey.NPC_PROFILE_NOT_FOUND,
                mapOf("name" to profileName)
            )
            is NpcManager.ProfileResult.Success -> hooker.messageManager.sendChat(
                player,
                MessageKey.NPC_EQUIPMENT_UPDATED,
                mapOf("name" to result.profile.name)
            )
        }
    }

    private fun handleTeleport(player: Player, args: List<String>) {
        val profileName = args.getOrNull(0)
        if (profileName.isNullOrBlank()) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_UNKNOWN_VALUE)
            return
        }
        if (args.size > 1) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_UNKNOWN_ARGUMENT)
            return
        }

        when (val result = npcManager.teleportPlayerToProfile(player, profileName)) {
            NpcManager.TeleportResult.ProfileNotFound -> hooker.messageManager.sendChat(
                player,
                MessageKey.NPC_PROFILE_NOT_FOUND,
                mapOf("name" to profileName)
            )
            NpcManager.TeleportResult.LocationUnavailable -> hooker.messageManager.sendChat(player, MessageKey.ERROR_UNKNOWN_VALUE)
            is NpcManager.TeleportResult.Success -> hooker.messageManager.sendChat(
                player,
                MessageKey.NPC_TELEPORTED,
                mapOf("name" to result.profile.name)
            )
        }
    }

    private fun handleTeleportHere(player: Player, args: List<String>) {
        val profileName = args.getOrNull(0)
        if (profileName.isNullOrBlank()) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_UNKNOWN_VALUE)
            return
        }
        if (args.size > 1) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_UNKNOWN_ARGUMENT)
            return
        }

        when (val result = npcManager.updatePositionFromPlayer(player, profileName)) {
            NpcManager.ProfileResult.ProfileNotFound -> hooker.messageManager.sendChat(
                player,
                MessageKey.NPC_PROFILE_NOT_FOUND,
                mapOf("name" to profileName)
            )
            is NpcManager.ProfileResult.Success -> hooker.messageManager.sendChat(
                player,
                MessageKey.NPC_POSITION_UPDATED,
                mapOf("name" to result.profile.name)
            )
        }
    }

    private fun runSync(action: () -> Unit) {
        if (org.bukkit.Bukkit.isPrimaryThread()) {
            action()
        } else {
            org.bukkit.Bukkit.getScheduler().runTask(hooker.plugin, Runnable { action() })
        }
    }
}
