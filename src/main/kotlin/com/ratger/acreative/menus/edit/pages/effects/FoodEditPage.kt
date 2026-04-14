package com.ratger.acreative.menus.edit.pages.effects

import com.ratger.acreative.itemedit.effects.ConsumableComponentSupport
import com.ratger.acreative.itemedit.effects.EdibleMenuSupport
import com.ratger.acreative.itemedit.effects.FoodComponentSupport
import com.ratger.acreative.itemedit.potion.PotionItemSupport
import com.ratger.acreative.itemedit.usecooldown.UseCooldownSupport
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.edit.ItemEditMenuSupport
import com.ratger.acreative.menus.edit.ItemEditSession
import com.ratger.acreative.itemedit.apply.core.EditorApplyKind
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation
import org.bukkit.Material
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.Menu
import ru.violence.coreapi.bukkit.api.menu.MenuRows
import ru.violence.coreapi.bukkit.api.menu.button.Button
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

class FoodEditPage(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val openRemoveEffectsPage: (Player, ItemEditSession, (Player, ItemEditSession) -> Unit) -> Unit,
    private val openApplyEffectsPage: (Player, ItemEditSession, (Player, ItemEditSession) -> Unit) -> Unit,
    private val requestApplyInput: (Player, ItemEditSession, EditorApplyKind, (Player, ItemEditSession) -> Unit) -> Unit
) {
    private val blackSlots = setOf(0, 8, 9, 12, 14, 17, 18, 26, 27, 35, 36, 44, 45, 53)

    fun open(player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit) {
        val menu = support.buildMenu(
            title = "<!i>▍ Редактор → Съедобность",
            menuSize = 54,
            rows = MenuRows.SIX,
            interactiveTopSlots = setOf(18, 27, 29, 30, 31, 32, 33, 38, 39, 40, 41, 42, 48, 50),
            session = session
        )

        support.fillBase(menu, 54, blackSlots)
        menu.setButton(18, buttonFactory.backButton { support.transition(session) { openBack(player, session) } })
        menu.setButton(27, buttonFactory.backButton { support.transition(session) { openBack(player, session) } })

        refreshButtons(menu, player, session, openBack)
        menu.open(player)
    }

    private fun refreshButtons(menu: Menu, player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit) {
        val item = session.editableItem
        val removedEffects = ConsumableComponentSupport.removedEffects(item)
        val applyEffects = ConsumableComponentSupport.applyEffectEntries(item)

        menu.setButton(support.editableSlot, buttonFactory.editablePreviewButton(item))
        menu.setButton(29, buildEdibleToggleButton(player, session, openBack))
        menu.setButton(30, buildAlwaysEatButton(player, session, openBack))
        menu.setButton(31, buildNutritionButton(player, session, openBack))
        menu.setButton(32, buildSaturationButton(player, session, openBack))
        menu.setButton(33, buildParticlesButton(player, session, openBack))
        menu.setButton(38, buildSoundButton(player, session, openBack))
        menu.setButton(39, buildClearAllEffectsButton(player, session, openBack))
        menu.setButton(40, buttonFactory.statefulSummaryButton(
            material = Material.LAVA_BUCKET,
            active = removedEffects.isNotEmpty(),
            activeName = "<!i><#C7A300>◎ <#FFD700>Снятие конкретных эффектов: <#00FF40>${removedEffects.size}",
            inactiveName = "<!i><#C7A300>⭘ <#FFD700>Снятие конкретных эффектов: <#FF1500>Нет",
            emptyLore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить"),
            selectedEntriesLore = removedEffects.map {
                "<!i><#C7A300> ● <#FFE68A>${PotionItemSupport.displayName(it)} "
            },
            action = { support.transition(session) { openRemoveEffectsPage(player, session, openBack) } }
        ))
        menu.setButton(41, buildRandomTeleportButton(player, session, openBack))
        menu.setButton(42, buttonFactory.statefulSummaryButton(
            material = Material.BREWING_STAND,
            active = applyEffects.isNotEmpty(),
            activeName = "<!i><#C7A300>◎ <#FFD700>Наложение эффектов: <#00FF40>${applyEffects.size}",
            inactiveName = "<!i><#C7A300>⭘ <#FFD700>Наложение эффектов: <#FF1500>Нет",
            emptyLore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить"),
            selectedEntriesLore = applyEffects.map { entry ->
                "<!i><#C7A300> ● <#FFE68A>${PotionItemSupport.displayName(entry.effect.type)} <#FFF3E0>${entry.effect.amplifier + 1} <#C7A300>[<#FFD700>${formatChancePercent(entry.probability)}%<#C7A300>] "
            },
            action = { support.transition(session) { openApplyEffectsPage(player, session, openBack) } }
        ))
        menu.setButton(48, buildAnimationButton(player, session, openBack))
        menu.setButton(50, buildConsumeSecondsButton(player, session, openBack))
    }

    private fun buildEdibleToggleButton(player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit) =
        buttonFactory.toggleButton(
            material = Material.APPLE,
            enabled = EdibleMenuSupport.isEnabled(session.editableItem),
            enabledName = "<!i><#C7A300>◎ <#FFD700>Съедобность: <#00FF40>Вкл",
            disabledName = "<!i><#C7A300>⭘ <#FFD700>Съедобность: <#FF1500>Выкл",
            lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить"),
            itemModifier = {
                buttonFactory.zeroFoodPreview().invoke(this)
                this
            },
            action = { event ->
                if (EdibleMenuSupport.isEnabled(session.editableItem)) {
                    EdibleMenuSupport.clearAll(session.editableItem)
                } else {
                    EdibleMenuSupport.ensureEnabledWithDefaults(session.editableItem)
                }
                refreshButtons(event.menu, player, session, openBack)
            }
        )

    private fun buildAlwaysEatButton(player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit) =
        buttonFactory.toggleButton(
            material = Material.GOLDEN_APPLE,
            enabled = FoodComponentSupport.canAlwaysEat(session.editableItem),
            enabledName = "<!i><#C7A300>◎ <#FFD700>Можно съесть всегда: <#00FF40>Вкл",
            disabledName = "<!i><#C7A300>⭘ <#FFD700>Можно съесть всегда: <#FF1500>Выкл",
            lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить"),
            action = { event ->
                val next = !FoodComponentSupport.canAlwaysEat(session.editableItem)
                if (next && !EdibleMenuSupport.isEnabled(session.editableItem)) {
                    EdibleMenuSupport.ensureEnabledWithDefaults(session.editableItem)
                }
                FoodComponentSupport.setCanAlwaysEat(session.editableItem, next)
                refreshButtons(event.menu, player, session, openBack)
            }
        )

    private fun buildNutritionButton(player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit) =
        buttonFactory.applyResetButton(
            material = Material.CARROT,
            active = FoodComponentSupport.nutrition(session.editableItem) > 0,
            activeName = "<!i><#C7A300>◎ <#FFD700>Сытость: <#00FF40>+${FoodComponentSupport.nutrition(session.editableItem)}",
            inactiveName = "<!i><#C7A300>⭘ <#FFD700>Сытость: <#FF1500>Обычная",
            activeLore = VALUE_LORE,
            inactiveLore = VALUE_LORE,
            onApply = {
                support.transition(session) {
                    requestApplyInput(player, session, EditorApplyKind.FOOD_NUTRITION) { reopenPlayer, reopenSession ->
                        open(reopenPlayer, reopenSession, openBack)
                    }
                }
            },
            onReset = { event ->
                FoodComponentSupport.setNutrition(session.editableItem, 0)
                refreshButtons(event.menu, player, session, openBack)
            }
        )

    private fun buildSaturationButton(player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit) =
        buttonFactory.applyResetButton(
            material = Material.GOLDEN_CARROT,
            active = FoodComponentSupport.saturation(session.editableItem) > 0f,
            activeName = "<!i><#C7A300>◎ <#FFD700>Насыщение: <#00FF40>+${formatFloat(FoodComponentSupport.saturation(session.editableItem))}",
            inactiveName = "<!i><#C7A300>⭘ <#FFD700>Насыщение: <#FF1500>Обычное",
            activeLore = VALUE_LORE,
            inactiveLore = VALUE_LORE,
            onApply = {
                support.transition(session) {
                    requestApplyInput(player, session, EditorApplyKind.FOOD_SATURATION) { reopenPlayer, reopenSession ->
                        open(reopenPlayer, reopenSession, openBack)
                    }
                }
            },
            onReset = { event ->
                FoodComponentSupport.setSaturation(session.editableItem, 0f)
                refreshButtons(event.menu, player, session, openBack)
            }
        )

    private fun buildParticlesButton(player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit) =
        buttonFactory.toggleButton(
            material = Material.GLOW_BERRIES,
            enabled = ConsumableComponentSupport.hasParticles(session.editableItem),
            enabledName = "<!i><#C7A300>◎ <#FFD700>Партиклы еды: <#00FF40>Вкл",
            disabledName = "<!i><#C7A300>⭘ <#FFD700>Партиклы еды: <#FF1500>Выкл",
            lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить"),
            action = { event ->
                val next = !ConsumableComponentSupport.hasParticles(session.editableItem)
                if (next && !EdibleMenuSupport.isEnabled(session.editableItem)) {
                    EdibleMenuSupport.ensureEnabledWithDefaults(session.editableItem)
                }
                ConsumableComponentSupport.setHasParticles(session.editableItem, next)
                refreshButtons(event.menu, player, session, openBack)
            }
        )

    private fun buildSoundButton(player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit): Button {
        val sound = ConsumableComponentSupport.soundKey(session.editableItem)?.asString()?.removePrefix("minecraft:")
        return buttonFactory.applyResetButton(
            material = Material.MUSIC_DISC_13,
            active = sound != null,
            activeName = "<!i><#C7A300>◎ <#FFD700>Звук: <#00FF40>${sound ?: "Обычный"}",
            inactiveName = "<!i><#C7A300>⭘ <#FFD700>Звук: <#FF1500>Обычный",
            activeLore = SOUND_LORE,
            inactiveLore = SOUND_LORE,
            itemModifier = {
                buttonFactory.hideJukeboxTooltip(Material.MUSIC_DISC_13).invoke(this)
                this
            },
            onApply = {
                support.transition(session) {
                    requestApplyInput(player, session, EditorApplyKind.CONSUMABLE_SOUND) { reopenPlayer, reopenSession ->
                        open(reopenPlayer, reopenSession, openBack)
                    }
                }
            },
            onReset = { event ->
                ConsumableComponentSupport.clearSound(session.editableItem)
                refreshButtons(event.menu, player, session, openBack)
            }
        )
    }

    private fun buildClearAllEffectsButton(player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit) =
        buttonFactory.toggleButton(
            material = Material.MILK_BUCKET,
            enabled = ConsumableComponentSupport.hasClearAllEffects(session.editableItem),
            enabledName = "<!i><#C7A300>◎ <#FFD700>Снятие всех эффектов: <#00FF40>Вкл",
            disabledName = "<!i><#C7A300>⭘ <#FFD700>Снятие всех эффектов: <#FF1500>Выкл",
            lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить"),
            action = { event ->
                val next = !ConsumableComponentSupport.hasClearAllEffects(session.editableItem)
                if (next && !EdibleMenuSupport.isEnabled(session.editableItem)) {
                    EdibleMenuSupport.ensureEnabledWithDefaults(session.editableItem)
                }
                ConsumableComponentSupport.setClearAllEffects(session.editableItem, next)
                refreshButtons(event.menu, player, session, openBack)
            }
        )

    private fun buildRandomTeleportButton(player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit): Button {
        val diameter = ConsumableComponentSupport.randomTeleportDiameter(session.editableItem)
        return buttonFactory.applyResetButton(
            material = Material.CHORUS_FRUIT,
            active = diameter != null,
            activeName = "<!i><#C7A300>◎ <#FFD700>Случайный телепорт: <#00FF40>${formatFloat(diameter)}",
            inactiveName = "<!i><#C7A300>⭘ <#FFD700>Случайный телепорт: <#FF1500>Нет",
            activeLore = TELEPORT_LORE,
            inactiveLore = TELEPORT_LORE,
            itemModifier = {
                buttonFactory.zeroFoodPreview().invoke(this)
                this
            },
            onApply = {
                support.transition(session) {
                    requestApplyInput(player, session, EditorApplyKind.CONSUMABLE_RANDOM_TELEPORT_DIAMETER) { reopenPlayer, reopenSession ->
                        open(reopenPlayer, reopenSession, openBack)
                    }
                }
            },
            onReset = { event ->
                ConsumableComponentSupport.clearRandomTeleport(session.editableItem)
                refreshButtons(event.menu, player, session, openBack)
            }
        )
    }

    private fun buildAnimationButton(player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit): Button {
        val options = listOf(
            MenuButtonFactory.ListButtonOption(ItemUseAnimation.EAT, "Еда"),
            MenuButtonFactory.ListButtonOption(ItemUseAnimation.DRINK, "Напиток"),
            MenuButtonFactory.ListButtonOption(ItemUseAnimation.BLOCK, "Щит"),
            MenuButtonFactory.ListButtonOption(ItemUseAnimation.BOW, "Лук"),
            MenuButtonFactory.ListButtonOption(ItemUseAnimation.CROSSBOW, "Арбалет"),
            MenuButtonFactory.ListButtonOption(ItemUseAnimation.SPEAR, "Копьё"),
            MenuButtonFactory.ListButtonOption(ItemUseAnimation.SPYGLASS, "Подзорная труба"),
            MenuButtonFactory.ListButtonOption(ItemUseAnimation.TOOT_HORN, "Рог"),
            MenuButtonFactory.ListButtonOption(ItemUseAnimation.BRUSH, "Кисточка"),
            MenuButtonFactory.ListButtonOption(ItemUseAnimation.NONE, "Нет")
        )
        val selectedAnimation = ConsumableComponentSupport.animation(session.editableItem)
        val selectedIndex = options.indexOfFirst { it.value == selectedAnimation }.takeIf { it >= 0 } ?: 0

        return buttonFactory.listButton(
            material = Material.ENDER_EYE,
            options = options,
            selectedIndex = selectedIndex,
            titleBuilder = { _, index ->
                when (index) {
                    0 -> "<!i><#C7A300>① <#FFD700>Анимация: <#FFF3E0>Еда"
                    1 -> "<!i><#C7A300>② <#FFD700>Анимация: <#FFF3E0>Напиток"
                    2 -> "<!i><#C7A300>③ <#FFD700>Анимация: <#FFF3E0>Щит"
                    3 -> "<!i><#C7A300>④ <#FFD700>Анимация: <#FFF3E0>Лук"
                    4 -> "<!i><#C7A300>⑤ <#FFD700>Анимация: <#FFF3E0>Арбалет"
                    5 -> "<!i><#C7A300>⑥ <#FFD700>Анимация: <#FFF3E0>Копьё"
                    6 -> "<!i><#C7A300>⑦ <#FFD700>Анимация: <#FFF3E0>Подзорная труба"
                    7 -> "<!i><#C7A300>⑧ <#FFD700>Анимация: <#FFF3E0>Рог"
                    8 -> "<!i><#C7A300>⑨ <#FFD700>Анимация: <#FFF3E0>Кисточка"
                    else -> "<!i><#C7A300>⑩ <#FFD700>Анимация: <#FFF3E0>Нет"
                }
            },
            beforeOptionsLore = listOf(
                "<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить",
                ""
            ),
            afterOptionsLore = listOf(
                "",
                "<!i><#FFD700>Назначение:",
                "<!i><#C7A300> ● <#FFE68A>Влияет на анимацию ",
                "<!i><#C7A300> ● <#FFE68A>предмета при <#FFF3E0>поедании. ",
                ""
            ),
            action = { event, newIndex ->
                if (!EdibleMenuSupport.isEnabled(session.editableItem)) {
                    EdibleMenuSupport.ensureEnabledWithDefaults(session.editableItem)
                }
                ConsumableComponentSupport.setAnimation(session.editableItem, options[newIndex].value)
                refreshButtons(event.menu, player, session, openBack)
            }
        )
    }

    private fun buildConsumeSecondsButton(player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit): Button {
        val consumeSeconds = ConsumableComponentSupport.consumeSeconds(session.editableItem)
        val defaultSeconds = ConsumableComponentSupport.defaultConsumeSeconds(session.editableItem)
        val active = consumeSeconds != null && abs(consumeSeconds - defaultSeconds) > 0.0001f
        return buttonFactory.applyResetButton(
            material = Material.SUGAR,
            active = active,
            activeName = "<!i><#C7A300>◎ <#FFD700>Скорость поедания: <#00FF40>${UseCooldownSupport.displaySeconds(consumeSeconds ?: defaultSeconds)}",
            inactiveName = "<!i><#C7A300>⭘ <#FFD700>Скорость поедания: <#FF1500>Обычная",
            activeLore = CONSUME_SECONDS_LORE,
            inactiveLore = CONSUME_SECONDS_LORE,
            itemModifier = {
                buttonFactory.zeroFoodPreview().invoke(this)
                this
            },
            onApply = {
                support.transition(session) {
                    requestApplyInput(player, session, EditorApplyKind.CONSUMABLE_CONSUME_SECONDS) { reopenPlayer, reopenSession ->
                        open(reopenPlayer, reopenSession, openBack)
                    }
                }
            },
            onReset = { event ->
                if (!EdibleMenuSupport.isEnabled(session.editableItem)) {
                    EdibleMenuSupport.ensureEnabledWithDefaults(session.editableItem)
                }
                ConsumableComponentSupport.resetConsumeSeconds(session.editableItem)
                refreshButtons(event.menu, player, session, openBack)
            }
        )
    }

    private fun formatFloat(value: Float?): String {
        if (value == null) return "Нет"
        val rounded = value.toInt()
        return if (value == rounded.toFloat()) rounded.toString() else String.format(Locale.US, "%.1f", value)
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
        private val VALUE_LORE = listOf(
            "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
            "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
            "",
            "<!i><#FFD700>После нажатия:",
            "<!i><#C7A300> ● <#FFF3E0>/apply <число> <#C7A300>- <#FFE68A>задать ",
            "<!i><#C7A300> ● <#FFF3E0>/apply cancel <#C7A300>- <#FFE68A>отмена ",
            ""
        )

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

        private val CONSUME_SECONDS_LORE = listOf(
            "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
            "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
            "",
            "<!i><#FFD700>После нажатия:",
            "<!i><#C7A300> ● <#FFF3E0>/apply <секунд> <#C7A300>- <#FFE68A>задать ",
            "<!i><#C7A300> ● <#FFF3E0>/apply cancel <#C7A300>- <#FFE68A>отмена ",
            ""
        )
    }
}
