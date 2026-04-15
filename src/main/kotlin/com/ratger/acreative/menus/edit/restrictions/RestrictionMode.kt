package com.ratger.acreative.menus.edit.restrictions

enum class RestrictionMode(
    val summaryEmptyName: String,
    val summaryFilledName: (Int) -> String,
    val summaryListTitle: String,
    val listTitle: String,
    val addButtonTitle: String,
    val itemMaterialFallback: org.bukkit.Material
) {
    CAN_PLACE_ON(
        summaryEmptyName = "<!i><#C7A300>⭘ <#FFD700>Установка: <#FF1500>Нет",
        summaryFilledName = { count -> "<!i><#C7A300>◎ <#FFD700>Установка: <#00FF40>$count" },
        summaryListTitle = "<!i><#FFD700>Разрешено ставить на:",
        listTitle = "<!i>▍ Ограничения → Установка",
        addButtonTitle = "<!i><#00FF40>₪ Добавить блок",
        itemMaterialFallback = org.bukkit.Material.FLINT
    ),
    CAN_BREAK(
        summaryEmptyName = "<!i><#C7A300>⭘ <#FFD700>Разрушение: <#FF1500>Нет",
        summaryFilledName = { count -> "<!i><#C7A300>◎ <#FFD700>Разрушение: <#00FF40>$count" },
        summaryListTitle = "<!i><#FFD700>Разрешено ломать:",
        listTitle = "<!i>▍ Ограничения → Разрушение",
        addButtonTitle = "<!i><#00FF40>₪ Добавить блок",
        itemMaterialFallback = org.bukkit.Material.BLAZE_POWDER
    )
}
