package com.ratger.acreative.menus.itemEdit.pages

import com.ratger.acreative.itemedit.head.HeadTextureMutationSupport
import com.ratger.acreative.itemedit.head.HeadTextureSnapshot
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.itemEdit.ItemEditMenuSupport
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import com.ratger.acreative.menus.itemEdit.apply.EditorApplyKind
import org.bukkit.Material
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.Menu
import ru.violence.coreapi.bukkit.api.menu.MenuRows
import ru.violence.coreapi.bukkit.api.menu.button.Button

class HeadTextureEditPage(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val mutationSupport: HeadTextureMutationSupport,
    private val openBack: (Player, ItemEditSession) -> Unit,
    private val requestApplyInput: (Player, ItemEditSession, EditorApplyKind, (Player, ItemEditSession) -> Unit) -> Unit
) {
    fun open(player: Player, session: ItemEditSession) {
        session.headTextureSectionActive = true
        val isLoading = session.headTextureLoadingToken != null

        val menu = support.buildMenu(
            title = "<!i>▍ Редактор → Текстура головы",
            menuSize = 45,
            rows = MenuRows.FIVE,
            interactiveTopSlots = if (isLoading) setOf(18) else setOf(18, 29, 30, 31, 33),
            session = session
        )

        support.fillBase(menu, 45, setOf(0, 8, 9, 17, 18, 26, 27, 35, 36, 44, 12, 14))
        menu.setButton(18, buttonFactory.backButton {
            session.headTextureSectionActive = false
            session.headTextureLoadingToken = null
            support.transition(session) { openBack(player, session) }
        })

        if (isLoading) {
            applyLoadingButtons(menu)
            menu.open(player)
            return
        }

        val snapshot = HeadTextureSnapshot.fromItem(session.editableItem)
        menu.setButton(13, buttonFactory.editablePreviewButton(session.editableItem))
        menu.setButton(29, onlineButton(player, session, snapshot, menu))
        menu.setButton(30, valueButton(player, session, snapshot, menu))
        menu.setButton(31, licensedButton(player, session, snapshot, menu))
        menu.setButton(33, getValueButton(player, session, menu))
        menu.open(player)
    }

    private fun applyLoadingButtons(menu: Menu) {
        val loading = buttonFactory.actionButton(Material.CLOCK, "<!i><#00FF40>Загрузка текстуры..", emptyList())
        menu.setButton(13, loading)
        menu.setButton(29, loading)
        menu.setButton(30, loading)
        menu.setButton(31, loading)
        menu.setButton(33, loading)
    }

    private fun getValueButton(player: Player, session: ItemEditSession, menu: Menu): Button = buttonFactory.actionButton(
        material = Material.ENDER_EYE,
        name = "<!i><#00FF40>₪ Получить value",
        lore = emptyList(),
        action = {
            val value = mutationSupport.texturesValue(session.editableItem)
            if (value.isNullOrBlank()) {
                support.replaceSlotTemporarily(
                    menu = menu,
                    slot = 33,
                    temporaryButton = buttonFactory.actionButton(
                        material = Material.BARRIER,
                        name = "<!i><#FF1500>⚠ У этой головы нет текстуры",
                        lore = emptyList()
                    ),
                    restoreAfterTicks = 30L,
                    restoreButton = { getValueButton(player, session, menu) }
                )
                return@actionButton
            }
            player.sendRichMessage("<dark_green>▍ <click:suggest_command:'$value'><hover:show_text:'<gray>Нажмите'><#00FF40><u>Скопировать value</u></hover></click>")
        }
    )

    private fun onlineButton(player: Player, session: ItemEditSession, snapshot: HeadTextureSnapshot, menu: Menu): Button = if (snapshot.canShowOnlineActive) {
        buttonFactory.headTextureSourceButton(
            editedItem = session.editableItem,
            activeName = "<!i><#C7A300>◎ <#FFD700>От игрока: <#00FF40>${snapshot.profileName}",
            lore = sourceLoreOnline(active = true),
            action = { event ->
                if (event.isRight || event.isShiftRight) {
                    clearAndRefresh(player, session, menu)
                } else if (event.isLeft || event.isShiftLeft) {
                    support.transition(session) {
                        requestApplyInput(player, session, EditorApplyKind.HEAD_ONLINE_NAME, this::open)
                    }
                }
            }
        )
    } else {
        buttonFactory.actionButton(
            material = Material.LIME_DYE,
            name = "<!i><#C7A300>⭘ <#FFD700>От игрока: <#FF1500>Нет",
            lore = sourceLoreOnline(active = false),
            action = { event ->
                if (event.isRight || event.isShiftRight) {
                    clearAndRefresh(player, session, menu)
                } else if (event.isLeft || event.isShiftLeft) {
                    support.transition(session) {
                        requestApplyInput(player, session, EditorApplyKind.HEAD_ONLINE_NAME, this::open)
                    }
                }
            }
        )
    }

    private fun valueButton(player: Player, session: ItemEditSession, snapshot: HeadTextureSnapshot, menu: Menu): Button = if (snapshot.canShowTextureValueActive) {
        buttonFactory.headTextureSourceButton(
            editedItem = session.editableItem,
            activeName = "<!i><#C7A300>◎ <#FFD700>По value: <#00FF40>Задано",
            lore = sourceLoreValue(active = true),
            action = { event ->
                if (event.isRight || event.isShiftRight) {
                    clearAndRefresh(player, session, menu)
                } else if (event.isLeft || event.isShiftLeft) {
                    support.transition(session) {
                        requestApplyInput(player, session, EditorApplyKind.HEAD_TEXTURE_VALUE, this::open)
                    }
                }
            }
        )
    } else {
        buttonFactory.actionButton(
            material = Material.LIGHT_BLUE_DYE,
            name = "<!i><#C7A300>⭘ <#FFD700>По value: <#FF1500>Нет",
            lore = sourceLoreValue(active = false),
            action = { event ->
                if (event.isRight || event.isShiftRight) {
                    clearAndRefresh(player, session, menu)
                } else if (event.isLeft || event.isShiftLeft) {
                    support.transition(session) {
                        requestApplyInput(player, session, EditorApplyKind.HEAD_TEXTURE_VALUE, this::open)
                    }
                }
            }
        )
    }

    private fun licensedButton(player: Player, session: ItemEditSession, snapshot: HeadTextureSnapshot, menu: Menu): Button = if (snapshot.canShowLicensedActive) {
        buttonFactory.headTextureSourceButton(
            editedItem = session.editableItem,
            activeName = "<!i><#C7A300>◎ <#FFD700>По лицензии: <#00FF40>${snapshot.profileName}",
            lore = sourceLoreLicensed(active = true),
            action = { event ->
                if (event.isRight || event.isShiftRight) {
                    clearAndRefresh(player, session, menu)
                } else if (event.isLeft || event.isShiftLeft) {
                    support.transition(session) {
                        requestApplyInput(player, session, EditorApplyKind.HEAD_LICENSED_NAME, this::open)
                    }
                }
            }
        )
    } else {
        buttonFactory.actionButton(
            material = Material.PURPLE_DYE,
            name = "<!i><#C7A300>⭘ <#FFD700>По лицензии: <#FF1500>Нет",
            lore = sourceLoreLicensed(active = false),
            action = { event ->
                if (event.isRight || event.isShiftRight) {
                    clearAndRefresh(player, session, menu)
                } else if (event.isLeft || event.isShiftLeft) {
                    support.transition(session) {
                        requestApplyInput(player, session, EditorApplyKind.HEAD_LICENSED_NAME, this::open)
                    }
                }
            }
        )
    }

    private fun clearAndRefresh(player: Player, session: ItemEditSession, menu: Menu) {
        mutationSupport.clearProfile(session.editableItem)
        menu.setButton(13, buttonFactory.editablePreviewButton(session.editableItem))
        val snapshot = HeadTextureSnapshot.fromItem(session.editableItem)
        menu.setButton(29, onlineButton(player, session, snapshot, menu))
        menu.setButton(30, valueButton(player, session, snapshot, menu))
        menu.setButton(31, licensedButton(player, session, snapshot, menu))
        menu.setButton(33, getValueButton(player, session, menu))
    }

    private fun sourceLoreOnline(active: Boolean): List<String> = listOf(
        "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
        "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
        "",
        "<!i><#FFD700>Назначение:",
        "<!i><#C7A300> ● <#FFE68A>Берёт голову игрока <#FFF3E0>онлайн. ",
        "",
        "<!i><#FFD700>После нажатия:",
        "<!i><#C7A300> ● <#FFF3E0>/apply <ник><#FFE68A> <#C7A300>- <#FFE68A>задать ",
        if (active) "<!i><#C7A300> ● <#FFF3E0>/apply cancel<#FFE68A> <#C7A300>- <#FFE68A>отмена" else "<!i><#C7A300> ● <#FFF3E0>/apply cancel<#FFE68A> <#C7A300>- <#FFE68A>отменить",
        ""
    )

    private fun sourceLoreValue(active: Boolean): List<String> = listOf(
        "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
        "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
        "",
        "<!i><#FFD700>Назначение:",
        "<!i><#C7A300> ● <#FFE68A>Берёт текстуру из <#FFF3E0>value. ",
        "",
        "<!i><#FFD700>После нажатия:",
        "<!i><#C7A300> ● <#FFF3E0>/apply <value><#FFE68A> <#C7A300>- <#FFE68A>задать ",
        if (active) "<!i><#C7A300> ● <#FFF3E0>/apply cancel<#FFE68A> <#C7A300>- <#FFE68A>отмена " else "<!i><#C7A300> ● <#FFF3E0>/apply cancel<#FFE68A> <#C7A300>- <#FFE68A>отменить ",
        ""
    )

    private fun sourceLoreLicensed(active: Boolean): List<String> = listOf(
        "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
        "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
        "",
        "<!i><#FFD700>Назначение:",
        "<!i><#C7A300> ● <#FFE68A>Берёт текстуру от",
        "<!i><#C7A300> ● <#FFF3E0>лицензионного<#FFE68A> ника.",
        "",
        "<!i><#FFD700>После нажатия:",
        "<!i><#C7A300> ● <#FFF3E0>/apply <ник><#FFE68A> <#C7A300>- <#FFE68A>задать ",
        if (active) "<!i><#C7A300> ● <#FFF3E0>/apply cancel<#FFE68A> <#C7A300>- <#FFE68A>отмена " else "<!i><#C7A300> ● <#FFF3E0>/apply cancel<#FFE68A> <#C7A300>- <#FFE68A>отменить ",
        ""
    )
}
