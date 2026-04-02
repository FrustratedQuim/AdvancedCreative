package com.ratger.acreative.core

enum class MessageChannel {
    CHAT,
    ACTION_BAR
}

enum class MessageTaskState {
    PENDING,
    ACTIVE,
    PAUSED,
    CANCELLED
}

enum class MessageKey {
    AHELP,
    PERMISSION_UNKNOWN,
    PERMISSION_REQUIRED,
    INFO_EMPTY,
    ERROR_UNKNOWN_VALUE,
    ERROR_UNKNOWN_PLAYER,
    ERROR_CANNOT_LAY,
    ERROR_CANNOT_DISGUISED,
    ERROR_CANNOT_OTHER_DISGUISED,
    ERROR_TOO_SMALL,
    ERROR_TOO_LARGE,
    ACTION_POSE_UNSET,
    ACTION_COOLDOWN,
    ERROR_SIT_IN_AIR,
    ERROR_LAY_IN_AIR,
    ERROR_CRAWL_IN_AIR,
    USAGE_SITHEAD,
    USAGE_SITHEAD_OTHER,
    SITHEAD_HIDDEN_BY_ONE,
    SITHEAD_HIDDEN_BY_ONE_TARGET,
    SITHEAD_HIDDEN_SELF,
    SITHEAD_HIDDEN_SELF_TARGET,
    SITHEAD_YOU_HIDE_ONE,
    SITHEAD_ONE_HIDDEN_BY_PLAYER,
    SITHEAD_BLOCK_INTERACT,
    SITHEAD_UNBLOCK_INTERACT,
    INFO_GLIDE_ON,
    INFO_GLIDE_OFF,
    INFO_CRAWL_ON,
    INFO_CRAWL_OFF,
    USAGE_HIDE,
    ERROR_HIDE_SELF,
    SUCCESS_HIDE,
    SUCCESS_HIDE_REMOVED,
    NOTIFY_HIDE,
    ERROR_HIDE_BYPASS,
    ERROR_HIDE_FAILED,
    USAGE_GRAVITY,
    SUCCESS_GRAVITY_SET,
    SUCCESS_GRAVITY_RESET,
    USAGE_RESIZE,
    SUCCESS_RESIZE_SET,
    SUCCESS_RESIZE_RESET,
    USAGE_STRENGTH,
    SUCCESS_STRENGTH_SET,
    SUCCESS_STRENGTH_RESET,
    USAGE_HEALTH,
    SUCCESS_HEALTH_SET,
    SUCCESS_HEALTH_RESET,
    SUCCESS_FREEZE,
    SUCCESS_FREEZE_SELF,
    INFO_GLOW_ON,
    INFO_GLOW_OFF,
    USAGE_DISGUISE,
    ERROR_DISGUISE_TYPE,
    ERROR_DISGUISE_BLOCKED,
    SUCCESS_DISGUISE,
    SUCCESS_DISGUISE_REMOVED,
    USAGE_EFFECTS,
    USAGE_JAR,
    USAGE_GRAB,
    ERROR_EFFECT_UNKNOWN,
    ERROR_GRAB_SELF,
    ERROR_GRAB_TOO_FAR,
    ERROR_GRAB_TARGET_BUSY,
    ERROR_JAR_HAND_NOT_EMPTY,
    ERROR_JAR_TARGET_BUSY,
    ERROR_JAR_NO_SUPPORT,
    ERROR_JAR_CONST_BLOCK,
    ERROR_JAR_OWNER_MISMATCH,
    SUCCESS_EFFECT,
    SUCCESS_EFFECT_TARGET,
    SUCCESS_EFFECT_REMOVED,
    SUCCESS_EFFECTS_CLEARED,
    SUCCESS_EFFECT_REMOVED_TARGET,
    INFO_GRAB_STARTED,
    INFO_JAR_ITEM_GIVEN,
    INFO_JAR_APPLIED,
    INFO_JAR_RELEASED,
    INFO_JAR_TARGET_APPLIED,
    INFO_JAR_TARGET_RELEASED,
    INFO_SLAP_ON,
    INFO_SLAP_OFF,
    ITEMDB_INFO,
    EDIT_EMPTY_HAND
}

object MessageCatalog {
    val templates: Map<MessageKey, String> = mapOf(
        MessageKey.AHELP to """
            <#FFD700><st>                      </st><<#FFE68A><b> Полезные команды </b><#FFD700>><st>                      </st>
            <#FFE68A>/sit <#EDC800>- <#FFF3E0>Сесть
            <#FFE68A>/lay <#EDC800>- <#FFF3E0>Лечь
            <#FFE68A>/crawl <#EDC800>- <#FFF3E0>Ползти
            <#FFE68A>/hide <игрок> <#EDC800>- <#FFF3E0>Скрыть игрока
            <#FFE68A>/strength <значение> <#EDC800>- <#FFF3E0>Установить силу удара
            <#FFE68A>/health <значение> <#EDC800>- <#FFF3E0>Установить максимальное здоровье
            <#FFE68A>/effects <эффект> [уровень] <#EDC800>- <#FFF3E0>Наложить эффект зелья
            <#FFE68A>/jar <игрок> [-const] <#EDC800>- <#FFF3E0>Выдать банку для поимки
            <#FFE68A>/sneeze <#EDC800>- <#FFF3E0>Чихнуть, вот это да
            <#FFE68A>/glide <#EDC800>- <#FFF3E0>Включить парение без элитр
            <#FFE68A>/gravity <значение> <#EDC800>- <#FFF3E0>Изменить свою гравитацию
            <#FFE68A>/resize <значение> <#EDC800>- <#FFF3E0>Изменить размер персонажа
            <#FFE68A>/freeze <#EDC800>- <#FFF3E0>Замёрзнуть? Буквально
            <#FFE68A>/disguise <существо> <#EDC800>- <#FFF3E0>Превратиться в что-то
            <#FFE68A>/glow <#EDC800>- <#FFF3E0>Включить свечение
            <#FFE68A>/spit <#EDC800>- <#FFF3E0>Плюнуть, агрессивно
            <#FFE68A>/piss <#EDC800>- <#FFF3E0>Пописать на негодяев
            <#FFD700><st>                                                                             </st>
        """.trimIndent(),
        MessageKey.PERMISSION_UNKNOWN to "<dark_red>▍ <#FF1500>У вас недостаточно прав!",
        MessageKey.PERMISSION_REQUIRED to "<dark_red>▍ <#FF1500>Команда доступна с привилегии <b>%role_display%",
        MessageKey.INFO_EMPTY to "",
        MessageKey.ERROR_UNKNOWN_VALUE to "<dark_red>▍ <#FF1500>Неизвестное значение!",
        MessageKey.ERROR_UNKNOWN_PLAYER to "<dark_red>▍ <#FF1500>Неизвестный игрок.",
        MessageKey.ERROR_CANNOT_LAY to "<dark_red>▍ <#FF1500>Вы не можете делать это лёжа!",
        MessageKey.ERROR_CANNOT_DISGUISED to "<dark_red>▍ <#FF1500>Вы не можете делать это в облике!",
        MessageKey.ERROR_CANNOT_OTHER_DISGUISED to "<dark_red>▍ <#FF1500>Это не получится сделать в облике!",
        MessageKey.ERROR_TOO_SMALL to "<dark_red>▍ <#FF1500>Вы слишком маленькие для этого!",
        MessageKey.ERROR_TOO_LARGE to "<dark_red>▍ <#FF1500>Вы слишком большие для этого!",
        MessageKey.ACTION_POSE_UNSET to "<#00FF40>Нажмите шифт, чтобы встать",
        MessageKey.ACTION_COOLDOWN to "<#FF1500>Подождите %time%",
        MessageKey.ERROR_SIT_IN_AIR to "<dark_red>▍ <#FF1500>Вы не можете сидеть в воздухе!",
        MessageKey.ERROR_LAY_IN_AIR to "<dark_red>▍ <#FF1500>Вы не можете лежать в воздухе!",
        MessageKey.ERROR_CRAWL_IN_AIR to "<dark_red>▍ <#FF1500>Вы не можете ползать в воздухе!",
        MessageKey.USAGE_SITHEAD to "<dark_red>▍ <#FF1500>Используйте /sithead toggle",
        MessageKey.USAGE_SITHEAD_OTHER to "<dark_red>▍ <#FF1500>Используйте /sithead <цель> [игрок]",
        MessageKey.SITHEAD_HIDDEN_BY_ONE to "<dark_red>▍ <#FF1500>Один из сидящих игроков скрыл вас!",
        MessageKey.SITHEAD_HIDDEN_BY_ONE_TARGET to "<dark_red>▍ <#FF1500>Один из сидящих игроков скрыл %player%!",
        MessageKey.SITHEAD_HIDDEN_SELF to "<dark_red>▍ <#FF1500>Этот игрок скрыл вас от себя!",
        MessageKey.SITHEAD_HIDDEN_SELF_TARGET to "<dark_red>▍ <#FF1500>Этот игрок скрыл %player% от себя!",
        MessageKey.SITHEAD_YOU_HIDE_ONE to "<dark_red>▍ <#FF1500>Вы скрыли одного из сидящих игроков!",
        MessageKey.SITHEAD_ONE_HIDDEN_BY_PLAYER to "<dark_red>▍ <#FF1500>Один из сидящих игроков скрыт %player%!",
        MessageKey.SITHEAD_BLOCK_INTERACT to "<dark_red>▍ <#FF1500>Вы отключили посадку на голову по нажатию!",
        MessageKey.SITHEAD_UNBLOCK_INTERACT to "<dark_green>▍ <#00FF40>Посадка на голову по нажатию включена.",
        MessageKey.INFO_GLIDE_ON to "<dark_green>▍ <#00FF40>Режим парения без элитр включён!",
        MessageKey.INFO_GLIDE_OFF to "<dark_red>▍ <#FF1500>Режим парения без элитр отключён.",
        MessageKey.INFO_CRAWL_ON to "<dark_green>▍ <#00FF40>Режим ползания включён!",
        MessageKey.INFO_CRAWL_OFF to "<dark_red>▍ <#FF1500>Режим ползания отключён.",
        MessageKey.USAGE_HIDE to "<dark_red>▍ <#FF1500>Используйте /hide <игрок>",
        MessageKey.ERROR_HIDE_SELF to "<dark_red>▍ <#FF1500>Вы не можете скрыть себя!",
        MessageKey.SUCCESS_HIDE to "<dark_green>▍ <#00FF40>Игрок %target% был скрыт от вас на 30 минут!",
        MessageKey.SUCCESS_HIDE_REMOVED to "<dark_red>▍ <#FF1500>Игрок %target% снова виден для вас.",
        MessageKey.NOTIFY_HIDE to "<dark_red>▍ <#FF1500>Игрок %hider% скрыл вас для себя на 30 минут.",
        MessageKey.ERROR_HIDE_BYPASS to "<dark_red>▍ <#FF1500>Вы не можете скрыть этого игрока!",
        MessageKey.ERROR_HIDE_FAILED to "<dark_red>▍ <#FF1500>Не удалось обновить состояние скрытия игрока.",
        MessageKey.USAGE_GRAVITY to "<dark_red>▍ <#FF1500>Используйте /gravity <значение>",
        MessageKey.SUCCESS_GRAVITY_SET to "<dark_green>▍ <#00FF40>Установлена гравитация: %value%",
        MessageKey.SUCCESS_GRAVITY_RESET to "<dark_green>▍ <#00FF40>Гравитация восстановлена!",
        MessageKey.USAGE_RESIZE to "<dark_red>▍ <#FF1500>Используйте /resize <значение>",
        MessageKey.SUCCESS_RESIZE_SET to "<dark_green>▍ <#00FF40>Установлен размер: %value%",
        MessageKey.SUCCESS_RESIZE_RESET to "<dark_green>▍ <#00FF40>Размер восстановлен!",
        MessageKey.USAGE_STRENGTH to "<dark_red>▍ <#FF1500>Используйте /strength <значение>",
        MessageKey.SUCCESS_STRENGTH_SET to "<dark_green>▍ <#00FF40>Установлена сила: %value%",
        MessageKey.SUCCESS_STRENGTH_RESET to "<dark_green>▍ <#00FF40>Сила восстановлена!",
        MessageKey.USAGE_HEALTH to "<dark_red>▍ <#FF1500>Используйте /health <значение>",
        MessageKey.SUCCESS_HEALTH_SET to "<dark_green>▍ <#00FF40>Установлено максимальное здоровье: %value%",
        MessageKey.SUCCESS_HEALTH_RESET to "<dark_green>▍ <#00FF40>Максимальное здоровье восстановлено!",
        MessageKey.SUCCESS_FREEZE to "<dark_green>▍ <#00FF40>Игрок %target% был заморожен!",
        MessageKey.SUCCESS_FREEZE_SELF to "<dark_green>▍ <#00FF40>Вы заморозили себя!",
        MessageKey.INFO_GLOW_ON to "<dark_green>▍ <#00FF40>Свечение включено.",
        MessageKey.INFO_GLOW_OFF to "<dark_red>▍ <#FF1500>Свечение отключено.",
        MessageKey.USAGE_DISGUISE to "<dark_red>▍ <#FF1500>Используйте /disguise <существо>",
        MessageKey.ERROR_DISGUISE_TYPE to "<dark_red>▍ <#FF1500>Неизвестное существо!",
        MessageKey.ERROR_DISGUISE_BLOCKED to "<dark_red>▍ <#FF1500>Заблокировано из-за непредсказуемого поведения.",
        MessageKey.SUCCESS_DISGUISE to "<dark_green>▍ <#00FF40>Вы успешно превратились!",
        MessageKey.SUCCESS_DISGUISE_REMOVED to "<dark_green>▍ <#00FF40>Ваш облик восстановлен.",
        MessageKey.USAGE_EFFECTS to "<dark_red>▍ <#FF1500>Используйте /effects <эффект> [уровень]",
        MessageKey.USAGE_JAR to "<dark_red>▍ <#FF1500>Используйте /jar <игрок>",
        MessageKey.USAGE_GRAB to "<dark_red>▍ <#FF1500>Используйте /grab <игрок>",
        MessageKey.ERROR_EFFECT_UNKNOWN to "<dark_red>▍ <#FF1500>Неизвестный эффект!",
        MessageKey.ERROR_GRAB_SELF to "<dark_red>▍ <#FF1500>Вы не можете схватить себя!",
        MessageKey.ERROR_GRAB_TOO_FAR to "<dark_red>▍ <#FF1500>Цель слишком далеко.",
        MessageKey.ERROR_GRAB_TARGET_BUSY to "<dark_red>▍ <#FF1500>Игрок уже захвачен кем-то.",
        MessageKey.ERROR_JAR_HAND_NOT_EMPTY to "<dark_red>▍ <#FF1500>Освободите слот в хотбаре.",
        MessageKey.ERROR_JAR_TARGET_BUSY to "<dark_red>▍ <#FF1500>Этот игрок уже находится в банке.",
        MessageKey.ERROR_JAR_NO_SUPPORT to "<dark_red>▍ <#FF1500>Под банкой должен быть твёрдый блок.",
        MessageKey.ERROR_JAR_CONST_BLOCK to "<dark_red>▍ <#FF1500>Константная банка не освобождается ломанием блока.",
        MessageKey.ERROR_JAR_OWNER_MISMATCH to "<dark_red>▍ <#FF1500>Банка защищена магией..",
        MessageKey.SUCCESS_EFFECT to "<dark_green>▍ <#00FF40>Эффект успешно наложен.",
        MessageKey.SUCCESS_EFFECT_TARGET to "<dark_green>▍ <#00FF40>Эффект успешно наложен на %player%.",
        MessageKey.SUCCESS_EFFECT_REMOVED to "<dark_red>▍ <#FF1500>Эффект снят.",
        MessageKey.SUCCESS_EFFECTS_CLEARED to "<dark_red>▍ <#FF1500>Все эффекты сняты.",
        MessageKey.SUCCESS_EFFECT_REMOVED_TARGET to "<dark_red>▍ <#FF1500>Эффект снят с %player%.",
        MessageKey.INFO_GRAB_STARTED to "<dark_green>▍ <#00FF40>Вы схватили %target%.",
        MessageKey.INFO_JAR_ITEM_GIVEN to "<dark_green>▍ <#00FF40>Вы получили банку для %target%.",
        MessageKey.INFO_JAR_APPLIED to "<dark_green>▍ <#00FF40>%target% помещён в банку.",
        MessageKey.INFO_JAR_RELEASED to "<dark_green>▍ <#00FF40>%target% освобождён из банки.",
        MessageKey.INFO_JAR_TARGET_APPLIED to "<dark_red>▍ <#FF1500>Вы были помещены в банку.",
        MessageKey.INFO_JAR_TARGET_RELEASED to "<dark_green>▍ <#00FF40>Вы освобождены из банки.",
        MessageKey.INFO_SLAP_ON to "<dark_green>▍ <#00FF40>Режим пощёчин включён!",
        MessageKey.INFO_SLAP_OFF to "<dark_red>▍ <#FF1500>Режим пощёчин отключён.",
        MessageKey.ITEMDB_INFO to """
            <#FFD700><u>▍</u> <#FFE68A>Предмет: <#FFF3E0>%item_name%
            <#FFD700>▍ <#FFE68A>Цифровое ID: <#FFF3E0>%numeric_id%
        """.trimIndent(),
        MessageKey.EDIT_EMPTY_HAND to "<dark_red>▍ <#FF1500>Возьмите предмет в руку"
    )
}
