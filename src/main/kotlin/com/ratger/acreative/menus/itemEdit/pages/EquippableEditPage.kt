@file:Suppress("UnstableApiUsage") //Experimental Sound/Equippable

package com.ratger.acreative.menus.itemEdit.pages

import com.ratger.acreative.itemedit.equippable.EquippableSupport
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.itemEdit.ItemEditMenuSupport
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import com.ratger.acreative.menus.itemEdit.apply.EditorApplyKind
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import ru.violence.coreapi.bukkit.api.menu.MenuRows

class EquippableEditPage(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val openAdvancedPageTwo: (Player, ItemEditSession) -> Unit,
    private val requestApplyInput: (Player, ItemEditSession, EditorApplyKind, (Player, ItemEditSession) -> Unit) -> Unit
) {
    private data class IconOption<T>(
        val value: T,
        val label: String,
        val icon: Material
    )

    private val slotOptions = listOf(
        IconOption(EquipmentSlot.HEAD, "Шлем", Material.IRON_HELMET),
        IconOption(EquipmentSlot.CHEST, "Нагрудник", Material.IRON_CHESTPLATE),
        IconOption(EquipmentSlot.LEGS, "Поножи", Material.IRON_LEGGINGS),
        IconOption(EquipmentSlot.FEET, "Ботинки", Material.IRON_BOOTS),
        IconOption(EquipmentSlot.HAND, "Основная рука", Material.IRON_SWORD),
        IconOption(EquipmentSlot.OFF_HAND, "Вторая рука", Material.SHIELD)
    )

    private val overlayOptions = listOf(
        IconOption<NamespacedKey?>(null, "Нет", Material.BRICK),
        IconOption(NamespacedKey.fromString("minecraft:misc/powder_snow_outline")!!, "Снег", Material.POWDER_SNOW_BUCKET),
        IconOption(NamespacedKey.fromString("minecraft:misc/underwater")!!, "Вода", Material.WATER_BUCKET),
        IconOption(NamespacedKey.fromString("minecraft:misc/spyglass_scope")!!, "Скафандр", Material.GOLDEN_HELMET),
        IconOption(NamespacedKey.fromString("minecraft:misc/pumpkinblur")!!, "Тыква", Material.CARVED_PUMPKIN),
        IconOption(NamespacedKey.fromString("minecraft:misc/forcefield")!!, "Барьер", Material.BARRIER),
        IconOption(NamespacedKey.fromString("minecraft:misc/white")!!, "Белое", Material.WHITE_CONCRETE),
        IconOption(NamespacedKey.fromString("minecraft:misc/shadow")!!, "Чёрный круг", Material.BLACK_CONCRETE),
        IconOption(NamespacedKey.fromString("minecraft:misc/nausea")!!, "Чёрно-белое", Material.PANDA_SPAWN_EGG),
        IconOption(NamespacedKey.fromString("minecraft:misc/unknown_server")!!, "Иконка", Material.PAINTING),
        IconOption(NamespacedKey.fromString("minecraft:misc/null")!!, "null", Material.STRUCTURE_VOID)
    )

    private val modelOptions = listOf(
        IconOption<NamespacedKey?>(null, "Обычное", Material.BRICK),
        IconOption(NamespacedKey.fromString("minecraft:leather")!!, "Кожа", Material.LEATHER_CHESTPLATE),
        IconOption(NamespacedKey.fromString("minecraft:chain")!!, "Кольчуга", Material.CHAINMAIL_CHESTPLATE),
        IconOption(NamespacedKey.fromString("minecraft:iron")!!, "Железо", Material.IRON_CHESTPLATE),
        IconOption(NamespacedKey.fromString("minecraft:gold")!!, "Золото", Material.GOLDEN_CHESTPLATE),
        IconOption(NamespacedKey.fromString("minecraft:diamond")!!, "Алмаз", Material.DIAMOND_CHESTPLATE),
        IconOption(NamespacedKey.fromString("minecraft:netherite")!!, "Незерит", Material.NETHERITE_CHESTPLATE)
    )

    fun open(player: Player, session: ItemEditSession) {
        val menu = support.buildMenu(
            title = "<!i>▍ Редактор → Экипировка",
            menuSize = 54,
            rows = MenuRows.SIX,
            interactiveTopSlots = setOf(18, 20, 22, 24, 27, 29, 31, 33, 40),
            session = session
        )

        support.fillBase(menu, 54, support.advancedBlackSlots)
        menu.setButton(18, buttonFactory.backButton { support.transition(session) { openAdvancedPageTwo(player, session) } })
        menu.setButton(27, buttonFactory.backButton { support.transition(session) { openAdvancedPageTwo(player, session) } })

        refreshButtons(menu, player, session)
        menu.open(player)
    }

    private fun refreshButtons(menu: ru.violence.coreapi.bukkit.api.menu.Menu, player: Player, session: ItemEditSession) {
        menu.setButton(support.editableSlot, buttonFactory.editablePreviewButton(session.editableItem))
        menu.setButton(20, buildDamageOnHurtButton(session))
        menu.setButton(22, buildSlotButton(session))
        menu.setButton(24, buildDispensableButton(session))
        menu.setButton(29, buildSoundButton(player, session))
        menu.setButton(31, buildOverlayButton(session))
        menu.setButton(33, buildSwappableButton(session))
        menu.setButton(40, buildModelButton(session))
    }

    private fun buildDamageOnHurtButton(session: ItemEditSession) = buttonFactory.actionButton(
        material = Material.BLAZE_POWDER,
        name = if (EquippableSupport.effectiveDamageOnHurt(session.editableItem)) {
            "<!i><#C7A300>◎ <#FFD700>Поломка при уроне: <#00FF40>Вкл"
        } else {
            "<!i><#C7A300>⭘ <#FFD700>Поломка при уроне: <#FF1500>Выкл"
        },
        lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить"),
        itemModifier = {
            if (EquippableSupport.effectiveDamageOnHurt(session.editableItem)) {
                glint(true)
            }
            this
        },
        action = { event ->
            EquippableSupport.mutateOrCreateForMenu(session.editableItem, EquipmentSlot.HAND) {
                isDamageOnHurt = !EquippableSupport.effectiveDamageOnHurt(session.editableItem)
            }
            refreshButtons(event.menu, event.player, session)
        }
    )

    private fun buildDispensableButton(session: ItemEditSession) = buttonFactory.actionButton(
        material = Material.REDSTONE,
        name = if (EquippableSupport.effectiveDispensable(session.editableItem)) {
            "<!i><#C7A300>◎ <#FFD700>Раздатчик: <#00FF40>Вкл"
        } else {
            "<!i><#C7A300>⭘ <#FFD700>Раздатчик: <#FF1500>Выкл"
        },
        lore = listOf(
            "<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить",
            "",
            "<!i><#FFD700>Назначение:",
            "<!i><#C7A300> ● <#FFE68A>Возможность <#FFF3E0>надеть",
            "<!i><#C7A300> ● <#FFE68A>через раздатчик.",
            ""
        ),
        itemModifier = {
            if (EquippableSupport.effectiveDispensable(session.editableItem)) {
                glint(true)
            }
            this
        },
        action = { event ->
            EquippableSupport.mutateOrCreateForMenu(session.editableItem, EquipmentSlot.HAND) {
                isDispensable = !EquippableSupport.effectiveDispensable(session.editableItem)
            }
            refreshButtons(event.menu, event.player, session)
        }
    )

    private fun buildSwappableButton(session: ItemEditSession) = buttonFactory.actionButton(
        material = Material.WIND_CHARGE,
        name = if (EquippableSupport.effectiveSwappable(session.editableItem)) {
            "<!i><#C7A300>◎ <#FFD700>Свап: <#00FF40>Вкл"
        } else {
            "<!i><#C7A300>⭘ <#FFD700>Свап: <#FF1500>Выкл"
        },
        lore = listOf(
            "<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить",
            "",
            "<!i><#FFD700>Назначение:",
            "<!i><#C7A300> ● <#FFE68A>Возможность <#FFF3E0>быстро",
            "<!i><#C7A300> ● <#FFE68A>заменить другой бронёй.",
            ""
        ),
        itemModifier = {
            if (EquippableSupport.effectiveSwappable(session.editableItem)) {
                glint(true)
            }
            this
        },
        action = { event ->
            EquippableSupport.mutateOrCreateForMenu(session.editableItem, EquipmentSlot.HAND) {
                isSwappable = !EquippableSupport.effectiveSwappable(session.editableItem)
            }
            refreshButtons(event.menu, event.player, session)
        }
    )

    private fun buildSlotButton(session: ItemEditSession): ru.violence.coreapi.bukkit.api.menu.button.Button {
        val displaySlot = EquippableSupport.effectiveSlot(session.editableItem) ?: EquipmentSlot.HAND
        val selectedIndex = slotOptions.indexOfFirst { it.value == displaySlot }.takeIf { it >= 0 } ?: 0

        return buttonFactory.listButton(
            material = slotOptions[selectedIndex].icon,
            options = slotOptions.map { MenuButtonFactory.ListButtonOption(it.value, it.label) },
            selectedIndex = selectedIndex,
            titleBuilder = { _, index ->
                when (index) {
                    0 -> "<!i><#C7A300>① <#FFD700>Слот экипировки: <#FFF3E0>Шлем"
                    1 -> "<!i><#C7A300>② <#FFD700>Слот экипировки: <#FFF3E0>Нагрудник"
                    2 -> "<!i><#C7A300>③ <#FFD700>Слот экипировки: <#FFF3E0>Поножи"
                    3 -> "<!i><#C7A300>④ <#FFD700>Слот экипировки: <#FFF3E0>Ботинки"
                    4 -> "<!i><#C7A300>⑤ <#FFD700>Слот экипировки: <#FFF3E0>Основная рука"
                    else -> "<!i><#C7A300>⑥ <#FFD700>Слот экипировки: <#FFF3E0>Вторая рука"
                }
            },
            beforeOptionsLore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить", ""),
            afterOptionsLore = listOf(""),
            itemModifier = {
                buttonFactory.hideAttributes().invoke(this)
            },
            action = { event, newIndex ->
                val selected = slotOptions[newIndex]
                EquippableSupport.mutateOrCreateForMenu(session.editableItem, EquipmentSlot.HAND) {
                    slot = selected.value
                }
                refreshButtons(event.menu, event.player, session)
            }
        )
    }

    private fun buildOverlayButton(session: ItemEditSession): ru.violence.coreapi.bukkit.api.menu.button.Button {
        val explicit = EquippableSupport.explicitSnapshot(session.editableItem)
        val prototype = EquippableSupport.prototypeSnapshot(session.editableItem)
        val selectedIndex = if (EquippableSupport.isFieldOrdinaryOverlay(session.editableItem)) {
            0
        } else {
            val current = explicit?.cameraOverlay ?: prototype?.cameraOverlay
            overlayOptions.indexOfFirst { it.value == current }.takeIf { it >= 0 } ?: 0
        }

        return buttonFactory.listButton(
            material = overlayOptions[selectedIndex].icon,
            options = overlayOptions.map { MenuButtonFactory.ListButtonOption(it.value, it.label) },
            selectedIndex = selectedIndex,
            titleBuilder = { _, index ->
                when (index) {
                    0 -> "<!i><#C7A300>① <#FFD700>Оверлэй: <#FFF3E0>Нет"
                    1 -> "<!i><#C7A300>② <#FFD700>Оверлэй: <#FFF3E0>Снег"
                    2 -> "<!i><#C7A300>③ <#FFD700>Оверлэй: <#FFF3E0>Вода"
                    3 -> "<!i><#C7A300>④ <#FFD700>Оверлэй: <#FFF3E0>Скафандр"
                    4 -> "<!i><#C7A300>⑤ <#FFD700>Оверлэй: <#FFF3E0>Тыква"
                    5 -> "<!i><#C7A300>⑥ <#FFD700>Оверлэй: <#FFF3E0>Барьер"
                    6 -> "<!i><#C7A300>⑦ <#FFD700>Оверлэй: <#FFF3E0>Белое"
                    7 -> "<!i><#C7A300>⑧ <#FFD700>Оверлэй: <#FFF3E0>Чёрный круг"
                    8 -> "<!i><#C7A300>⑨ <#FFD700>Оверлэй: <#FFF3E0>Чёрно-белое"
                    9 -> "<!i><#C7A300>⑩ <#FFD700>Оверлэй: <#FFF3E0>Иконка"
                    else -> "<!i><#C7A300>⑪ <#FFD700>Оверлэй: <#FFF3E0>null"
                }
            },
            beforeOptionsLore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить", ""),
            afterOptionsLore = listOf(
                "",
                "<!i><#FFD700>Назначение:",
                "<!i><#C7A300> ● <#FFE68A>Текстура <#FFF3E0>на экране<#FFE68A>, если",
                "<!i><#C7A300> ● <#FFE68A>предмет надет на голову.",
                ""
            ),
            itemModifier = {
                buttonFactory.hideAttributes().invoke(this)
            },
            action = { event, newIndex ->
                val selected = overlayOptions[newIndex]
                EquippableSupport.mutateOrCreateForMenu(session.editableItem, EquipmentSlot.HEAD) {
                    cameraOverlay = selected.value
                }
                refreshButtons(event.menu, event.player, session)
            }
        )
    }

    private fun buildModelButton(session: ItemEditSession): ru.violence.coreapi.bukkit.api.menu.button.Button {
        val explicit = EquippableSupport.explicitSnapshot(session.editableItem)
        val prototype = EquippableSupport.prototypeSnapshot(session.editableItem)
        val selectedIndex = if (EquippableSupport.isFieldOrdinaryModel(session.editableItem)) {
            0
        } else {
            val current = explicit?.model ?: prototype?.model
            modelOptions.indexOfFirst { it.value == current }.takeIf { it >= 0 } ?: 0
        }

        return buttonFactory.listButton(
            material = modelOptions[selectedIndex].icon,
            options = modelOptions.map { MenuButtonFactory.ListButtonOption(it.value, it.label) },
            selectedIndex = selectedIndex,
            titleBuilder = { _, index ->
                when (index) {
                    0 -> "<!i><#C7A300>① <#FFD700>Отображение: <#FFF3E0>Обычное"
                    1 -> "<!i><#C7A300>② <#FFD700>Отображение: <#FFF3E0>Кожа"
                    2 -> "<!i><#C7A300>③ <#FFD700>Отображение: <#FFF3E0>Кольчуга"
                    3 -> "<!i><#C7A300>④ <#FFD700>Отображение: <#FFF3E0>Железо"
                    4 -> "<!i><#C7A300>⑤ <#FFD700>Отображение: <#FFF3E0>Золото"
                    5 -> "<!i><#C7A300>⑥ <#FFD700>Отображение: <#FFF3E0>Алмаз"
                    else -> "<!i><#C7A300>⑦ <#FFD700>Отображение: <#FFF3E0>Незерит"
                }
            },
            beforeOptionsLore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить", ""),
            afterOptionsLore = listOf(
                "",
                "<!i><#FFD700>Назначение:",
                "<!i><#C7A300> ● <#FFE68A>Влияет на отображаемую",
                "<!i><#C7A300> ● <#FFF3E0>модель<#FFE68A> надетой брони.",
                ""
            ),
            itemModifier = {
                buttonFactory.hideAttributes().invoke(this)
            },
            action = { event, newIndex ->
                val selected = modelOptions[newIndex]
                EquippableSupport.mutateOrCreateForMenu(session.editableItem, EquipmentSlot.HAND) {
                    model = selected.value
                }
                refreshButtons(event.menu, event.player, session)
            }
        )
    }

    private fun buildSoundButton(player: Player, session: ItemEditSession): ru.violence.coreapi.bukkit.api.menu.button.Button {
        val isOrdinary = EquippableSupport.isFieldOrdinarySound(session.editableItem)
        val soundKey = EquippableSupport.effectiveEquipSound(session.editableItem)?.let(Registry.SOUNDS::getKey)?.asString().orEmpty()

        return buttonFactory.actionButton(
            material = Material.MUSIC_DISC_13,
            name = if (isOrdinary) {
                "<!i><#C7A300>⭘ <#FFD700>Звук: <#FF1500>Обычный"
            } else {
                "<!i><#C7A300>◎ <#FFD700>Звук: <#00FF40>$soundKey"
            },
            itemModifier = {
                buttonFactory.hideJukeboxTooltip(Material.MUSIC_DISC_13).invoke(this)
                if (!isOrdinary) {
                    glint(true)
                }
                this
            },
            lore = listOf(
                "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
                "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
                "",
                "<!i><#FFD700>Назначение:",
                "<!i><#C7A300> ● <#FFE68A>Проигрываемый при",
                "<!i><#C7A300> ● <#FFF3E0>надевании<#FFE68A> брони.",
                "",
                "<!i><#FFD700>После нажатия:",
                "<!i><#C7A300> ● <#FFF3E0>/apply <звук> <#C7A300>- <#FFE68A>задать",
                "<!i><#C7A300> ● <#FFF3E0>/apply cancel <#C7A300>- <#FFE68A>отмена",
                ""
            ),
            action = { event ->
                if (event.isRight || event.isShiftRight) {
                    EquippableSupport.mutateOrCreateForMenu(session.editableItem, EquipmentSlot.HAND) {
                        setEquipSound(null)
                    }
                    refreshButtons(event.menu, player, session)
                } else if (event.isLeft || event.isShiftLeft) {
                    support.transition(session) {
                        requestApplyInput(player, session, EditorApplyKind.EQUIP_SOUND) { reopenPlayer, reopenSession ->
                            open(reopenPlayer, reopenSession)
                        }
                    }
                }
            }
        )
    }
}
