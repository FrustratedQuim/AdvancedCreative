package com.ratger.acreative.commands.edit

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.entity.Player

class EditService(
    private val targetResolver: EditTargetResolver,
    private val validationService: EditValidationService,
    private val showService: EditShowService,
    private val metaActionsApplier: EditMetaActionsApplier,
    private val experimentalComponentsService: EditExperimentalComponentsService,
    private val headProfileService: EditHeadProfileService
) {
    private val mini = MiniMessage.miniMessage()

    fun execute(player: Player, action: EditAction): EditResult {
        val context = targetResolver.resolve(player) ?: return EditResult(false, emptyList())
        if (action is EditAction.Show) return EditResult(true, showService.render(context))

        if (action is EditAction.Reset && action.scope.startsWith("unsupported:")) {
            val key = action.scope.removePrefix("unsupported:")
            return EditResult(
                false,
                listOf(mini.deserialize("<yellow>Ветка <white>$key<yellow> пока unsupported на стабильном API сборки.")),
                warning = true
            )
        }

        validationService.validate(action, context, player)?.let { return it }

        if (action is EditAction.HeadSetFromName) {
            return headProfileService.applyFromNameAsync(player.uniqueId, action.name)
        }

        val initialItem = context.item.clone()
        val outcome = apply(player, action, initialItem)
        if (!outcome.result.ok) return outcome.result

        targetResolver.save(player, outcome.item)
        return outcome.result
    }

    private fun apply(player: Player, action: EditAction, item: org.bukkit.inventory.ItemStack): ApplyOutcome {
        if (action is EditAction.Reset) {
            return when (action.scope) {
                "all" -> {
                    EditItemStackReplacementSupport.resetAll(item)
                    ApplyOutcome(item, success("<green>Состояние предмета очищено (reset all)."))
                }
                else -> ApplyOutcome(item, failure("<red>Использование: /edit reset <all>"))
            }
        }

        if (action is EditAction.SetItemId) {
            val replaced = EditItemStackReplacementSupport.replaceItemId(item, action.material)
            return ApplyOutcome(replaced, success("<green>ID предмета изменён на <white>${action.material.key.asString()}</white>."))
        }

        if (experimentalComponentsService.supports(action)) {
            return ApplyOutcome(item, experimentalComponentsService.apply(player, action, item))
        }

        if (action is EditAction.LockSetFromOffhand || action is EditAction.LockClear) {
            return ApplyOutcome(item, EditLockActionsHelper.apply(player, action, item))
        }

        val metaResult = metaActionsApplier.apply(action, player, item)
            ?: failure("<red>Ветка не поддерживается для item meta")
        return ApplyOutcome(item, metaResult)
    }

    private fun success(message: String): EditResult = EditResult(true, listOf(mini.deserialize(message)))

    private fun failure(message: String): EditResult = EditResult(false, listOf(mini.deserialize(message)))

    private data class ApplyOutcome(
        val item: org.bukkit.inventory.ItemStack,
        val result: EditResult
    )
}
