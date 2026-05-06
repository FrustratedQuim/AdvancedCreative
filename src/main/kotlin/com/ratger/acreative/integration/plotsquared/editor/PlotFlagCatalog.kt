package com.ratger.acreative.integration.plotsquared.editor

import com.plotsquared.core.plot.flag.PlotFlag
import com.ratger.acreative.integration.plotsquared.flags.PlotPaintFlag

enum class PlotFlagEntryKind {
    PRESET,
    TEXT,
    NUMBER,
    TIMED_NUMBER,
    COLLECTION,
    TITLE_PART
}

enum class PlotFlagCollectionAddMode {
    COMMAND_ONLY,
    COMMAND_AND_MENU
}

enum class PlotTitlePart {
    TITLE,
    SUBTITLE
}

data class PlotFlagOption(
    val label: String,
    val value: String
)

data class PlotFlagDefinition(
    val key: String,
    val groupKey: String,
    val title: String,
    val description: String,
    val itemModel: String,
    val kind: PlotFlagEntryKind,
    val flagClassCandidates: List<String>,
    val presetOptions: List<PlotFlagOption> = emptyList(),
    val collectionAddMode: PlotFlagCollectionAddMode? = null,
    val titlePart: PlotTitlePart? = null
) {
    @Suppress("UNCHECKED_CAST")
    fun resolveFlagClass(): Class<out PlotFlag<*, *>>? {
        flagClassCandidates.forEach { candidate ->
            val resolved = when (candidate) {
                CUSTOM_PLOT_PAINT -> PlotPaintFlag.TRUE.javaClass
                else -> runCatching { Class.forName(candidate) }.getOrNull()
            } ?: return@forEach

            if (PlotFlag::class.java.isAssignableFrom(resolved)) {
                return resolved as Class<out PlotFlag<*, *>>
            }
        }
        return null
    }

    companion object {
        private const val CUSTOM_PLOT_PAINT = "__custom_plot_paint__"

        private fun impl(name: String): String = "com.plotsquared.core.plot.flag.implementations.$name"

        private fun preset(
            key: String,
            title: String,
            description: String,
            itemModel: String,
            className: String,
            options: List<PlotFlagOption>
        ) = PlotFlagDefinition(
            key = key,
            groupKey = key,
            title = title,
            description = description,
            itemModel = itemModel,
            kind = PlotFlagEntryKind.PRESET,
            flagClassCandidates = listOf(impl(className)),
            presetOptions = options
        )

        private fun booleanPreset(
            key: String,
            title: String,
            description: String,
            itemModel: String,
            className: String,
            trueLabel: String,
            falseLabel: String,
            noneLabel: String = "Не задано"
        ) = preset(
            key = key,
            title = title,
            description = description,
            itemModel = itemModel,
            className = className,
            options = listOf(
                PlotFlagOption(noneLabel, "none"),
                PlotFlagOption(trueLabel, "true"),
                PlotFlagOption(falseLabel, "false")
            )
        )

        private fun text(
            key: String,
            title: String,
            description: String,
            itemModel: String,
            className: String
        ) = PlotFlagDefinition(
            key = key,
            groupKey = key,
            title = title,
            description = description,
            itemModel = itemModel,
            kind = PlotFlagEntryKind.TEXT,
            flagClassCandidates = listOf(impl(className))
        )

        private fun number(
            key: String,
            title: String,
            description: String,
            itemModel: String,
            className: String
        ) = PlotFlagDefinition(
            key = key,
            groupKey = key,
            title = title,
            description = description,
            itemModel = itemModel,
            kind = PlotFlagEntryKind.NUMBER,
            flagClassCandidates = listOf(impl(className))
        )

        private fun timedNumber(
            key: String,
            title: String,
            description: String,
            itemModel: String,
            className: String
        ) = PlotFlagDefinition(
            key = key,
            groupKey = key,
            title = title,
            description = description,
            itemModel = itemModel,
            kind = PlotFlagEntryKind.TIMED_NUMBER,
            flagClassCandidates = listOf(impl(className))
        )

        private fun collection(
            key: String,
            title: String,
            description: String,
            itemModel: String,
            className: String,
            addMode: PlotFlagCollectionAddMode
        ) = PlotFlagDefinition(
            key = key,
            groupKey = key,
            title = title,
            description = description,
            itemModel = itemModel,
            kind = PlotFlagEntryKind.COLLECTION,
            flagClassCandidates = listOf(impl(className)),
            collectionAddMode = addMode
        )

        private fun plotTitlePart(
            key: String,
            title: String,
            description: String,
            part: PlotTitlePart
        ) = PlotFlagDefinition(
            key = key,
            groupKey = "plot-title",
            title = title,
            description = description,
            itemModel = "minecraft:paper",
            kind = PlotFlagEntryKind.TITLE_PART,
            flagClassCandidates = listOf(impl("PlotTitleFlag")),
            titlePart = part
        )

        val definitions: List<PlotFlagDefinition> = listOf(
            preset(
                key = "time",
                title = "Время",
                description = "Настраивает время суток на участке.",
                itemModel = "minecraft:clock",
                className = "TimeFlag",
                options = listOf(
                    PlotFlagOption("Не задано", "none"),
                    PlotFlagOption("Рассвет", "0"),
                    PlotFlagOption("Утро", "1000"),
                    PlotFlagOption("День", "6000"),
                    PlotFlagOption("Закат", "12000"),
                    PlotFlagOption("Ночь", "15000"),
                    PlotFlagOption("Полночь", "18000")
                )
            ),
            preset(
                key = "weather",
                title = "Погода",
                description = "Настраивает погоду на участке.",
                itemModel = "minecraft:sunflower",
                className = "WeatherFlag",
                options = listOf(
                    PlotFlagOption("Не задана", "none"),
                    PlotFlagOption("Солнце", "clear"),
                    PlotFlagOption("Дождь", "rain")
                )
            ),
            preset(
                key = "music",
                title = "Музыка",
                description = "Настраивает музыку, проигрываемую на участке.",
                itemModel = "minecraft:music_disc_otherside",
                className = "MusicFlag",
                options = listOf(
                    PlotFlagOption("Не задана", "none"),
                    PlotFlagOption("Otherside", "music_disc_otherside"),
                    PlotFlagOption("Pigstep", "music_disc_pigstep"),
                    PlotFlagOption("Relic", "music_disc_relic"),
                    PlotFlagOption("Creator", "music_disc_creator"),
                    PlotFlagOption("Cat", "music_disc_cat"),
                    PlotFlagOption("Blocks", "music_disc_blocks"),
                    PlotFlagOption("Chirp", "music_disc_chirp"),
                    PlotFlagOption("Far", "music_disc_far"),
                    PlotFlagOption("Mall", "music_disc_mall"),
                    PlotFlagOption("Mellohi", "music_disc_mellohi"),
                    PlotFlagOption("Stal", "music_disc_stal"),
                    PlotFlagOption("Strad", "music_disc_strad"),
                    PlotFlagOption("Wait", "music_disc_wait"),
                    PlotFlagOption("Ward", "music_disc_ward")
                )
            ),
            booleanPreset("titles", "Показ заголовка", "Переключает показ заголовка при входе на участок.", "minecraft:name_tag", "TitlesFlag", "Включить", "Выключить"),
            plotTitlePart("plot-title-title", "Заголовок", "Настраивает текст заголовка участка.", PlotTitlePart.TITLE),
            plotTitlePart("plot-title-subtitle", "Подзаголовок", "Настраивает текст подзаголовка участка.", PlotTitlePart.SUBTITLE),
            text("greeting", "Приветствие", "Настраивает сообщение при входе на участок.", "minecraft:oak_sign", "GreetingFlag"),
            text("farewell", "Прощание", "Настраивает сообщение при выходе с участка.", "minecraft:spruce_sign", "FarewellFlag"),
            booleanPreset("notify-enter", "Уведомление о входе", "Переключает уведомления о входе игрока на участок.", "minecraft:bell", "NotifyEnterFlag", "Включить", "Выключить"),
            booleanPreset("notify-leave", "Уведомление о выходе", "Переключает уведомления о выходе игрока с участка.", "minecraft:bell", "NotifyLeaveFlag", "Включить", "Выключить"),
            booleanPreset("hide-info", "Скрытие инфо", "Переключает возможность /plot info.", "minecraft:spyglass", "HideInfoFlag", "Скрыть", "Показывать"),
            booleanPreset("animal-interact", "Взаимодействие с мирными мобами", "Управляет взаимодействием гостей с мирными животными.", "minecraft:wheat", "AnimalInteractFlag", "Разрешить", "Запретить"),
            booleanPreset("animal-attack", "Убийство животных", "Управляет атакой гостей по мирным животным.", "minecraft:iron_sword", "AnimalAttackFlag", "Разрешить", "Запретить"),
            number("animal-cap", "Лимит животных", "Задаёт лимит мирных животных на участке.", "minecraft:cow_spawn_egg", "AnimalCapFlag"),
            booleanPreset("tamed-interact", "Взаимодействие с питомцами", "Управляет взаимодействием гостей с прирученными животными.", "minecraft:bone", "TamedInteractFlag", "Разрешить", "Запретить"),
            booleanPreset("tamed-attack", "Убийство питомцев", "Управляет атакой гостей по прирученным животным.", "minecraft:bone", "TamedAttackFlag", "Разрешить", "Запретить"),
            booleanPreset("villager-interact", "Взаимодействие с жителями", "Управляет взаимодействием гостей с жителями.", "minecraft:emerald", "VillagerInteractFlag", "Разрешить", "Запретить"),
            booleanPreset("hostile-interact", "Взаимодействие с монстрами", "Управляет взаимодействием гостей с монстрами.", "minecraft:spawner", "HostileInteractFlag", "Разрешить", "Запретить"),
            booleanPreset("hostile-attack", "Убийство монстров", "Управляет атакой гостей по монстрам.", "minecraft:zombie_head", "HostileAttackFlag", "Разрешить", "Запретить"),
            number("hostile-cap", "Лимит монстров", "Задаёт лимит монстров на участке.", "minecraft:zombie_spawn_egg", "HostileCapFlag"),
            booleanPreset("mob-place", "Спавн мобов", "Управляет спавном существ гостями на участке.", "minecraft:sheep_spawn_egg", "MobPlaceFlag", "Разрешить", "Запретить"),
            number("mob-cap", "Лимит мобов", "Задаёт общий лимит живых существ на участке.", "minecraft:lead", "MobCapFlag"),
            number("entity-cap", "Лимит сущностей", "Задаёт общий лимит всех сущностей на участке.", "minecraft:ender_eye", "EntityCapFlag"),
            booleanPreset("entity-change-block", "Изменение блоков сущностями", "Управляет изменением блоков сущностями на участке.", "minecraft:enderman_spawn_egg", "EntityChangeBlockFlag", "Разрешить", "Запретить"),
            booleanPreset("vehicle-use", "Использование транспорта", "Управляет использованием транспорта гостями.", "minecraft:minecart", "VehicleUseFlag", "Разрешить", "Запретить"),
            booleanPreset("vehicle-place", "Установка транспорта", "Управляет установкой транспорта гостями.", "minecraft:oak_boat", "VehiclePlaceFlag", "Разрешить", "Запретить"),
            booleanPreset("vehicle-break", "Ломать транспорт", "Управляет разрушением транспорта гостями.", "minecraft:minecart", "VehicleBreakFlag", "Разрешить", "Запретить"),
            number("vehicle-cap", "Лимит транспорта", "Задаёт лимит транспорта на участке.", "minecraft:minecart", "VehicleCapFlag"),
            booleanPreset("misc-interact", "Взаимодействие со стойкой", "Управляет взаимодействием гостей со стойками для брони.", "minecraft:armor_stand", "MiscInteractFlag", "Разрешить", "Запретить"),
            booleanPreset("misc-place", "Установка стойки", "Управляет установкой стоек для брони гостями.", "minecraft:armor_stand", "MiscPlaceFlag", "Разрешить", "Запретить"),
            booleanPreset("misc-break", "Разрушение стойки", "Управляет разрушением стоек для брони гостями.", "minecraft:armor_stand", "MiscBreakFlag", "Разрешить", "Запретить"),
            number("misc-cap", "Лимит стоек", "Задаёт лимит стоек для брони на участке.", "minecraft:armor_stand", "MiscCapFlag"),
            collection("use", "Взаимодействие с блоками", "Настраивает список блоков, доступных гостям для взаимодействия.", "minecraft:lever", "UseFlag", PlotFlagCollectionAddMode.COMMAND_AND_MENU),
            collection("place", "Установка блоков", "Настраивает список блоков, доступных гостям для установки.", "minecraft:scaffolding", "PlaceFlag", PlotFlagCollectionAddMode.COMMAND_AND_MENU),
            collection("break", "Разрушение блоков", "Настраивает список блоков, доступных гостям для разрушения.", "minecraft:iron_pickaxe", "BreakFlag", PlotFlagCollectionAddMode.COMMAND_AND_MENU),
            booleanPreset("tile-drop", "Дроп блоков", "Переключает выпадение предметов при разрушении блоков.", "minecraft:chest", "TileDropFlag", "Включить", "Выключить"),
            booleanPreset("item-drop", "Выброс предметов", "Управляет выбрасыванием предметов гостями.", "minecraft:dropper", "ItemDropFlag", "Разрешить", "Запретить"),
            booleanPreset("drop-protection", "Подбор предметов", "Управляет защитой выброшенных предметов от подбора гостями.", "minecraft:hopper", "DropProtectionFlag", "Запретить", "Разрешить"),
            booleanPreset("hanging-place", "Установка рамок и картин", "Управляет установкой рамок и картин гостями.", "minecraft:item_frame", "HangingPlaceFlag", "Разрешить", "Запретить"),
            booleanPreset("hanging-break", "Разрушение рамок и картин", "Управляет разрушением рамок и картин гостями.", "minecraft:item_frame", "HangingBreakFlag", "Разрешить", "Запретить"),
            booleanPreset("edit-sign", "Редактирование табличек", "Управляет редактированием табличек гостями.", "minecraft:oak_sign", "EditSignFlag", "Разрешить", "Запретить"),
            booleanPreset("device-interact", "Простейшие механизмы", "Управляет активацией простых механизмов гостями.", "minecraft:stone_pressure_plate", "DeviceInteractFlag", "Разрешить", "Запретить"),
            booleanPreset("sculk-sensor-interact", "Использование скалка", "Управляет реакцией скалк-сенсоров на действия гостей.", "minecraft:sculk_sensor", "SculkSensorInteractFlag", "Реагирует", "Игнорирует"),
            booleanPreset("fishing", "Использование удочки", "Управляет использованием удочки гостями.", "minecraft:fishing_rod", "FishingFlag", "Разрешить", "Запретить"),
            booleanPreset("projectiles", "Снаряды", "Управляет использованием снарядов гостями.", "minecraft:arrow", "ProjectilesFlag", "Разрешить", "Запретить"),
            booleanPreset("pve", "PvE", "Переключает возможность мобов атаковать игроков на участке.", "minecraft:skeleton_skull", "PveFlag", "Включить", "Выключить"),
            booleanPreset("forcefield", "Силовое поле", "Переключает отталкивание гостей от участников участка.", "minecraft:shield", "ForcefieldFlag", "Включить", "Выключить"),
            booleanPreset("fly", "Возможность полёта", "Переключает полёт при входе на участок.", "minecraft:feather", "FlyFlag", "Включить", "Выключить"),
            booleanPreset("invincible", "Бессмертие", "Переключает получение урона игроками на участке.", "minecraft:totem_of_undying", "InvincibleFlag", "Включить", "Выключить"),
            booleanPreset("instabreak", "Мгновенная поломка", "Переключает мгновенное разрушение блоков в режиме выживания.", "minecraft:golden_pickaxe", "InstabreakFlag", "Включить", "Выключить"),
            timedNumber("feed", "Восстановление сытости", "Настраивает периодическое восстановление сытости игрокам на участке.", "minecraft:cooked_beef", "FeedFlag"),
            timedNumber("heal", "Восстановление здоровья", "Настраивает периодическое восстановление здоровья игрокам на участке.", "minecraft:golden_apple", "HealFlag"),
            preset(
                key = "gamemode",
                title = "Режим игры",
                description = "Настраивает режим игры при входе на участок.",
                itemModel = "minecraft:diamond_sword",
                className = "GamemodeFlag",
                options = listOf(
                    PlotFlagOption("Не задан", "none"),
                    PlotFlagOption("Выживание", "survival"),
                    PlotFlagOption("Творческий", "creative"),
                    PlotFlagOption("Приключение", "adventure"),
                    PlotFlagOption("Наблюдатель", "spectator")
                )
            ),
            preset(
                key = "guest-gamemode",
                title = "Режим игры гостей",
                description = "Настраивает режим игры для гостей участка.",
                itemModel = "minecraft:leather_boots",
                className = "GuestGamemodeFlag",
                options = listOf(
                    PlotFlagOption("Не задан", "none"),
                    PlotFlagOption("Выживание", "survival"),
                    PlotFlagOption("Творческий", "creative"),
                    PlotFlagOption("Приключение", "adventure"),
                    PlotFlagOption("Наблюдатель", "spectator")
                )
            ),
            booleanPreset("crop-grow", "Рост посевов", "Переключает рост посевов на участке.", "minecraft:wheat", "CropGrowFlag", "Включить", "Выключить"),
            booleanPreset("grass-grow", "Рост травы", "Переключает распространение дёрна на участке.", "minecraft:grass_block", "GrassGrowFlag", "Включить", "Выключить"),
            booleanPreset("kelp-grow", "Рост ламинарии", "Переключает рост ламинарии на участке.", "minecraft:kelp", "KelpGrowFlag", "Включить", "Выключить"),
            booleanPreset("vine-grow", "Рост лиан", "Переключает рост лиан на участке.", "minecraft:vine", "VineGrowFlag", "Включить", "Выключить"),
            booleanPreset("mycel-grow", "Рост мицелия", "Переключает распространение мицелия на участке.", "minecraft:mycelium", "MycelGrowFlag", "Включить", "Выключить"),
            booleanPreset("leaf-decay", "Опадение листвы", "Переключает естественное исчезновение листвы на участке.", "minecraft:oak_leaves", "LeafDecayFlag", "Включить", "Выключить"),
            booleanPreset("soil-dry", "Высыхание грядок", "Переключает высыхание грядок на участке.", "minecraft:farmland", "SoilDryFlag", "Включить", "Выключить"),
            booleanPreset("concrete-harden", "Формирование бетона", "Переключает превращение цемента в бетон на участке.", "minecraft:white_concrete_powder", "ConcreteHardenFlag", "Включить", "Выключить"),
            booleanPreset("copper-oxide", "Окисление меди", "Переключает окисление меди на участке.", "minecraft:copper_block", "CopperOxideFlag", "Включить", "Выключить"),
            booleanPreset("coral-dry", "Высыхание кораллов", "Переключает высыхание кораллов на участке.", "minecraft:tube_coral", "CoralDryFlag", "Включить", "Выключить"),
            booleanPreset("ice-form", "Формирование льда", "Переключает замерзание воды на участке.", "minecraft:ice", "IceFormFlag", "Включить", "Выключить"),
            booleanPreset("ice-melt", "Таяние льда", "Переключает таяние льда на участке.", "minecraft:packed_ice", "IceMeltFlag", "Включить", "Выключить"),
            booleanPreset("snow-form", "Формирование снега", "Переключает формирование снега на участке.", "minecraft:snow", "SnowFormFlag", "Включить", "Выключить"),
            booleanPreset("snow-melt", "Таяние снега", "Переключает таяние снега на участке.", "minecraft:snow_block", "SnowMeltFlag", "Включить", "Выключить"),
            booleanPreset("weaving-death-place", "Установка паутины", "Переключает появление паутины от эффекта плетения.", "minecraft:cobweb", "WeavingDeathPlace", "Включить", "Выключить"),
            booleanPreset("beacon-effects", "Эффекты маяка", "Переключает наложение эффектов маяка на игроков.", "minecraft:beacon", "BeaconEffectsFlag", "Включить", "Выключить"),
            booleanPreset("redstone", "Работа механизмов", "Переключает работу редстоуна на участке.", "minecraft:redstone", "RedstoneFlag", "Включить", "Выключить"),
            booleanPreset("redstone-clock", "Redstone-clock", "Управляет защитой редстоун-схем от удаления из-за clock-детекции.", "minecraft:repeater", "RedstoneClockFlag", "Включить", "Выключить"),
            booleanPreset("liquid-flow", "Течение жидкостей", "Переключает течение жидкостей на участке.", "minecraft:water_bucket", "LiquidFlowFlag", "Включить", "Выключить"),
            booleanPreset("disable-physics", "Работа физики", "Управляет физикой блоков и жидкостей на участке.", "minecraft:sand", "DisablePhysicsFlag", "Отключить", "Включить"),
            booleanPreset("block-burn", "Горение блоков", "Управляет сгоранием блоков на участке.", "minecraft:flint_and_steel", "BlockBurnFlag", "Разрешить", "Запретить"),
            booleanPreset("block-ignition", "Возгорание блоков", "Управляет возгоранием блоков на участке.", "minecraft:fire_charge", "BlockIgnitionFlag", "Разрешить", "Запретить"),
            booleanPreset("deny-exit", "Запрет выхода", "Управляет возможностью гостей покидать участок.", "minecraft:barrier", "DenyExitFlag", "Запретить выход", "Разрешить выход"),
            booleanPreset("deny-portals", "Создание порталов", "Управляет возможностью создания порталов на участке.", "minecraft:obsidian", "DenyPortalsFlag", "Запретить", "Разрешить"),
            booleanPreset("no-worldedit", "WorldEdit", "Управляет использованием WorldEdit на участке.", "minecraft:wooden_axe", "NoWorldeditFlag", "Запретить", "Разрешить"),
            collection("blocked-cmds", "Заблокированные команды", "Настраивает список команд, заблокированных на участке.", "minecraft:command_block", "BlockedCmdsFlag", PlotFlagCollectionAddMode.COMMAND_ONLY),
            booleanPreset("server-plot", "Серверный участок", "Переключает визуальное отображение участка как серверного.", "minecraft:nether_star", "ServerPlotFlag", "Включить", "Выключить"),
            PlotFlagDefinition(
                key = "plot-paint",
                groupKey = "plot-paint",
                title = "Искусство",
                description = "Управляет возможностью гостей использовать /paint.",
                itemModel = "minecraft:painting",
                kind = PlotFlagEntryKind.PRESET,
                flagClassCandidates = listOf("__custom_plot_paint__"),
                presetOptions = listOf(
                    PlotFlagOption("Не задано", "none"),
                    PlotFlagOption("Включить", "true"),
                    PlotFlagOption("Выключить", "false")
                )
            )
        )
    }
}

object PlotFlagCatalog {
    val definitions: List<PlotFlagDefinition> = PlotFlagDefinition.definitions
}
