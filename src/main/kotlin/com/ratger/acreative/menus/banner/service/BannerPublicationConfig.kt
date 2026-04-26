package com.ratger.acreative.menus.banner.service

data class BannerPublicationConfig(
    val defaultLimit: Int,
    val limitsByPermission: Map<String, Int>
)
