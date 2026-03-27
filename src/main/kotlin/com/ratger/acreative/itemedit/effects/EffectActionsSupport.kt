package com.ratger.acreative.itemedit.effects

import com.ratger.acreative.commands.edit.EditParsers
import com.ratger.acreative.itemedit.api.EffectActionSpec
import com.ratger.acreative.itemedit.api.EffectApplyEntrySpec
import com.ratger.acreative.itemedit.validation.ValidationService

object EffectActionsSupport {
    fun parseEffectSpec(parsers: EditParsers, args: List<String>): EffectActionSpec? {
        val kind = args.firstOrNull()?.lowercase() ?: return null
        return when (kind) {
            "clear_all_effects" -> EffectActionSpec.ClearAllEffects
            "play_sound" -> {
                val key = parsers.parseAdventureKey(args.getOrNull(1) ?: return null) ?: return null
                EffectActionSpec.PlaySound(key)
            }

            "remove_effects" -> {
                val raw = args.drop(1)
                val effects = raw.mapNotNull(parsers::effectFromToken)
                if (effects.size != raw.size) return null
                if (effects.isEmpty()) return null
                EffectActionSpec.RemoveEffects(effects)
            }

            "teleport_randomly" -> {
                val diameter = args.getOrNull(1)?.toFloatOrNull() ?: return null
                EffectActionSpec.TeleportRandomly(diameter)
            }

            "apply_effects" -> {
                val probability = args.getOrNull(1)?.toFloatOrNull() ?: return null
                val entries = parseApplyEntries(parsers, args.drop(2))
                if (entries.isEmpty()) return null
                EffectActionSpec.ApplyEffects(probability, entries)
            }

            else -> null
        }
    }

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

    fun parseApplyEntries(parsers: EditParsers, tokens: List<String>): List<EffectApplyEntrySpec> {
        if (tokens.isEmpty()) return emptyList()
        val groups = mutableListOf<MutableList<String>>()
        var current = mutableListOf<String>()
        for (token in tokens) {
            if (token == ";") {
                if (current.isNotEmpty()) groups += current
                current = mutableListOf()
            } else {
                current += token
            }
        }
        if (current.isNotEmpty()) groups += current

        val parsed = groups.map { parseApplyEntry(parsers, it) }
        if (parsed.any { it == null }) return emptyList()
        return parsed.filterNotNull()
    }

    private fun parseApplyEntry(parsers: EditParsers, tokens: List<String>): EffectApplyEntrySpec? {
        val type = parsers.effectFromToken(tokens.getOrNull(0) ?: return null) ?: return null
        val duration = tokens.getOrNull(1)?.toIntOrNull() ?: return null
        val amplifier = tokens.getOrNull(2)?.toIntOrNull() ?: return null
        val showParticlesRaw = tokens.getOrNull(3)
        val showParticles = if (showParticlesRaw == null) true else parsers.parseBooleanStrict(showParticlesRaw) ?: return null
        val showIconRaw = tokens.getOrNull(4)
        val showIcon = if (showIconRaw == null) true else parsers.parseBooleanStrict(showIconRaw) ?: return null
        return EffectApplyEntrySpec(type, duration, amplifier, showParticles, showIcon)
    }

}
