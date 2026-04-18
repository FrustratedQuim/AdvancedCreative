package com.ratger.acreative.menus.edit.effects.visual

data class VisualEffectDraft(
    val effectTypeKey: String? = null,
    val level: Int = 1,
    val durationSeconds: Int = VisualEffectInputSupport.DEFAULT_DURATION_SECONDS,
    val probabilityPercent: Int = 100,
    val showParticles: Boolean = true,
    val showIcon: Boolean = true
)

enum class VisualEffectContextKey {
    POTION,
    CONSUMABLE,
    DEATH_PROTECTION
}
