package com.ratger.acreative.menus.common

import org.bukkit.Sound
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.event.ClickEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

object MenuSoundSupport {
    private enum class PendingButtonSoundOverride {
        ERROR
    }

    private val pendingButtonSoundOverrides = ConcurrentHashMap<UUID, PendingButtonSoundOverride>()

    enum class ButtonSoundProfile {
        NONE,
        CLICK,
        LIST,
        CLICK_WITH_DROP_POP,
        LIST_WITH_DROP_POP
    }

    fun playForButtonAction(
        player: Player,
        event: ClickEvent,
        soundProfile: ButtonSoundProfile
    ) {
        when (pendingButtonSoundOverrides.remove(player.uniqueId)) {
            PendingButtonSoundOverride.ERROR -> {
                error(player)
                return
            }
            null -> Unit
        }

        when (soundProfile) {
            ButtonSoundProfile.NONE -> Unit
            ButtonSoundProfile.CLICK -> click(player)
            ButtonSoundProfile.LIST -> listStep(player, isForwardStep(event))
            ButtonSoundProfile.CLICK_WITH_DROP_POP -> if (isDropClick(event)) dropPop(player) else click(player)
            ButtonSoundProfile.LIST_WITH_DROP_POP -> if (isDropClick(event)) dropPop(player) else listStep(player, isForwardStep(event))
        }
    }

    fun overrideNextButtonActionWithError(player: Player) {
        pendingButtonSoundOverrides[player.uniqueId] = PendingButtonSoundOverride.ERROR
    }

    fun click(player: Player) {
        play(player, Sound.UI_BUTTON_CLICK, 0.2f, 1f)
    }

    fun listStep(player: Player, forward: Boolean) {
        play(player, Sound.ENTITY_ITEM_FRAME_ROTATE_ITEM, 1f, if (forward) 1f else 2f)
    }

    fun success(player: Player) {
        play(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.2f, 1f)
    }

    fun error(player: Player) {
        play(player, Sound.ITEM_TRIDENT_HIT_GROUND, 0.5f, 1.5f)
    }

    fun dropPop(player: Player) {
        play(player, Sound.ENTITY_ITEM_PICKUP, 0.3f, Random.nextDouble(0.55, 0.75).toFloat())
    }

    fun paintComplete(player: Player) {
        success(player)
    }

    fun itemFramePlace(player: Player) {
        play(player, Sound.ENTITY_ITEM_FRAME_PLACE, 1f, 1f)
    }

    fun itemFrameBreak(player: Player) {
        play(player, Sound.ENTITY_ITEM_FRAME_BREAK, 1f, 1f)
    }

    fun itemFrameRemoveItem(player: Player) {
        play(player, Sound.ENTITY_ITEM_FRAME_REMOVE_ITEM, 1f, 1f)
    }

    private fun isForwardStep(event: ClickEvent): Boolean {
        return event.isLeft || event.isShiftLeft
    }

    private fun isDropClick(event: ClickEvent): Boolean {
        return event.type == org.bukkit.event.inventory.ClickType.DROP || event.type == org.bukkit.event.inventory.ClickType.CONTROL_DROP
    }

    private fun play(player: Player, sound: Sound, volume: Float, pitch: Float) {
        player.playSound(player.location, sound, volume, pitch)
    }
}
