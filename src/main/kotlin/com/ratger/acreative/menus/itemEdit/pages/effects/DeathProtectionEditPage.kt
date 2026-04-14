package com.ratger.acreative.menus.itemEdit.pages.effects

import com.ratger.acreative.itemedit.effects.DeathProtectionMenuSupport
import com.ratger.acreative.itemedit.potion.PotionItemSupport
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.itemEdit.ItemEditMenuSupport
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import com.ratger.acreative.itemedit.apply.core.EditorApplyKind
import org.bukkit.Material
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.Menu
import ru.violence.coreapi.bukkit.api.menu.MenuRows
import ru.violence.coreapi.bukkit.api.menu.button.Button
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

class DeathProtectionEditPage(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val openAdvancedPageTwo: (Player, ItemEditSession) -> Unit,
    private val openRemoveEffectsPage: (Player, ItemEditSession) -> Unit,
    private val openApplyEffectsPage: (Player, ItemEditSession) -> Unit,
    private val requestApplyInput: (Player, ItemEditSession, EditorApplyKind, (Player, ItemEditSession) -> Unit) -> Unit
) {
    private val blackSlots = setOf(0, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 53)

    fun open(player: Player, session: ItemEditSession) {
        val menu = support.buildMenu(
            title = "<!i>▍ Редактор → Защита от смерти",
            menuSize = 54,
            rows = MenuRows.SIX,
            interactiveTopSlots = setOf(18, 27, 31, 38, 39, 40, 41, 42),
            session = session
        )

        support.fillBase(menu, 54, blackSlots)
        menu.setButton(18, buttonFactory.backButton { support.transition(session) { openAdvancedPageTwo(player, session) } })
        menu.setButton(27, buttonFactory.backButton { support.transition(session) { openAdvancedPageTwo(player, session) } })

        refreshButtons(menu, player, session)
        menu.open(player)
    }

    private fun refreshButtons(menu: Menu, player: Player, session: ItemEditSession) {
        val item = session.editableItem
        val removedEffects = DeathProtectionMenuSupport.removedEffects(item)
        val applyEffects = DeathProtectionMenuSupport.applyEffectEntries(item)

        menu.setButton(support.editableSlot, buttonFactory.editablePreviewButton(item))
        menu.setButton(31, buildToggleButton(player, session))
        menu.setButton(38, buildSoundButton(player, session))
        menu.setButton(39, buildClearAllEffectsButton(player, session))
        menu.setButton(40, buttonFactory.statefulSummaryButton(
            material = Material.LAVA_BUCKET,
            active = removedEffects.isNotEmpty(),
            activeName = "<!i><#C7A300>◎ <#FFD700>Снятие конкретных эффектов: <#00FF40>${removedEffects.size}",
            inactiveName = "<!i><#C7A300>⭘ <#FFD700>Снятие конкретных эффектов: <#FF1500>Нет",
            emptyLore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить"),
            selectedEntriesLore = removedEffects.map {
                "<!i><#C7A300> ● <#FFE68A>${PotionItemSupport.displayName(it)} "
            },
            action = { support.transition(session) { openRemoveEffectsPage(player, session) } }
        ))
        menu.setButton(41, buildRandomTeleportButton(player, session))
        menu.setButton(42, buttonFactory.statefulSummaryButton(
            material = Material.BREWING_STAND,
            active = applyEffects.isNotEmpty(),
            activeName = "<!i><#C7A300>◎ <#FFD700>Наложение эффектов: <#00FF40>${applyEffects.size}",
            inactiveName = "<!i><#C7A300>⭘ <#FFD700>Наложение эффектов: <#FF1500>Нет",
            emptyLore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить"),
            selectedEntriesLore = applyEffects.map { entry ->
                "<!i><#C7A300> ● <#FFE68A>${PotionItemSupport.displayName(entry.effect.type)} <#FFF3E0>${entry.effect.amplifier + 1} <#C7A300>[<#FFD700>${formatChancePercent(entry.probability)}%<#C7A300>] "
            },
            action = { support.transition(session) { openApplyEffectsPage(player, session) } }
        ))
    }

    private fun buildToggleButton(player: Player, session: ItemEditSession) = buttonFactory.toggleButton(
        material = Material.TOTEM_OF_UNDYING,
        enabled = DeathProtectionMenuSupport.isEnabled(session.editableItem),
        enabledName = "<!i><#C7A300>◎ <#FFD700>Защита от смерти: <#00FF40>Вкл",
        disabledName = "<!i><#C7A300>⭘ <#FFD700>Защита от смерти: <#FF1500>Выкл",
        lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить"),
        itemModifier = {
            buttonFactory.hideAttributes().invoke(this)
            this
        },
        action = { event ->
            val enabled = DeathProtectionMenuSupport.isEnabled(session.editableItem)
            if (enabled) {
                DeathProtectionMenuSupport.clearAll(session.editableItem)
            } else {
                DeathProtectionMenuSupport.setEnabled(session.editableItem, true)
            }
            refreshButtons(event.menu, player, session)
        }
    )

    private fun buildSoundButton(player: Player, session: ItemEditSession): Button {
        val sound = DeathProtectionMenuSupport.soundKey(session.editableItem)?.asString()
        val active = sound != null
        return buttonFactory.applyResetButton(
            material = Material.MUSIC_DISC_13,
            active = active,
            activeName = "<!i><#C7A300>◎ <#FFD700>Звук: <#00FF40>${formatSoundKey(sound)}",
            inactiveName = "<!i><#C7A300>⭘ <#FFD700>Звук: <#FF1500>Обычный",
            activeLore = SOUND_LORE,
            inactiveLore = SOUND_LORE,
            itemModifier = {
                buttonFactory.hideJukeboxTooltip(Material.MUSIC_DISC_13).invoke(this)
                this
            },
            onApply = {
                support.transition(session) {
                    requestApplyInput(player, session, EditorApplyKind.DEATH_PROTECTION_SOUND, ::open)
                }
            },
            onReset = { event ->
                DeathProtectionMenuSupport.clearSound(session.editableItem)
                refreshButtons(event.menu, player, session)
            }
        )
    }

    private fun buildClearAllEffectsButton(player: Player, session: ItemEditSession) = buttonFactory.toggleButton(
        material = Material.MILK_BUCKET,
        enabled = DeathProtectionMenuSupport.hasClearAllEffects(session.editableItem),
        enabledName = "<!i><#C7A300>◎ <#FFD700>Снятие всех эффектов: <#00FF40>Вкл",
        disabledName = "<!i><#C7A300>⭘ <#FFD700>Снятие всех эффектов: <#FF1500>Выкл",
        lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить"),
        action = { event ->
            val next = !DeathProtectionMenuSupport.hasClearAllEffects(session.editableItem)
            DeathProtectionMenuSupport.setClearAllEffects(session.editableItem, next)
            refreshButtons(event.menu, player, session)
        }
    )

    private fun buildRandomTeleportButton(player: Player, session: ItemEditSession): Button {
        val diameter = DeathProtectionMenuSupport.randomTeleportDiameter(session.editableItem)
        val active = diameter != null
        return buttonFactory.applyResetButton(
            material = Material.CHORUS_FRUIT,
            active = active,
            activeName = "<!i><#C7A300>◎ <#FFD700>Случайный телепорт: <#00FF40>${formatDiameter(diameter)}",
            inactiveName = "<!i><#C7A300>⭘ <#FFD700>Случайный телепорт: <#FF1500>Нет",
            activeLore = TELEPORT_LORE,
            inactiveLore = TELEPORT_LORE,
            itemModifier = {
                buttonFactory.zeroFoodPreview().invoke(this)
                this
            },
            onApply = {
                support.transition(session) {
                    requestApplyInput(player, session, EditorApplyKind.DEATH_PROTECTION_RANDOM_TELEPORT_DIAMETER, ::open)
                }
            },
            onReset = { event ->
                DeathProtectionMenuSupport.clearRandomTeleport(session.editableItem)
                refreshButtons(event.menu, player, session)
            }
        )
    }

    private fun formatSoundKey(soundKey: String?): String = soundKey?.removePrefix("minecraft:") ?: "Обычный"

    private fun formatDiameter(value: Float?): String {
        if (value == null) return "Нет"
        val rounded = value.toInt()
        return if (value == rounded.toFloat()) rounded.toString() else value.toString()
    }

    private fun formatChancePercent(probability: Float): String {
        val percent = probability * 100f
        val rounded = round(percent)
        return if (abs(percent - rounded) < 0.0001f) {
            rounded.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", percent)
        }
    }

    companion object {
        private val SOUND_LORE = listOf(
            "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
            "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
            "",
            "<!i><#FFD700>После нажатия:",
            "<!i><#C7A300> ● <#FFF3E0>/apply <звук> <#C7A300>- <#FFE68A>задать ",
            "<!i><#C7A300> ● <#FFF3E0>/apply cancel <#C7A300>- <#FFE68A>отмена ",
            ""
        )

        private val TELEPORT_LORE = listOf(
            "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
            "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
            "",
            "<!i><#FFD700>Назначение:",
            "<!i><#C7A300> ● <#FFE68A>Телепортирует на <#FFF3E0>случайную ",
            "<!i><#C7A300> ● <#FFE68A>точку в указанном радиусе. ",
            "",
            "<!i><#FFD700>После нажатия:",
            "<!i><#C7A300> ● <#FFF3E0>/apply <число> <#C7A300>- <#FFE68A>задать ",
            "<!i><#C7A300> ● <#FFF3E0>/apply cancel <#C7A300>- <#FFE68A>отмена ",
            ""
        )
    }
}
