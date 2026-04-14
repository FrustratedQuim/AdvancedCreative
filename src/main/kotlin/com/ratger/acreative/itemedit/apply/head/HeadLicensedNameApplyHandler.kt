package com.ratger.acreative.itemedit.apply.head

import com.ratger.acreative.itemedit.apply.core.ApplyExecutionResult
import com.ratger.acreative.itemedit.apply.core.EditorApplyHandler
import com.ratger.acreative.itemedit.apply.core.EditorApplyKind
import com.ratger.acreative.itemedit.head.HeadProfileService
import com.ratger.acreative.itemedit.head.HeadTextureMutationSupport
import com.ratger.acreative.itemedit.head.HeadTextureSource
import com.ratger.acreative.menus.edit.ItemEditSession
import com.ratger.acreative.menus.edit.ItemEditSessionManager
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class HeadLicensedNameApplyHandler(
    private val plugin: JavaPlugin,
    private val sessionManager: ItemEditSessionManager,
    private val headProfileService: HeadProfileService,
    private val mutationSupport: HeadTextureMutationSupport,
    private val reopenHeadTexturePage: (Player) -> Unit
) : EditorApplyHandler {
    override val kind: EditorApplyKind = EditorApplyKind.HEAD_LICENSED_NAME

    override fun apply(player: Player, session: ItemEditSession, args: Array<out String>): ApplyExecutionResult {
        if (args.size != 1) return ApplyExecutionResult.InvalidValue
        val name = args[0]

        val operationToken = session.headTextureOpSequence + 1
        session.headTextureOpSequence = operationToken
        session.headTextureLoadingToken = operationToken

        headProfileService.lookupLicensedProfileAsync(name).whenComplete { payload, error ->
            Bukkit.getScheduler().runTask(plugin, Runnable {
                val onlinePlayer = Bukkit.getPlayer(player.uniqueId) ?: return@Runnable
                val onlineSession = sessionManager.getSession(onlinePlayer) ?: return@Runnable
                val isSameOperation = onlineSession.headTextureLoadingToken == operationToken
                val isStillInSection = onlineSession.headTextureSectionActive
                if (!isSameOperation || !isStillInSection) {
                    if (isSameOperation) {
                        onlineSession.headTextureLoadingToken = null
                    }
                    return@Runnable
                }

                if (error == null && payload != null) {
                    val applyResult = mutationSupport.applyFromLicensedPayload(onlineSession.editableItem, payload)
                    if (applyResult is HeadTextureMutationSupport.MutationResult.Success) {
                        onlineSession.headTextureSource = HeadTextureSource.LICENSED_NAME
                        onlineSession.headTextureVirtualValue = mutationSupport.texturesValue(onlineSession.editableItem)?.takeUnless { it.isBlank() }
                    }
                }
                onlineSession.headTextureLoadingToken = null
                reopenHeadTexturePage(onlinePlayer)
            })
        }
        return ApplyExecutionResult.Success
    }

    override fun suggestions(args: Array<out String>): List<String> = emptyList()
}
