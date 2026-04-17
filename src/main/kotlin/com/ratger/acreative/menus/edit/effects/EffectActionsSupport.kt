package com.ratger.acreative.menus.edit.effects

import com.ratger.acreative.menus.edit.api.EffectActionSpec
import com.ratger.acreative.menus.edit.validation.ValidationService
import org.bukkit.potion.PotionEffect

object EffectActionsSupport {

    fun validateSpec(spec: EffectActionSpec, validation: ValidationService): String? {
        return when (spec) {
            EffectActionSpec.ClearAllEffects -> null
            is EffectActionSpec.PlaySound -> if (!validation.isValidKey(spec.key.asString())) "Некорректный sound key" else null
            is EffectActionSpec.RemoveEffects -> if (spec.effects.isEmpty()) "remove_effects требует минимум 1 эффект" else null
            is EffectActionSpec.TeleportRandomly -> {
                if (!spec.diameter.isFinite()) "diameter должен быть конечным числом"
                else if (spec.diameter <= 0f) "diameter должен быть > 0"
                else null
            }

            is EffectActionSpec.ApplyEffects -> {
                if (!spec.probability.isFinite()) return "probability должен быть конечным числом"
                if (spec.probability !in 0f..1f) return "probability должен быть в диапазоне 0.0..1.0"
                if (spec.effects.isEmpty()) return "apply_effects требует минимум 1 effect entry"
                val bad = spec.effects.firstOrNull {
                    val isInfinite = it.duration == PotionEffect.INFINITE_DURATION
                    (!isInfinite && it.duration <= 0) || it.amplifier < 0
                }
                if (bad != null) {
                    val isInfinite = bad.duration == PotionEffect.INFINITE_DURATION
                    if (!isInfinite && bad.duration <= 0) "duration должен быть > 0 или равен inf" else "amplifier не может быть отрицательным"
                } else null
            }
        }
    }

}
