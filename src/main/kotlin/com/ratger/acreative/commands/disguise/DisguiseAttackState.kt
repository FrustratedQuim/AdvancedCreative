package com.ratger.acreative.commands.disguise

import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.entity.armadillo.ArmadilloState
import com.github.retrooper.packetevents.protocol.entity.sniffer.SnifferState
import me.tofaa.entitylib.meta.mobs.BeeMeta
import me.tofaa.entitylib.meta.mobs.PandaMeta
import me.tofaa.entitylib.meta.mobs.SnifferMeta
import me.tofaa.entitylib.meta.mobs.golem.SnowGolemMeta
import me.tofaa.entitylib.meta.mobs.monster.piglin.PiglinMeta
import me.tofaa.entitylib.meta.mobs.passive.ArmadilloMeta
import me.tofaa.entitylib.meta.mobs.PolarBearMeta
import me.tofaa.entitylib.meta.mobs.golem.ShulkerMeta
import me.tofaa.entitylib.meta.mobs.monster.EndermanMeta
import me.tofaa.entitylib.meta.mobs.monster.raider.SpellcasterIllagerMeta
import me.tofaa.entitylib.meta.mobs.tameable.CatMeta
import me.tofaa.entitylib.meta.mobs.water.PufferFishMeta
import me.tofaa.entitylib.meta.types.MobMeta
import me.tofaa.entitylib.wrapper.WrapperEntity

data class DisguiseAttackContext(
    val targetEntityId: Int?
)

enum class DisguiseAttackResetMode {
    NONE,
    SERVER_ONLY,
    SERVER_AND_CLIENT
}

sealed interface DisguiseAttackStatePresentation {
    data class Metadata(val entries: List<EntityData<*>>) : DisguiseAttackStatePresentation
    data class EntityStatus(val status: Int) : DisguiseAttackStatePresentation
}

sealed class DisguiseAttackState(
    open val durationTicks: Long,
    open val resetMode: DisguiseAttackResetMode = DisguiseAttackResetMode.SERVER_AND_CLIENT,
    open val isToggle: Boolean = false,
    open val reapplyAfterPositionSyncWhenActive: Boolean = false,
    open val reappliesContinuouslyWhenActive: Boolean = false
) {
    abstract fun apply(entity: WrapperEntity, context: DisguiseAttackContext): Boolean
    abstract fun clear(entity: WrapperEntity): Boolean
    open fun isActive(entity: WrapperEntity): Boolean = false
    open fun metadataSnapshot(entity: WrapperEntity, active: Boolean): List<EntityData<*>>? = null

    open fun presentation(entity: WrapperEntity, active: Boolean): DisguiseAttackStatePresentation? {
        val metadata = metadataSnapshot(entity, active) ?: entity.entityMeta.entityData()
        if (metadata.isEmpty()) return null
        return DisguiseAttackStatePresentation.Metadata(metadata)
    }

    data object None : DisguiseAttackState(
        durationTicks = 0L,
        resetMode = DisguiseAttackResetMode.NONE
    ) {
        override fun apply(entity: WrapperEntity, context: DisguiseAttackContext): Boolean = false
        override fun clear(entity: WrapperEntity): Boolean = false
    }

    data object MobAggressive : DisguiseAttackState(HALF_SECOND_TICKS) {
        override fun apply(entity: WrapperEntity, context: DisguiseAttackContext): Boolean {
            val meta = entity.entityMeta as? MobMeta ?: return false
            if (meta.isAggressive) return false
            meta.isAggressive = true
            return true
        }

        override fun clear(entity: WrapperEntity): Boolean {
            val meta = entity.entityMeta as? MobMeta ?: return false
            if (!meta.isAggressive) return false
            meta.isAggressive = false
            return true
        }

        override fun isActive(entity: WrapperEntity): Boolean {
            val meta = entity.entityMeta as? MobMeta ?: return false
            return meta.isAggressive
        }
    }

    data object PolarBearStanding : DisguiseAttackState(0L, isToggle = true) {
        override fun apply(entity: WrapperEntity, context: DisguiseAttackContext): Boolean {
            val meta = entity.entityMeta as? PolarBearMeta ?: return false
            if (meta.isStandingUp) return false
            meta.isStandingUp = true
            return true
        }

        override fun clear(entity: WrapperEntity): Boolean {
            val meta = entity.entityMeta as? PolarBearMeta ?: return false
            if (!meta.isStandingUp) return false
            meta.isStandingUp = false
            return true
        }

        override fun isActive(entity: WrapperEntity): Boolean {
            val meta = entity.entityMeta as? PolarBearMeta ?: return false
            return meta.isStandingUp
        }
    }

    data object EndermanAggressive : DisguiseAttackState(0L, isToggle = true) {
        override fun apply(entity: WrapperEntity, context: DisguiseAttackContext): Boolean {
            val meta = entity.entityMeta as? EndermanMeta ?: return false
            if (meta.isScreaming && meta.isStaring) return false
            meta.isScreaming = true
            meta.isStaring = true
            return true
        }

        override fun clear(entity: WrapperEntity): Boolean {
            val meta = entity.entityMeta as? EndermanMeta ?: return false
            if (!meta.isScreaming && !meta.isStaring) return false
            meta.isScreaming = false
            meta.isStaring = false
            return true
        }

        override fun isActive(entity: WrapperEntity): Boolean {
            val meta = entity.entityMeta as? EndermanMeta ?: return false
            return meta.isScreaming || meta.isStaring
        }
    }

    data object Spellcasting : DisguiseAttackState(HALF_SECOND_TICKS) {
        override fun apply(entity: WrapperEntity, context: DisguiseAttackContext): Boolean {
            val meta = entity.entityMeta as? SpellcasterIllagerMeta ?: return false
            if (meta.getIndex(SPELLCASTER_SPELL_METADATA_INDEX, 0.toByte()) == SPELLCASTER_ACTIVE_SPELL_ID) {
                return false
            }
            meta.setIndex(SPELLCASTER_SPELL_METADATA_INDEX, EntityDataTypes.BYTE, SPELLCASTER_ACTIVE_SPELL_ID)
            return true
        }

        override fun clear(entity: WrapperEntity): Boolean {
            val meta = entity.entityMeta as? SpellcasterIllagerMeta ?: return false
            if (meta.getIndex(SPELLCASTER_SPELL_METADATA_INDEX, 0.toByte()) == 0.toByte()) {
                return false
            }
            meta.setIndex(SPELLCASTER_SPELL_METADATA_INDEX, EntityDataTypes.BYTE, 0.toByte())
            return true
        }

        override fun isActive(entity: WrapperEntity): Boolean {
            val meta = entity.entityMeta as? SpellcasterIllagerMeta ?: return false
            return meta.getIndex(SPELLCASTER_SPELL_METADATA_INDEX, 0.toByte()) == SPELLCASTER_ACTIVE_SPELL_ID
        }
    }

    data object FullyPuffedPufferfish : DisguiseAttackState(0L, isToggle = true) {
        override fun apply(entity: WrapperEntity, context: DisguiseAttackContext): Boolean {
            val meta = entity.entityMeta as? PufferFishMeta ?: return false
            if (meta.state == PufferFishMeta.State.FULLY_PUFFED) return false
            meta.state = PufferFishMeta.State.FULLY_PUFFED
            return true
        }

        override fun clear(entity: WrapperEntity): Boolean {
            val meta = entity.entityMeta as? PufferFishMeta ?: return false
            if (meta.state == PufferFishMeta.State.UNPUFFED) return false
            meta.state = PufferFishMeta.State.UNPUFFED
            return true
        }

        override fun isActive(entity: WrapperEntity): Boolean {
            val meta = entity.entityMeta as? PufferFishMeta ?: return false
            return meta.state == PufferFishMeta.State.FULLY_PUFFED
        }
    }

    data object ShulkerOpenShell : DisguiseAttackState(
        durationTicks = 0L,
        isToggle = true,
        reapplyAfterPositionSyncWhenActive = true
    ) {
        override fun apply(entity: WrapperEntity, context: DisguiseAttackContext): Boolean {
            val meta = entity.entityMeta as? ShulkerMeta ?: return false
            if (meta.getIndex(SHULKER_PEEK_METADATA_INDEX, SHULKER_CLOSED_HEIGHT) == SHULKER_FULLY_OPEN_HEIGHT) {
                return false
            }
            meta.setIndex(SHULKER_PEEK_METADATA_INDEX, EntityDataTypes.BYTE, SHULKER_FULLY_OPEN_HEIGHT)
            return true
        }

        override fun clear(entity: WrapperEntity): Boolean {
            val meta = entity.entityMeta as? ShulkerMeta ?: return false
            if (meta.getIndex(SHULKER_PEEK_METADATA_INDEX, SHULKER_CLOSED_HEIGHT) == SHULKER_CLOSED_HEIGHT) {
                return false
            }
            meta.setIndex(SHULKER_PEEK_METADATA_INDEX, EntityDataTypes.BYTE, SHULKER_CLOSED_HEIGHT)
            return true
        }

        override fun isActive(entity: WrapperEntity): Boolean {
            val meta = entity.entityMeta as? ShulkerMeta ?: return false
            return meta.getIndex(SHULKER_PEEK_METADATA_INDEX, SHULKER_CLOSED_HEIGHT) == SHULKER_FULLY_OPEN_HEIGHT
        }

        override fun metadataSnapshot(entity: WrapperEntity, active: Boolean): List<EntityData<*>> {
            return listOf(
                EntityData(
                    SHULKER_PEEK_METADATA_INDEX.toInt(),
                    EntityDataTypes.BYTE,
                    if (active) SHULKER_FULLY_OPEN_HEIGHT else SHULKER_CLOSED_HEIGHT
                )
            )
        }
    }

    data object SnifferSniffing : DisguiseAttackState(SNIFFER_SNIFF_TICKS) {
        override fun apply(entity: WrapperEntity, context: DisguiseAttackContext): Boolean {
            val meta = entity.entityMeta as? SnifferMeta ?: return false
            if (meta.state == SnifferState.SNIFFING) return false
            meta.state = SnifferState.SNIFFING
            return true
        }

        override fun clear(entity: WrapperEntity): Boolean {
            val meta = entity.entityMeta as? SnifferMeta ?: return false
            if (meta.state == SnifferState.IDLING) return false
            meta.state = SnifferState.IDLING
            return true
        }

        override fun isActive(entity: WrapperEntity): Boolean {
            val meta = entity.entityMeta as? SnifferMeta ?: return false
            return meta.state == SnifferState.SNIFFING
        }
    }

    data object PandaRolling : DisguiseAttackState(
        durationTicks = PANDA_NATURAL_ROLL_TICKS,
        resetMode = DisguiseAttackResetMode.SERVER_ONLY
    ) {
        override fun apply(entity: WrapperEntity, context: DisguiseAttackContext): Boolean {
            val meta = entity.entityMeta as? PandaMeta ?: return false
            if (meta.isRolling) return false
            meta.isRolling = true
            return true
        }

        override fun clear(entity: WrapperEntity): Boolean {
            val meta = entity.entityMeta as? PandaMeta ?: return false
            if (!meta.isRolling) return false
            meta.isRolling = false
            return true
        }

        override fun isActive(entity: WrapperEntity): Boolean {
            val meta = entity.entityMeta as? PandaMeta ?: return false
            return meta.isRolling
        }
    }

    data object ArmadilloRolledUp : DisguiseAttackState(0L, isToggle = true) {
        override fun apply(entity: WrapperEntity, context: DisguiseAttackContext): Boolean {
            val meta = entity.entityMeta as? ArmadilloMeta ?: return false
            if (meta.state == ArmadilloState.SCARED) return false
            meta.state = ArmadilloState.SCARED
            return true
        }

        override fun clear(entity: WrapperEntity): Boolean {
            val meta = entity.entityMeta as? ArmadilloMeta ?: return false
            if (meta.state == ArmadilloState.IDLE) return false
            meta.state = ArmadilloState.IDLE
            return true
        }

        override fun isActive(entity: WrapperEntity): Boolean {
            val meta = entity.entityMeta as? ArmadilloMeta ?: return false
            return meta.state == ArmadilloState.SCARED
        }
    }

    data object WolfShake : DisguiseAttackState(
        durationTicks = 0L,
        resetMode = DisguiseAttackResetMode.NONE
    ) {
        override fun apply(entity: WrapperEntity, context: DisguiseAttackContext): Boolean = true
        override fun clear(entity: WrapperEntity): Boolean = false

        override fun presentation(entity: WrapperEntity, active: Boolean): DisguiseAttackStatePresentation {
            return DisguiseAttackStatePresentation.EntityStatus(WOLF_START_SHAKE_STATUS)
        }
    }

    data object CatLying : DisguiseAttackState(0L, isToggle = true) {
        override fun apply(entity: WrapperEntity, context: DisguiseAttackContext): Boolean {
            val meta = entity.entityMeta as? CatMeta ?: return false
            if (meta.isLying) return false
            meta.isRelaxed = false
            meta.isLying = true
            return true
        }

        override fun clear(entity: WrapperEntity): Boolean {
            val meta = entity.entityMeta as? CatMeta ?: return false
            if (!meta.isLying && !meta.isRelaxed) return false
            meta.isLying = false
            meta.isRelaxed = false
            return true
        }

        override fun isActive(entity: WrapperEntity): Boolean {
            val meta = entity.entityMeta as? CatMeta ?: return false
            return meta.isLying
        }
    }

    data object BeeAggressive : DisguiseAttackState(0L, isToggle = true) {
        override fun apply(entity: WrapperEntity, context: DisguiseAttackContext): Boolean {
            val meta = entity.entityMeta as? BeeMeta ?: return false
            if (meta.angerTicks > 0) return false
            meta.angerTicks = ACTIVE_BEE_ANGER_TICKS
            return true
        }

        override fun clear(entity: WrapperEntity): Boolean {
            val meta = entity.entityMeta as? BeeMeta ?: return false
            if (meta.angerTicks == 0) return false
            meta.angerTicks = 0
            return true
        }

        override fun isActive(entity: WrapperEntity): Boolean {
            val meta = entity.entityMeta as? BeeMeta ?: return false
            return meta.angerTicks > 0
        }
    }

    data object PiglinDancing : DisguiseAttackState(0L, isToggle = true) {
        override fun apply(entity: WrapperEntity, context: DisguiseAttackContext): Boolean {
            val meta = entity.entityMeta as? PiglinMeta ?: return false
            if (meta.isDancing) return false
            meta.isDancing = true
            return true
        }

        override fun clear(entity: WrapperEntity): Boolean {
            val meta = entity.entityMeta as? PiglinMeta ?: return false
            if (!meta.isDancing) return false
            meta.isDancing = false
            return true
        }

        override fun isActive(entity: WrapperEntity): Boolean {
            val meta = entity.entityMeta as? PiglinMeta ?: return false
            return meta.isDancing
        }
    }

    data object SnowGolemPumpkin : DisguiseAttackState(0L, isToggle = true) {
        override fun apply(entity: WrapperEntity, context: DisguiseAttackContext): Boolean {
            val meta = entity.entityMeta as? SnowGolemMeta ?: return false
            if (meta.isHasPumpkinHat) return false
            meta.isHasPumpkinHat = true
            return true
        }

        override fun clear(entity: WrapperEntity): Boolean {
            val meta = entity.entityMeta as? SnowGolemMeta ?: return false
            if (!meta.isHasPumpkinHat) return false
            meta.isHasPumpkinHat = false
            return true
        }

        override fun isActive(entity: WrapperEntity): Boolean {
            val meta = entity.entityMeta as? SnowGolemMeta ?: return false
            return meta.isHasPumpkinHat
        }
    }

    companion object {
        private const val HALF_SECOND_TICKS = 10L
        // Sniffer sniffing is driven by synced state rather than a one-shot entity event,
        // so we still transition it back to idling after a short presentation window.
        private const val SNIFFER_SNIFF_TICKS = 40L
        // Panda clears its own rolling flag in vanilla after the animation finishes,
        // but we still reset the wrapper state locally so repeated triggers can resend
        // a fresh rolling snapshot without forcing a client-side cancel packet.
        private const val PANDA_NATURAL_ROLL_TICKS = 32L

        // Raw metadata fallback for 1.21.4 spellcaster casting-state; EntityLib exposes the meta
        // class, but not the tracked-byte setter itself.
        private const val SPELLCASTER_SPELL_METADATA_INDEX: Byte = 17
        private const val SPELLCASTER_ACTIVE_SPELL_ID: Byte = 1

        private const val SHULKER_CLOSED_HEIGHT: Byte = 0
        private const val SHULKER_FULLY_OPEN_HEIGHT: Byte = 100.toByte()
        private const val SHULKER_PEEK_METADATA_INDEX: Byte = 17

        private const val ACTIVE_BEE_ANGER_TICKS = Int.MAX_VALUE

        // Vanilla wolf client animation uses status 8 to start shaking; the animation finishes
        // on the client without a follow-up cancel packet.
        private const val WOLF_START_SHAKE_STATUS = 8
    }
}
