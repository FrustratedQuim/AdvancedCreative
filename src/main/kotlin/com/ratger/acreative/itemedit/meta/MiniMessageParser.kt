package com.ratger.acreative.itemedit.meta

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage

class MiniMessageParser {
    private val mini = MiniMessage.miniMessage()

    fun parse(input: String): Component = mini.deserialize(input)
}
