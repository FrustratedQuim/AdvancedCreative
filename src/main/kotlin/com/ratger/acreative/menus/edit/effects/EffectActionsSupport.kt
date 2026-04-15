package com.ratger.acreative.menus.edit.effects

import com.ratger.acreative.menus.edit.api.EffectActionSpec
import com.ratger.acreative.menus.edit.validation.ValidationService

object EffectActionsSupport {

    fun render(spec: EffectActionSpec): String {
        return when (spec) {
            EffectActionSpec.ClearAllEffects -> "clear_all_effects"
            is EffectActionSpec.PlaySound -> "play_sound sound=${spec.key.asString()}"
            is EffectActionSpec.RemoveEffects -> {
                val keys = spec.effects.joinToString(" ") { it.key.key }
                "remove_effects effects=$keys"
            }
            is EffectActionSpec.TeleportRandomly -> "teleport_randomly diameter=${spec.diameter}"
            is EffectActionSpec.ApplyEffects -> {
                val effects = spec.effects.joinToString("; ") {
                    "${it.type.key.key} dur=${it.duration} amp=${it.amplifier} particles=${it.showParticles} icon=${it.showIcon}"
                }
                "apply_effects probability=${spec.probability} effects=$effects"
            }
        }
    }

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
                val bad = spec.effects.firstOrNull { it.duration <= 0 || it.amplifier < 0 }
                if (bad != null) {
                    if (bad.duration <= 0) "duration должен быть > 0" else "amplifier не может быть отрицательным"
                } else null
            }
        }
    }

}
