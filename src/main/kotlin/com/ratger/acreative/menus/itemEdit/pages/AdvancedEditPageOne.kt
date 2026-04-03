package com.ratger.acreative.menus.itemEdit.pages

import com.ratger.acreative.menus.itemEdit.apply.EditorApplyKind
import com.ratger.acreative.itemedit.meta.MaxStackSizeSupport
import com.ratger.acreative.menus.itemEdit.ItemEditMenuSupport
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import com.ratger.acreative.menus.MenuButtonFactory
import org.bukkit.NamespacedKey
import org.bukkit.Material
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.MenuRows

class AdvancedEditPageOne(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val openRoot: (Player, ItemEditSession) -> Unit,
    private val openAdvancedPageTwo: (Player, ItemEditSession) -> Unit,
    private val requestApplyInput: (Player, ItemEditSession, EditorApplyKind, (Player, ItemEditSession) -> Unit) -> Unit
) {
    private fun updateEditablePreview(menu: ru.violence.coreapi.bukkit.api.menu.Menu, session: ItemEditSession) {
        menu.setButton(support.editableSlot, buttonFactory.editablePreviewButton(session.editableItem))
    }

    fun open(player: Player, session: ItemEditSession) {
        val menuSize = 54
        val menu = support.buildMenu(
            title = "<!i>▍ Продвинутый редактор [1/2]",
            menuSize = menuSize,
            rows = MenuRows.SIX,
            interactiveTopSlots = setOf(18, 27, 26, 35, 31, 32, 33, 38, 39, 40, 41),
            session = session
        )

        support.fillBase(menu, menuSize, support.advancedBlackSlots)
        menu.setButton(support.editableSlot, buttonFactory.editablePreviewButton(session.editableItem))
        menu.setButton(18, buttonFactory.backButton { support.transition(session) { openRoot(player, session) } })
        menu.setButton(27, buttonFactory.backButton { support.transition(session) { openRoot(player, session) } })
        menu.setButton(26, buttonFactory.forwardButton { support.transition(session) { openAdvancedPageTwo(player, session) } })
        menu.setButton(35, buttonFactory.forwardButton { support.transition(session) { openAdvancedPageTwo(player, session) } })

        val itemId = session.editableItem.type.key.key
        val amount = session.editableItem.amount
        val editableMeta = session.editableItem.itemMeta
        val modelKey = runCatching { editableMeta?.itemModel }.getOrNull()
        val modelId = runCatching { modelKey?.asString() }.getOrNull()
        val modelName = if (modelId == null) {
            "<!i><#C7A300>⭘ <#FFD700>Модель: <#FF1500>Обычная"
        } else {
            "<!i><#C7A300>◎ <#FFD700>Модель: <#00FF40>$modelId"
        }
        val stackSize = if (editableMeta?.hasMaxStackSize() == true) editableMeta.maxStackSize else null
        val stackSizeName = if (stackSize == null) {
            "<!i><#C7A300>⭘ <#FFD700>Размер стака: <#FF1500>Обычный"
        } else {
            "<!i><#C7A300>◎ <#FFD700>Размер стака: <#00FF40>$stackSize"
        }
        menu.setButton(29, buttonFactory.specialParameterButton(session.editableItem, player))
        menu.setButton(30, buttonFactory.actionButton(Material.NAME_TAG, "<!i><#C7A300>✎ <#FFD700>Изменить название и описание", listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть")))
        menu.setButton(31, buttonFactory.actionButton(session.editableItem.type, "<!i><#C7A300>◎ <#FFD700>ID предмета: <#00FF40>$itemId", listOf(
            "<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить",
            "",
            "<!i><#FFD700>После нажатия:",
            "<!i><#C7A300> ● <#FFF3E0>/apply <id> <#C7A300>- <#FFE68A>задать по id ",
            "<!i><#C7A300> ● <#FFF3E0>/apply cancel <#C7A300>- <#FFE68A>отмена ",
            ""
        ), buttonFactory.hideEverythingExceptTooltip(), action = {
            support.transition(session) {
                requestApplyInput(player, session, EditorApplyKind.ITEM_ID) { reopenPlayer, reopenSession ->
                    open(reopenPlayer, reopenSession)
                }
            }
        }))
        menu.setButton(32, buttonFactory.actionButton(Material.STRUCTURE_VOID, modelName, listOf(
            "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
            "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
            "",
            "<!i><#FFD700>Назначение:",
            "<!i><#C7A300> ● <#FFE68A>Задаёт <#FFF3E0>внешний <#FFE68A>вид предмета. ",
            "<!i><#C7A300> ● <#FFE68A>Не влияет на его <#FFF3E0>поведение. ",
            "",
            "<!i><#FFD700>После нажатия:",
            "<!i><#C7A300> ● <#FFF3E0>/apply <id> <#C7A300>- <#FFE68A>задать по id ",
            "<!i><#C7A300> ● <#FFF3E0>/apply hand <#C7A300>- <#FFE68A>взять из руки ",
            ""
        ), itemModifier = {
            modelKey?.let { key ->
                edit { item ->
                    val meta = item.itemMeta ?: return@edit
                    meta.itemModel = key
                    item.itemMeta = meta
                }
            }
            buttonFactory.hideEverythingExceptTooltip().invoke(this)
        }, action = { event ->
            if (event.isRight || event.isShiftRight) {
                val meta = session.editableItem.itemMeta
                if (meta != null) {
                    meta.itemModel = null
                    session.editableItem.itemMeta = meta
                    menu.setButton(32, buttonFactory.actionButton(
                        Material.STRUCTURE_VOID,
                        "<!i><#C7A300>⭘ <#FFD700>Модель: <#FF1500>Обычная",
                        listOf(
                            "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
                            "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
                            "",
                            "<!i><#FFD700>Назначение:",
                            "<!i><#C7A300> ● <#FFE68A>Задаёт <#FFF3E0>внешний <#FFE68A>вид предмета. ",
                            "<!i><#C7A300> ● <#FFE68A>Не влияет на его <#FFF3E0>поведение. ",
                            "",
                            "<!i><#FFD700>После нажатия:",
                            "<!i><#C7A300> ● <#FFF3E0>/apply <id> <#C7A300>- <#FFE68A>задать по id ",
                            "<!i><#C7A300> ● <#FFF3E0>/apply hand <#C7A300>- <#FFE68A>взять из руки ",
                            ""
                        ),
                        itemModifier = { buttonFactory.hideEverythingExceptTooltip().invoke(this) },
                        action = { clickEvent ->
                            if (clickEvent.isLeft || clickEvent.isShiftLeft) {
                                support.transition(session) {
                                    requestApplyInput(player, session, EditorApplyKind.ITEM_MODEL) { reopenPlayer, reopenSession ->
                                        open(reopenPlayer, reopenSession)
                                    }
                                }
                            }
                        }
                    ))
                    updateEditablePreview(menu, session)
                }
            } else if (event.isLeft || event.isShiftLeft) {
                support.transition(session) {
                    requestApplyInput(player, session, EditorApplyKind.ITEM_MODEL) { reopenPlayer, reopenSession ->
                        open(reopenPlayer, reopenSession)
                    }
                }
            }
        }))
        menu.setButton(33, buttonFactory.actionButton(Material.BUNDLE, "<!i><#C7A300>◎ <#FFD700>Количество: <#00FF40>$amount", listOf(
            "<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить",
            "",
            "<!i><#FFD700>После нажатия:",
            "<!i><#C7A300> ● <#FFF3E0>/apply <число> <#C7A300>- <#FFE68A>задать ",
            "<!i><#C7A300> ● <#FFF3E0>/apply cancel <#C7A300>- <#FFE68A>отмена ",
            ""
        ), buttonFactory.hideAdditionalTooltip(), action = {
            support.transition(session) {
                requestApplyInput(player, session, EditorApplyKind.AMOUNT) { reopenPlayer, reopenSession ->
                    open(reopenPlayer, reopenSession)
                }
            }
        }))
        menu.setButton(38, buttonFactory.actionButton(Material.BRICK, stackSizeName, listOf(
            "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
            "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
            "",
            "<!i><#FFD700>После нажатия:",
            "<!i><#C7A300> ● <#FFF3E0>/apply <число> <#C7A300>- <#FFE68A>задать ",
            "<!i><#C7A300> ● <#FFF3E0>/apply max <#C7A300>- <#FFE68A>максимум ",
            ""
        ), action = { event ->
            if (event.isRight || event.isShiftRight) {
                val meta = session.editableItem.itemMeta
                if (meta != null) {
                    if (meta.hasMaxStackSize()) {
                        if (MaxStackSizeSupport.clearCustomMaxStackSize(meta)) {
                            session.editableItem.itemMeta = meta
                        }
                    }
                    menu.setButton(38, buttonFactory.actionButton(
                        Material.BRICK,
                        "<!i><#C7A300>⭘ <#FFD700>Размер стака: <#FF1500>Обычный",
                        listOf(
                            "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
                            "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
                            "",
                            "<!i><#FFD700>После нажатия:",
                            "<!i><#C7A300> ● <#FFF3E0>/apply <число> <#C7A300>- <#FFE68A>задать ",
                            "<!i><#C7A300> ● <#FFF3E0>/apply max <#C7A300>- <#FFE68A>максимум ",
                            ""
                        ),
                        action = { clickEvent ->
                            if (clickEvent.isLeft || clickEvent.isShiftLeft) {
                                support.transition(session) {
                                    requestApplyInput(player, session, EditorApplyKind.STACK_SIZE) { reopenPlayer, reopenSession ->
                                        open(reopenPlayer, reopenSession)
                                    }
                                }
                            }
                        }
                    ))
                    updateEditablePreview(menu, session)
                }
            } else if (event.isLeft || event.isShiftLeft) {
                support.transition(session) {
                    requestApplyInput(player, session, EditorApplyKind.STACK_SIZE) { reopenPlayer, reopenSession ->
                        open(reopenPlayer, reopenSession)
                    }
                }
            }
        }))
        val brokenTooltipKey = NamespacedKey.minecraft("null")
        val selectedTooltipIndex = if (session.editableItem.itemMeta?.tooltipStyle == brokenTooltipKey) 1 else 0
        val tooltipOptions: List<MenuButtonFactory.ListButtonOption<NamespacedKey?>> = listOf(
            MenuButtonFactory.ListButtonOption(null, "Обычный"),
            MenuButtonFactory.ListButtonOption(brokenTooltipKey, "Сломанный")
        )
        menu.setButton(39, buttonFactory.listButton(
            material = Material.PAINTING,
            options = tooltipOptions,
            selectedIndex = selectedTooltipIndex,
            titleBuilder = { _, index ->
                when (index) {
                    0 -> "<!i><#C7A300>① <#FFD700>Тултип: <#FFF3E0>Обычный"
                    else -> "<!i><#C7A300>② <#FFD700>Тултип: <#FFF3E0>Сломанный"
                }
            },
            beforeOptionsLore = listOf(
                "<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить",
                ""
            ),
            afterOptionsLore = listOf("<!i>"),
            itemModifier = { selected ->
                edit { item ->
                    val meta = item.itemMeta ?: return@edit
                    meta.tooltipStyle = selected.value
                    item.itemMeta = meta
                }
            },
            action = { _, newIndex ->
                val selected = tooltipOptions[newIndex]
                val meta = session.editableItem.itemMeta ?: return@listButton
                meta.tooltipStyle = selected.value
                session.editableItem.itemMeta = meta
                menu.setButton(39, buildTooltipButton(session))
                updateEditablePreview(menu, session)
            }
        ))
        val unbreakableEnabled = session.editableItem.itemMeta?.isUnbreakable == true
        val unbreakableButtonName = if (unbreakableEnabled) {
            "<!i><#C7A300>◎ <#FFD700>Неразрушимость: <#00FF40>Вкл"
        } else {
            "<!i><#C7A300>⭘ <#FFD700>Неразрушимость: <#FF1500>Выкл"
        }
        menu.setButton(40, buttonFactory.actionButton(
            Material.NETHERITE_INGOT,
            unbreakableButtonName,
            listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить"),
            action = {
                val meta = session.editableItem.itemMeta ?: return@actionButton
                meta.isUnbreakable = !unbreakableEnabled
                session.editableItem.itemMeta = meta
                menu.setButton(40, buildUnbreakableButton(session))
                updateEditablePreview(menu, session)
            }
        ))
        val gliderEnabled = runCatching { session.editableItem.itemMeta?.isGlider == true }.getOrDefault(false)
        val gliderButtonName = if (gliderEnabled) {
            "<!i><#C7A300>◎ <#FFD700>Парение: <#00FF40>Вкл"
        } else {
            "<!i><#C7A300>⭘ <#FFD700>Парение: <#FF1500>Выкл"
        }
        menu.setButton(41, buttonFactory.actionButton(Material.ELYTRA, gliderButtonName, listOf(
            "<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить",
            "",
            "<!i><#FFD700>Назначение:",
            "<!i><#C7A300> ● <#FFE68A>Позволяет <#FFF3E0>парить, <#FFE68A>как на элитрах. ",
            ""
        ), action = {
            val meta = session.editableItem.itemMeta ?: return@actionButton
            meta.isGlider = !gliderEnabled
            session.editableItem.itemMeta = meta
            menu.setButton(41, buildGliderButton(session))
            updateEditablePreview(menu, session)
        }))
        menu.setButton(42, buttonFactory.actionButton(Material.BRUSH, "<!i><#C7A300>✂ <#FFD700>Скрытие информации", listOf(
            "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы идти дальше",
            "<!i><#FFD700>ПКМ, <#FFE68A>чтобы переключить",
            "<!i><#FFD700>Q, <#FFE68A>чтобы всё сбросить",
            "",
            "<!i><#FFF3E0>[<#00FF40>✔<#FFF3E0>]  <#00FF40>» Скрыть зачарования ",
            "<!i><#FFF3E0>[<#FF1500>✘<#FFF3E0>]<b> </b><#C7A300>» Скрыть атрибуты ",
            "<!i><#FFF3E0>[<#00FF40>✔<#FFF3E0>]<b> </b><#C7A300>» Скрыть неразрушимость ",
            "<!i><#FFF3E0>[<#FF1500>✘<#FFF3E0>]<b> </b><#C7A300>» Скрыть разное ",
            "<!i><#FFF3E0>[<#FF1500>✘<#FFF3E0>]<b> </b><#C7A300>» Скрыть цвет брони ",
            "<!i><#FFF3E0>[<#00FF40>✔<#FFF3E0>]<b> </b><#C7A300>» Скрыть ограничения ломания ",
            "<!i><#FFF3E0>[<#FF1500>✘<#FFF3E0>]<b> </b><#C7A300>» Скрыть ограничения установки ",
            "<!i><#FFF3E0>[<#FF1500>✘<#FFF3E0>]<b> </b><#C7A300>» Скрыть отделку брони ",
            "<!i><#FFF3E0>[<#FF1500>✘<#FFF3E0>]<b> </b><#C7A300>» Скрыть музыку ",
            "<!i><#FFF3E0>[<#00FF40>✔<#FFF3E0>]<b> </b><#C7A300>» Скрыть всё ",
            "<!i><#FFF3E0>[<#FF1500>✘<#FFF3E0>]<b> </b><#C7A300>» Скрыть само отображение ",
            ""
        )))
        menu.open(player)
    }

    private fun buildTooltipButton(session: ItemEditSession): ru.violence.coreapi.bukkit.api.menu.button.Button {
        val brokenTooltipKey = NamespacedKey.minecraft("null")
        val tooltipOptions: List<MenuButtonFactory.ListButtonOption<NamespacedKey?>> = listOf(
            MenuButtonFactory.ListButtonOption(null, "Обычный"),
            MenuButtonFactory.ListButtonOption(brokenTooltipKey, "Сломанный")
        )
        val selectedTooltipIndex = if (session.editableItem.itemMeta?.tooltipStyle == brokenTooltipKey) 1 else 0
        return buttonFactory.listButton(
            material = Material.PAINTING,
            options = tooltipOptions,
            selectedIndex = selectedTooltipIndex,
            titleBuilder = { _, index ->
                when (index) {
                    0 -> "<!i><#C7A300>① <#FFD700>Тултип: <#FFF3E0>Обычный"
                    else -> "<!i><#C7A300>② <#FFD700>Тултип: <#FFF3E0>Сломанный"
                }
            },
            beforeOptionsLore = listOf(
                "<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить",
                ""
            ),
            afterOptionsLore = listOf("<!i>"),
            itemModifier = { selected ->
                edit { item ->
                    val meta = item.itemMeta ?: return@edit
                    meta.tooltipStyle = selected.value
                    item.itemMeta = meta
                }
            },
            action = { event, newIndex ->
                val selected = tooltipOptions[newIndex]
                val meta = session.editableItem.itemMeta ?: return@listButton
                meta.tooltipStyle = selected.value
                session.editableItem.itemMeta = meta
                event.menu.setButton(39, buildTooltipButton(session))
                updateEditablePreview(event.menu, session)
            }
        )
    }

    private fun buildUnbreakableButton(session: ItemEditSession): ru.violence.coreapi.bukkit.api.menu.button.Button {
        val unbreakableEnabled = session.editableItem.itemMeta?.isUnbreakable == true
        val unbreakableButtonName = if (unbreakableEnabled) {
            "<!i><#C7A300>◎ <#FFD700>Неразрушимость: <#00FF40>Вкл"
        } else {
            "<!i><#C7A300>⭘ <#FFD700>Неразрушимость: <#FF1500>Выкл"
        }
        return buttonFactory.actionButton(
            Material.NETHERITE_INGOT,
            unbreakableButtonName,
            listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить"),
            action = { event ->
                val meta = session.editableItem.itemMeta ?: return@actionButton
                meta.isUnbreakable = !unbreakableEnabled
                session.editableItem.itemMeta = meta
                event.menu.setButton(40, buildUnbreakableButton(session))
                updateEditablePreview(event.menu, session)
            }
        )
    }

    private fun buildGliderButton(session: ItemEditSession): ru.violence.coreapi.bukkit.api.menu.button.Button {
        val gliderEnabled = runCatching { session.editableItem.itemMeta?.isGlider == true }.getOrDefault(false)
        val gliderButtonName = if (gliderEnabled) {
            "<!i><#C7A300>◎ <#FFD700>Парение: <#00FF40>Вкл"
        } else {
            "<!i><#C7A300>⭘ <#FFD700>Парение: <#FF1500>Выкл"
        }
        return buttonFactory.actionButton(
            Material.ELYTRA,
            gliderButtonName,
            listOf(
                "<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить",
                "",
                "<!i><#FFD700>Назначение:",
                "<!i><#C7A300> ● <#FFE68A>Позволяет <#FFF3E0>парить, <#FFE68A>как на элитрах. ",
                ""
            ),
            action = { event ->
                val meta = session.editableItem.itemMeta ?: return@actionButton
                meta.isGlider = !gliderEnabled
                session.editableItem.itemMeta = meta
                event.menu.setButton(41, buildGliderButton(session))
                updateEditablePreview(event.menu, session)
            }
        )
    }
}
