package com.ratger.acreative.menus.itemEdit.pages.pot

import org.bukkit.Material

object PotterySherdCatalog {
    data class SherdDescriptor(
        val material: Material,
        val displayName: String
    )

    val workSlots: List<Int> = listOf(
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        39, 41
    )

    val orderedSherds: List<SherdDescriptor> = listOf(
        SherdDescriptor(Material.MINER_POTTERY_SHERD, "Шахтёр"),
        SherdDescriptor(Material.ARMS_UP_POTTERY_SHERD, "Руки вверх"),
        SherdDescriptor(Material.EXPLORER_POTTERY_SHERD, "Исследователь"),
        SherdDescriptor(Material.FLOW_POTTERY_SHERD, "Поток"),
        SherdDescriptor(Material.PRIZE_POTTERY_SHERD, "Награда"),
        SherdDescriptor(Material.ARCHER_POTTERY_SHERD, "Лучник"),
        SherdDescriptor(Material.ANGLER_POTTERY_SHERD, "Рыбак"),
        SherdDescriptor(Material.PLENTY_POTTERY_SHERD, "Изобилие"),
        SherdDescriptor(Material.SKULL_POTTERY_SHERD, "Череп"),
        SherdDescriptor(Material.SCRAPE_POTTERY_SHERD, "Обтёсывание"),
        SherdDescriptor(Material.SHEAF_POTTERY_SHERD, "Сноп"),
        SherdDescriptor(Material.BLADE_POTTERY_SHERD, "Клинок"),
        SherdDescriptor(Material.BURN_POTTERY_SHERD, "Пламя"),
        SherdDescriptor(Material.DANGER_POTTERY_SHERD, "Угроза"),
        SherdDescriptor(Material.MOURNER_POTTERY_SHERD, "Скорбь"),
        SherdDescriptor(Material.SNORT_POTTERY_SHERD, "Шмыг"),
        SherdDescriptor(Material.FRIEND_POTTERY_SHERD, "Друг"),
        SherdDescriptor(Material.HOWL_POTTERY_SHERD, "Вой"),
        SherdDescriptor(Material.HEARTBREAK_POTTERY_SHERD, "Разбитое сердце"),
        SherdDescriptor(Material.HEART_POTTERY_SHERD, "Сердце"),
        SherdDescriptor(Material.SHELTER_POTTERY_SHERD, "Укрытие"),
        SherdDescriptor(Material.BREWER_POTTERY_SHERD, "Зельевар"),
        SherdDescriptor(Material.GUSTER_POTTERY_SHERD, "Вихрь")
    )

    private val byMaterial: Map<Material, SherdDescriptor> = orderedSherds.associateBy { it.material }

    fun descriptor(material: Material): SherdDescriptor? = byMaterial[material]
}
