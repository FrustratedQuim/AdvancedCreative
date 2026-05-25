package com.ratger.acreative.menus.edit.simple

import com.ratger.acreative.menus.edit.ItemEditSession
import com.ratger.acreative.menus.edit.effects.EdibleMenuSupport
import com.ratger.acreative.menus.edit.effects.FoodComponentSupport
import com.ratger.acreative.menus.edit.equippable.EquippableSupport
import com.ratger.acreative.menus.edit.meta.ItemStackReplacementSupport
import com.ratger.acreative.menus.edit.meta.MaxStackSizeSupport
import com.ratger.acreative.menus.edit.text.ItemTextStyleService
import com.ratger.acreative.menus.edit.text.VanillaRuLocalization
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

object SimpleThrowableToggleSupport {
    fun isEnabled(session: ItemEditSession): Boolean = session.simpleThrowableSnapshot != null

    fun toggle(session: ItemEditSession, textStyleService: ItemTextStyleService) {
        if (isEnabled(session)) {
            disable(session)
        } else {
            enable(session, textStyleService)
        }
    }

    private fun enable(session: ItemEditSession, textStyleService: ItemTextStyleService) {
        session.simpleThrowableSnapshot = session.editableItem.clone()
        if (session.editableItem.type == Material.SNOWBALL) {
            return
        }

        val source = session.editableItem
        val sourceMeta = source.itemMeta
        val sourceType = source.type
        val sourceAmount = source.amount
        val sourceTargetMaxStack = if (sourceMeta?.hasMaxStackSize() == true) {
            sourceMeta.maxStackSize
        } else {
            sourceType.maxStackSize
        }

        val converted = ItemStackReplacementSupport.replaceItemId(source, Material.SNOWBALL)
        val convertedMeta = converted.itemMeta ?: return
        val previousModel = runCatching { sourceMeta?.itemModel }.getOrNull()
        convertedMeta.itemModel = previousModel ?: NamespacedKey.minecraft(sourceType.key.key)
        if (sourceTargetMaxStack != Material.SNOWBALL.maxStackSize) {
            convertedMeta.setMaxStackSize(sourceTargetMaxStack)
        } else {
            MaxStackSizeSupport.clearCustomMaxStackSize(convertedMeta)
        }
        if (!textStyleService.hasCustomName(source)) {
            convertedMeta.customName(generatedDefaultName(sourceType))
        }
        converted.itemMeta = convertedMeta
        converted.amount = sourceAmount.coerceIn(1, effectiveMaxStack(converted))
        session.editableItem = converted
    }

    private fun disable(session: ItemEditSession) {
        val snapshot = session.simpleThrowableSnapshot ?: return
        session.simpleThrowableSnapshot = null
        if (snapshot.type == Material.SNOWBALL) {
            return
        }

        val current = session.editableItem
        val restored = ItemStackReplacementSupport.replaceItemId(current, snapshot.type)
        val restoredMeta = restored.itemMeta ?: run {
            session.editableItem = restored
            return
        }
        val snapshotMeta = snapshot.itemMeta

        restoredMeta.itemModel = runCatching { snapshotMeta?.itemModel }.getOrNull()
        if (snapshotMeta?.hasMaxStackSize() == true) {
            restoredMeta.setMaxStackSize(snapshotMeta.maxStackSize)
        } else {
            MaxStackSizeSupport.clearCustomMaxStackSize(restoredMeta)
        }
        if (snapshotMeta?.hasCustomName() != true && restoredMeta.customName() == generatedDefaultName(snapshot.type)) {
            restoredMeta.customName(null)
        }
        restored.itemMeta = restoredMeta
        restored.amount = current.amount.coerceIn(1, effectiveMaxStack(restored))
        session.editableItem = restored
    }

    private fun effectiveMaxStack(item: ItemStack): Int {
        val meta = item.itemMeta
        return if (meta?.hasMaxStackSize() == true) meta.maxStackSize else item.type.maxStackSize
    }

    private fun generatedDefaultName(material: Material): Component =
        Component.text(VanillaRuLocalization.itemName(material.key.key))
            .decoration(TextDecoration.ITALIC, false)
}

object SimpleEdibleToggleSupport {
    fun isEnabled(session: ItemEditSession): Boolean = session.simpleEdibleSnapshot != null

    fun toggle(session: ItemEditSession) {
        if (isEnabled(session)) {
            disable(session)
        } else {
            enable(session)
        }
    }

    private fun enable(session: ItemEditSession) {
        session.simpleEdibleSnapshot = session.editableItem.clone()
        if (EdibleMenuSupport.isEnabled(session.editableItem)) {
            return
        }

        EdibleMenuSupport.ensureEnabledWithDefaults(session.editableItem)
        FoodComponentSupport.setCanAlwaysEat(session.editableItem, true)
        FoodComponentSupport.setSaturation(session.editableItem, 6f)
        FoodComponentSupport.setNutrition(session.editableItem, 5)
    }

    private fun disable(session: ItemEditSession) {
        val snapshot = session.simpleEdibleSnapshot ?: return
        session.simpleEdibleSnapshot = null
        EdibleMenuSupport.restoreSnapshot(session.editableItem, EdibleMenuSupport.captureSnapshot(snapshot))
    }
}

object SimpleHeadEquippableToggleSupport {
    fun isEnabled(session: ItemEditSession): Boolean = session.simpleHeadEquippableSnapshot != null

    fun toggle(session: ItemEditSession) {
        if (isEnabled(session)) {
            disable(session)
        } else {
            enable(session)
        }
    }

    private fun enable(session: ItemEditSession) {
        session.simpleHeadEquippableSnapshot = session.editableItem.clone()
        if (EquippableSupport.effectiveSlot(session.editableItem) == EquipmentSlot.HEAD) {
            return
        }
        EquippableSupport.setSlot(session.editableItem, EquipmentSlot.HEAD)
    }

    private fun disable(session: ItemEditSession) {
        val snapshot = session.simpleHeadEquippableSnapshot ?: return
        session.simpleHeadEquippableSnapshot = null
        EquippableSupport.restoreExplicitState(session.editableItem, snapshot)
    }
}
