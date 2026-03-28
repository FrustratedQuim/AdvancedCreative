package com.ratger.acreative.itemedit.core

import com.ratger.acreative.commands.edit.EditTargetResolver
import com.ratger.acreative.itemedit.api.ItemAction
import com.ratger.acreative.itemedit.api.ItemContext
import com.ratger.acreative.itemedit.api.ItemResult
import com.ratger.acreative.itemedit.container.LockActionsHelper
import com.ratger.acreative.itemedit.experimental.ComponentsService
import com.ratger.acreative.itemedit.head.HeadProfileService
import com.ratger.acreative.itemedit.invisibility.FrameInvisibilitySupport
import com.ratger.acreative.itemedit.meta.ItemStackReplacementSupport
import com.ratger.acreative.itemedit.meta.MetaActionsApplier
import com.ratger.acreative.itemedit.show.ShowService
import com.ratger.acreative.itemedit.validation.ValidationService
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.entity.Player

class ItemEditingService(
    private val targetResolver: EditTargetResolver,
    private val validationService: ValidationService,
    private val showService: ShowService,
    private val metaActionsApplier: MetaActionsApplier,
    private val componentsService: ComponentsService,
    private val headProfileService: HeadProfileService
) {
    private val mini = MiniMessage.miniMessage()

    fun show(context: ItemContext): ItemResult = ItemResult(true, showService.render(context))

    fun execute(player: Player, action: ItemAction): ItemResult {
        val context = targetResolver.resolve(player) ?: return ItemResult(false, emptyList())
        if (action is ItemAction.Show) return show(context)

        if (action is ItemAction.Reset && action.scope.startsWith("unsupported:")) {
            val key = action.scope.removePrefix("unsupported:")
            return ItemResult(
                false,
                listOf(mini.deserialize("<yellow>Ветка <white>$key<yellow> пока unsupported на стабильном API сборки.")),
                warning = true
            )
        }

        validationService.validate(action, context, player)?.let { return it }

        if (action is ItemAction.HeadSetFromName) {
            return headProfileService.applyFromNameAsync(player.uniqueId, action.name)
        }

        val initialItem = context.item.clone()
        val outcome = apply(player, action, initialItem)
        if (!outcome.result.ok) return outcome.result

        targetResolver.save(player, outcome.item)
        return outcome.result
    }

    private fun apply(player: Player, action: ItemAction, item: org.bukkit.inventory.ItemStack): ApplyOutcome {
        if (action is ItemAction.Reset) {
            return when (action.scope) {
                "all" -> {
                    ItemStackReplacementSupport.resetAll(item)
                    ApplyOutcome(item, success("<green>Состояние предмета очищено (reset all)."))
                }
                else -> ApplyOutcome(item, failure("<red>Использование: /edit reset <all>"))
            }
        }

        if (action is ItemAction.SetItemId) {
            val replaced = ItemStackReplacementSupport.replaceItemId(item, action.material)
            return ApplyOutcome(replaced, success("<green>ID предмета изменён на <white>${action.material.key.asString()}</white>."))
        }

        if (componentsService.supports(action)) {
            return ApplyOutcome(item, componentsService.apply(player, action, item))
        }

        if (action is ItemAction.LockSetFromOffhand || action is ItemAction.LockClear) {
            return ApplyOutcome(item, LockActionsHelper.apply(player, action, item))
        }

        if (action is ItemAction.FrameSetInvisibility) {
            val replaced = FrameInvisibilitySupport.apply(item, action.value)
                ?: return ApplyOutcome(item, failure("<red>Эта ветка только для minecraft:item_frame и minecraft:glow_item_frame"))
            val state = if (action.value) "on" else "off"
            return ApplyOutcome(replaced, success("<green>Frame invisibility: <white>$state</white>."))
        }

        val metaResult = metaActionsApplier.apply(action, item)
            ?: failure("<red>Ветка не поддерживается для item meta")
        return ApplyOutcome(item, metaResult)
    }

    private fun success(message: String): ItemResult = ItemResult(true, listOf(mini.deserialize(message)))

    private fun failure(message: String): ItemResult = ItemResult(false, listOf(mini.deserialize(message)))

    private data class ApplyOutcome(
        val item: org.bukkit.inventory.ItemStack,
        val result: ItemResult
    )
}
