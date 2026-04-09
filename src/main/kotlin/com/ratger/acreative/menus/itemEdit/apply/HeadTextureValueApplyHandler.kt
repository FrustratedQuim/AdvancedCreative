package com.ratger.acreative.menus.itemEdit.apply

import com.ratger.acreative.itemedit.head.HeadTextureMutationSupport
import com.ratger.acreative.itemedit.head.HeadTextureSource
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.entity.Player

class HeadTextureValueApplyHandler(
    private val mutationSupport: HeadTextureMutationSupport
) : EditorApplyHandler {
    override val kind: EditorApplyKind = EditorApplyKind.HEAD_TEXTURE_VALUE
    private val mini = MiniMessage.miniMessage()

    override fun apply(player: Player, session: ItemEditSession, args: Array<out String>): ApplyExecutionResult {
        if (args.size != 1) return ApplyExecutionResult.InvalidValue
        return when (val result = mutationSupport.applyFromTextureValue(session.editableItem, args[0])) {
            is HeadTextureMutationSupport.MutationResult.Failure -> {
                player.sendMessage(mini.deserialize("<red>${result.reason}"))
                ApplyExecutionResult.InvalidValue
            }
            HeadTextureMutationSupport.MutationResult.Success -> {
                session.headTextureSource = HeadTextureSource.TEXTURE_VALUE
                session.headTextureVirtualValue = mutationSupport.texturesValue(session.editableItem)?.takeUnless { it.isBlank() }
                ApplyExecutionResult.Success
            }
        }
    }

    override fun suggestions(args: Array<out String>): List<String> = emptyList()
}
