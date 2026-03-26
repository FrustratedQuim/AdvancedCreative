package com.ratger.acreative.commands.edit

import io.papermc.paper.datacomponent.item.consumable.ConsumeEffect
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.set.RegistrySet
import org.bukkit.Registry
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

object EditEffectActionsSupport {
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

    fun toConsumeEffect(spec: EffectActionSpec): ConsumeEffect {
        return when (spec) {
            EffectActionSpec.ClearAllEffects -> ConsumeEffect.clearAllStatusEffects()
            is EffectActionSpec.PlaySound -> ConsumeEffect.playSoundConsumeEffect(spec.key)
            is EffectActionSpec.RemoveEffects -> ConsumeEffect.removeEffects(
                RegistrySet.keySetFromValues(RegistryKey.MOB_EFFECT, spec.effects)
            )

            is EffectActionSpec.TeleportRandomly -> ConsumeEffect.teleportRandomlyEffect(spec.diameter)
            is EffectActionSpec.ApplyEffects -> ConsumeEffect.applyStatusEffects(
                spec.effects.map { PotionEffect(it.type, it.duration, it.amplifier, false, it.showParticles, it.showIcon) },
                spec.probability
            )
        }
    }

    fun render(effect: ConsumeEffect): String {
        return when (effect) {
            is ConsumeEffect.ClearAllStatusEffects -> "clear_all_effects"
            is ConsumeEffect.PlaySound -> "play_sound sound=${effect.sound().asString()}"
            is ConsumeEffect.RemoveStatusEffects -> {
                val keys = effect.removeEffects().resolve(Registry.EFFECT).joinToString(" ") { it.key.key }
                "remove_effects effects=$keys"
            }

            is ConsumeEffect.TeleportRandomly -> "teleport_randomly diameter=${effect.diameter()}"
            is ConsumeEffect.ApplyStatusEffects -> {
                val effects = effect.effects().joinToString("; ") {
                    "${it.type.key.key} dur=${it.duration} amp=${it.amplifier} particles=${it.hasParticles()} icon=${it.hasIcon()}"
                }
                "apply_effects probability=${effect.probability()} effects=$effects"
            }

            else -> "<unknown_effect:${effect::class.simpleName ?: "?"}>"
        }
    }

    fun validateSpec(spec: EffectActionSpec, validation: EditValidationService): String? {
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

    fun removeByIndex(effects: List<ConsumeEffect>, index: Int): List<ConsumeEffect>? {
        if (index !in effects.indices) return null
        return effects.filterIndexed { i, _ -> i != index }
    }

    fun clear(): List<ConsumeEffect> = emptyList()
}
