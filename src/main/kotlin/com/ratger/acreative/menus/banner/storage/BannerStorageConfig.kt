package com.ratger.acreative.menus.banner.storage

data class BannerStorageConfig(
    val defaultLimit: Int,
    val minPages: Int,
    val pageSize: Int,
    val limitsByPermission: Map<String, Int>
)
