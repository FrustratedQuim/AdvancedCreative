@file:Suppress("UnstableApiUsage")

package com.ratger.acreative.commands.edit

import net.kyori.adventure.key.Key
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.Sound
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.components.EquippableComponent

object EditEquippableSupport {

    data class EquippableView(
        val slot: EquipmentSlot,
        val dispensable: Boolean,
        val swappable: Boolean,
        val damageOnHurt: Boolean,
        val equipSound: Sound,
        val cameraOverlay: NamespacedKey?,
        val assetId: NamespacedKey?,
        val allowedEntitiesCount: Int
    )

    fun existingView(item: ItemStack): EquippableView? {
        val component = existingSnapshot(item) ?: return null
        return EquippableView(
            slot = component.slot,
            dispensable = component.isDispensable,
            swappable = component.isSwappable,
            damageOnHurt = component.isDamageOnHurt,
            equipSound = component.equipSound,
            cameraOverlay = component.cameraOverlay,
            assetId = component.model,
            allowedEntitiesCount = component.allowedEntities?.size ?: 0
        )
    }

    fun hasExisting(item: ItemStack): Boolean = item.itemMeta?.hasEquippable() == true

    fun hasExistingOrPrototype(item: ItemStack): Boolean = hasExisting(item) || prototypeSnapshot(item) != null

    fun existingSnapshot(item: ItemStack): EquippableComponent? {
        val meta = item.itemMeta ?: return null
        if (!meta.hasEquippable()) return null
        return meta.equippable
    }

    fun prototypeSnapshot(item: ItemStack): EquippableComponent? {
        val meta = ItemStack(item.type).itemMeta ?: return null
        if (!meta.hasEquippable()) return null
        return meta.equippable
    }

    fun setSlot(item: ItemStack, slot: EquipmentSlot): Boolean {
        return mutateFromExistingOrNew(item) { setSlot(slot) }
    }

    fun clear(item: ItemStack) {
        val meta = item.itemMeta ?: return
        meta.setEquippable(null)
        item.itemMeta = meta
    }

    fun setDispensable(item: ItemStack, value: Boolean): Boolean =
        mutateFromExistingOrPrototype(item) { isDispensable = value }

    fun setSwappable(item: ItemStack, value: Boolean): Boolean =
        mutateFromExistingOrPrototype(item) { isSwappable = value }

    fun setDamageOnHurt(item: ItemStack, value: Boolean): Boolean =
        mutateFromExistingOrPrototype(item) { isDamageOnHurt = value }

    fun setEquipSound(item: ItemStack, key: Key): Boolean {
        val sound = Registry.SOUNDS.get(toNamespacedKey(key)) ?: return false
        return mutateFromExistingOrPrototype(item) { setEquipSound(sound) }
    }

    fun restoreDefaultEquipSound(item: ItemStack): Boolean {
        val defaultSound = prototypeSnapshot(item)?.equipSound ?: return false
        return mutateFromExistingOrPrototype(item) { setEquipSound(defaultSound) }
    }

    fun setCameraOverlay(item: ItemStack, keyOrNull: Key?): Boolean =
        mutateFromExistingOrPrototype(item) { cameraOverlay = keyOrNull?.let(::toNamespacedKey) }

    fun setAssetId(item: ItemStack, keyOrNull: Key?): Boolean =
        mutateFromExistingOrPrototype(item) { model = keyOrNull?.let(::toNamespacedKey) }

    private fun mutateFromExistingOrPrototype(item: ItemStack, mutator: EquippableComponent.() -> Unit): Boolean {
        val base = existingSnapshot(item) ?: prototypeSnapshot(item) ?: return false
        return apply(item, base, mutator)
    }

    private fun mutateFromExistingOrNew(item: ItemStack, mutator: EquippableComponent.() -> Unit): Boolean {
        val meta = item.itemMeta ?: return false
        val base = meta.equippable
        return apply(item, base, mutator)
    }

    private fun apply(item: ItemStack, base: EquippableComponent, mutator: EquippableComponent.() -> Unit): Boolean {
        val meta = item.itemMeta ?: return false
        base.mutator()
        meta.setEquippable(base)
        item.itemMeta = meta
        return true
    }

    private fun toNamespacedKey(key: Key): NamespacedKey =
        requireNotNull(NamespacedKey.fromString(key.asString())) { "Invalid key: ${key.asString()}" }
}
