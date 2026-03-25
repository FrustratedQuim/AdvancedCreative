package com.ratger.acreative.commands.edit

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage

class EditMiniMessage {
    private val mini = MiniMessage.miniMessage()

    fun parse(input: String): Component = mini.deserialize(input)
}
