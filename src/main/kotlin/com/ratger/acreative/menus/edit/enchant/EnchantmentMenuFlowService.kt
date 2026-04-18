package com.ratger.acreative.menus.edit.enchant

import com.ratger.acreative.menus.edit.ItemEditSession
import org.bukkit.enchantments.Enchantment

class EnchantmentMenuFlowService {
    fun begin(session: ItemEditSession) {
        if (session.enchantmentDraftLevel < 1) {
            session.enchantmentDraftLevel = 1
        }
    }

    fun resolveSelected(session: ItemEditSession): Enchantment? {
        val key = session.enchantmentDraftKey ?: return null
        return EnchantmentSupport.resolve(key)
    }

    fun setSelected(session: ItemEditSession, enchantment: Enchantment?) {
        session.enchantmentDraftKey = enchantment?.let(EnchantmentSupport::keyPath)
    }

    fun setLevel(session: ItemEditSession, level: Int) {
        session.enchantmentDraftLevel = level.coerceIn(1, 127)
    }

    fun apply(session: ItemEditSession): Boolean {
        val enchantment = resolveSelected(session) ?: return false
        val meta = session.editableItem.itemMeta ?: return false
        EnchantmentSupport.add(meta, enchantment, session.enchantmentDraftLevel, ignoreLevelRestriction = true)
        session.editableItem.itemMeta = meta
        return true
    }

    fun reset(session: ItemEditSession) {
        session.enchantmentDraftKey = null
        session.enchantmentDraftLevel = 1
        session.enchantmentDraftLastTypePage = 0
    }
}
