package com.ratger.acreative.menus.itemEdit.apply

import com.ratger.acreative.itemedit.head.HeadTextureMutationSupport
import com.ratger.acreative.itemedit.head.HeadTextureSource
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import org.bukkit.entity.Player

class HeadOnlineNameApplyHandler(
    private val mutationSupport: HeadTextureMutationSupport
) : EditorApplyHandler {
    override val kind: EditorApplyKind = EditorApplyKind.HEAD_ONLINE_NAME
    override fun apply(player: Player, session: ItemEditSession, args: Array<out String>): ApplyExecutionResult {
        if (args.size != 1) return ApplyExecutionResult.InvalidValue
        return when (mutationSupport.applyFromOnlinePlayer(session.editableItem, args[0])) {
            is HeadTextureMutationSupport.MutationResult.Failure -> ApplyExecutionResult.InvalidValue
            HeadTextureMutationSupport.MutationResult.Success -> {
                session.headTextureSource = HeadTextureSource.ONLINE_PLAYER
                ApplyExecutionResult.Success
            }
        }
    }

    override fun suggestions(args: Array<out String>): List<String> {
        if (args.size != 1) return emptyList()
        val prefix = args[0]
        return org.bukkit.Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(prefix, ignoreCase = true) }
    }
}
