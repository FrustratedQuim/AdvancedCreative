package com.ratger.acreative.menus.edit.effects

import com.ratger.acreative.menus.edit.api.EffectActionSpec
import com.ratger.acreative.menus.edit.validation.ValidationService
import org.bukkit.potion.PotionEffect

object EffectActionsSupport {

    fun validateSpec(spec: EffectActionSpec, validation: ValidationService): Boolean {
        return when (spec) {
            EffectActionSpec.ClearAllEffects -> true
            is EffectActionSpec.PlaySound -> validation.isValidKey(spec.key.asString())
            is EffectActionSpec.RemoveEffects -> spec.effects.isNotEmpty()
            is EffectActionSpec.TeleportRandomly -> {
                spec.diameter.isFinite() && spec.diameter > 0f
            }

            is EffectActionSpec.ApplyEffects -> {
                if (!spec.probability.isFinite()) return false
                if (spec.probability !in 0f..1f) return false
                if (spec.effects.isEmpty()) return false
                val bad = spec.effects.firstOrNull {
                    val isInfinite = it.duration == PotionEffect.INFINITE_DURATION
                    (!isInfinite && it.duration <= 0) || it.amplifier < 0
                }
                bad == null
            }
        }
    }

}
