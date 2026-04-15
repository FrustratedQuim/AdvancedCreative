@file:Suppress("UnstableApiUsage") // Experimental Equippable

package com.ratger.acreative.menus.edit.equippable

import net.kyori.adventure.key.Key
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.Sound
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.components.EquippableComponent

object EquippableSupport {

    fun hasExisting(item: ItemStack): Boolean = item.itemMeta?.hasEquippable() == true

    fun hasExistingOrPrototype(item: ItemStack): Boolean = hasExisting(item) || baselineComponent(item, EquipmentSlot.HAND) != null

    fun explicitSnapshot(item: ItemStack): EquippableComponent? {
        val meta = item.itemMeta ?: return null
        if (!meta.hasEquippable()) return null
        return meta.equippable
    }

    fun resolvedSnapshot(item: ItemStack): EquippableComponent? = explicitSnapshot(item) ?: baselineComponent(item, EquipmentSlot.HAND)

    fun prototypeSnapshot(item: ItemStack): EquippableComponent? {
        val meta = ItemStack(item.type).itemMeta ?: return null
        if (!meta.hasEquippable()) return null
        return meta.equippable
    }

    fun effectiveSlot(item: ItemStack): EquipmentSlot? = resolvedSnapshot(item)?.slot

    fun effectiveEquipSound(item: ItemStack): Sound? = resolvedSnapshot(item)?.equipSound

    fun effectiveDispensable(item: ItemStack): Boolean = resolvedSnapshot(item)?.isDispensable == true

    fun effectiveSwappable(item: ItemStack): Boolean = resolvedSnapshot(item)?.isSwappable == true

    fun effectiveDamageOnHurt(item: ItemStack): Boolean = resolvedSnapshot(item)?.isDamageOnHurt ?: true

    fun isFieldOrdinarySound(item: ItemStack): Boolean {
        val explicit = explicitSnapshot(item) ?: return true
        return explicit.equipSound == baselineComponent(item, EquipmentSlot.HAND)?.equipSound
    }

    fun isFieldOrdinaryOverlay(item: ItemStack): Boolean {
        val explicit = explicitSnapshot(item) ?: return true
        return explicit.cameraOverlay == baselineComponent(item, EquipmentSlot.HAND)?.cameraOverlay
    }

    fun isFieldOrdinaryModel(item: ItemStack): Boolean {
        val explicit = explicitSnapshot(item) ?: return true
        return explicit.model == baselineComponent(item, EquipmentSlot.HAND)?.model
    }

    fun mutateOrCreateForMenu(
        item: ItemStack,
        preferredFallbackSlot: EquipmentSlot = EquipmentSlot.HAND,
        mutator: EquippableComponent.() -> Unit
    ): Boolean {
        val meta = item.itemMeta ?: return false
        val base = explicitSnapshot(item)
            ?: baselineComponent(item, preferredFallbackSlot)
            ?: return false
        base.mutator()
        meta.setEquippable(base)
        item.itemMeta = meta
        return true
    }

    fun normalizeAfterMutation(item: ItemStack) {
        val meta = item.itemMeta ?: return
        if (!meta.hasEquippable()) return

        val explicit = meta.equippable
        val baseline = baselineComponent(item, EquipmentSlot.HAND) ?: return
        if (componentsMatch(explicit, baseline)) {
            meta.setEquippable(null)
            item.itemMeta = meta
        }
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
        val defaultSound = baselineComponent(item, EquipmentSlot.HAND)?.equipSound ?: return false
        return mutateFromExistingOrPrototype(item) { setEquipSound(defaultSound) }
    }

    fun setCameraOverlay(item: ItemStack, keyOrNull: Key?): Boolean =
        mutateFromExistingOrPrototype(item) { cameraOverlay = keyOrNull?.let(::toNamespacedKey) }

    fun setAssetId(item: ItemStack, keyOrNull: Key?): Boolean =
        mutateFromExistingOrPrototype(item) { model = keyOrNull?.let(::toNamespacedKey) }


    private fun baselineComponent(item: ItemStack, preferredFallbackSlot: EquipmentSlot): EquippableComponent? {
        val freshMeta = ItemStack(item.type).itemMeta ?: return null
        val baseline = freshMeta.equippable
        baseline.slot = inferredPrototypeSlot(item) ?: preferredFallbackSlot
        return baseline
    }

    fun inferredPrototypeSlot(item: ItemStack): EquipmentSlot? {
        val typeName = item.type.name
        return when {
            typeName.endsWith("HELMET") -> EquipmentSlot.HEAD
            typeName.endsWith("CHESTPLATE") -> EquipmentSlot.CHEST
            typeName.endsWith("LEGGINGS") -> EquipmentSlot.LEGS
            typeName.endsWith("BOOTS") -> EquipmentSlot.FEET
            typeName == "ELYTRA" -> EquipmentSlot.CHEST
            typeName == "CARVED_PUMPKIN" -> EquipmentSlot.HEAD
            typeName == "SHIELD" -> EquipmentSlot.OFF_HAND
            else -> null
        }
    }

    private fun mutateFromExistingOrPrototype(item: ItemStack, mutator: EquippableComponent.() -> Unit): Boolean {
        val base = explicitSnapshot(item) ?: baselineComponent(item, EquipmentSlot.HAND) ?: return false
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

    private fun componentsMatch(a: EquippableComponent, b: EquippableComponent): Boolean {
        return a.slot == b.slot &&
            a.equipSound == b.equipSound &&
            a.model == b.model &&
            a.cameraOverlay == b.cameraOverlay &&
            a.isDispensable == b.isDispensable &&
            a.isSwappable == b.isSwappable &&
            a.isDamageOnHurt == b.isDamageOnHurt &&
            a.allowedEntities == b.allowedEntities
    }

    private fun toNamespacedKey(key: Key): NamespacedKey =
        requireNotNull(NamespacedKey.fromString(key.asString())) { "Invalid key: ${key.asString()}" }
}
