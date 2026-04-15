package com.ratger.acreative.menus.edit.apply.effects

import com.ratger.acreative.commands.edit.EditTargetResolver
import com.ratger.acreative.menus.edit.api.ItemAction
import com.ratger.acreative.menus.edit.api.ItemContext
import com.ratger.acreative.menus.edit.apply.core.ApplyExecutionResult
import com.ratger.acreative.menus.edit.apply.core.EditorApplyHandler
import com.ratger.acreative.menus.edit.apply.core.EditorApplyKind
import com.ratger.acreative.menus.edit.usecooldown.UseCooldownSupport
import com.ratger.acreative.menus.edit.validation.ValidationService
import com.ratger.acreative.menus.edit.ItemEditSession
import java.util.Locale
import kotlin.random.Random
import net.kyori.adventure.key.Key
import org.bukkit.entity.Player

class UseCooldownGroupApplyHandler(
    private val validationService: ValidationService,
    private val targetResolver: EditTargetResolver
) : EditorApplyHandler {
    override val kind: EditorApplyKind = EditorApplyKind.USE_COOLDOWN_GROUP

    private val randomCharset = "abcdefghijklmnopqrstuvwxyz0123456789"

    override fun apply(player: Player, session: ItemEditSession, args: Array<out String>): ApplyExecutionResult {
        val rawInput = args.joinToString(" ").trim()
        if (rawInput.isEmpty()) return ApplyExecutionResult.InvalidValue

        val normalized = if (rawInput.equals("rand", ignoreCase = true)) {
            (1..8).map { randomCharset[Random.nextInt(randomCharset.length)] }.joinToString("")
        } else {
            rawInput.lowercase(Locale.ROOT).replace("\\s+".toRegex(), "_")
        }

        if (!Key.parseable(normalized)) return ApplyExecutionResult.InvalidValue
        val currentSeconds = UseCooldownSupport.seconds(session.editableItem) ?: 1.0f
        val group = Key.key(normalized)

        val action = ItemAction.SetUseCooldown(currentSeconds, group)
        val context = ItemContext(session.editableItem, targetResolver.snapshot(session.editableItem))
        if (!validationService.validate(action, context, player)) {
            return ApplyExecutionResult.InvalidValue
        }

        if (!UseCooldownSupport.has(session.editableItem)) {
            UseCooldownSupport.setSeconds(session.editableItem, currentSeconds)
        }
        UseCooldownSupport.setGroup(session.editableItem, group)
        return ApplyExecutionResult.Success
    }

    override fun suggestions(args: Array<out String>): List<String> {
        if (args.size != 1) return emptyList()
        return listOf("rand").filter { it.startsWith(args[0], ignoreCase = true) }
    }
}
