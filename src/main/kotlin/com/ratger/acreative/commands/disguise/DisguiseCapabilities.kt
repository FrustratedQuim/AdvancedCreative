package com.ratger.acreative.commands.disguise

import me.tofaa.entitylib.meta.types.ObjectData
import me.tofaa.entitylib.wrapper.WrapperEntity
import org.bukkit.entity.EntityType

sealed interface DisguiseAttackAnimation {
    data object None : DisguiseAttackAnimation
    data object SwingMainHand : DisguiseAttackAnimation
    data class EntityStatus(val status: Int) : DisguiseAttackAnimation
}

data class DisguiseCapabilities(
    val attackAnimation: DisguiseAttackAnimation = DisguiseAttackAnimation.None,
    val attackState: DisguiseAttackState = DisguiseAttackState.None,
    val supportsHeadRotation: Boolean = false,
    val supportsMainHandEquipment: Boolean = false,
    val supportsOffHandEquipment: Boolean = false,
    val supportsArmorEquipment: Boolean = false,
    val supportsSneakState: Boolean = false,
    val mirrorsMainHandIntoItemMetadata: Boolean = false,
    val requiresSpawnVelocity: Boolean = false,
    val tracksVelocityContinuously: Boolean = false
) {
    val supportsEquipment: Boolean
        get() = supportsMainHandEquipment || supportsOffHandEquipment || supportsArmorEquipment
}

object DisguiseCapabilityResolver {
    private const val ATTACK_ENTITY_STATUS = 4

    private val humanoidCombatPreset = DisguiseCapabilities(
        attackAnimation = DisguiseAttackAnimation.SwingMainHand,
        supportsHeadRotation = true,
        supportsMainHandEquipment = true,
        supportsOffHandEquipment = true,
        supportsArmorEquipment = true,
        supportsSneakState = true
    )

    private val humanoidUtilityPreset = humanoidCombatPreset.copy(
        attackAnimation = DisguiseAttackAnimation.None
    )

    private val staticEquipmentPreset = DisguiseCapabilities(
        supportsMainHandEquipment = true,
        supportsOffHandEquipment = true,
        supportsArmorEquipment = true
    )

    private val handOnlyPreset = DisguiseCapabilities(
        attackAnimation = DisguiseAttackAnimation.SwingMainHand,
        supportsHeadRotation = true,
        supportsMainHandEquipment = true,
        supportsSneakState = true
    )

    private val humanoidStatusAttackPreset = humanoidCombatPreset.copy(
        attackAnimation = DisguiseAttackAnimation.EntityStatus(ATTACK_ENTITY_STATUS)
    )

    private val humanoidSpellcastingPreset = humanoidCombatPreset.copy(
        attackAnimation = DisguiseAttackAnimation.None,
        attackState = DisguiseAttackState.Spellcasting
    )

    private val humanoidAggressivePosePreset = humanoidCombatPreset.copy(
        attackState = DisguiseAttackState.MobAggressive
    )

    private val livingPreset = DisguiseCapabilities(
        supportsHeadRotation = true,
        supportsSneakState = true
    )

    private val livingStatusAttackPreset = livingPreset.copy(
        attackAnimation = DisguiseAttackAnimation.EntityStatus(ATTACK_ENTITY_STATUS)
    )

    private val polarBearPreset = livingPreset.copy(
        attackState = DisguiseAttackState.PolarBearStanding
    )

    private val endermanPreset = livingPreset.copy(
        attackState = DisguiseAttackState.EndermanAggressive
    )

    private val pufferfishPreset = livingPreset.copy(
        attackState = DisguiseAttackState.FullyPuffedPufferfish
    )

    private val snifferPreset = livingPreset.copy(
        attackState = DisguiseAttackState.SnifferSniffing
    )

    private val pandaPreset = livingPreset.copy(
        attackState = DisguiseAttackState.PandaRolling
    )

    private val armadilloPreset = livingPreset.copy(
        attackState = DisguiseAttackState.ArmadilloRolledUp
    )

    private val wolfPreset = livingPreset.copy(
        attackState = DisguiseAttackState.WolfShake
    )

    private val catPreset = livingPreset.copy(
        attackState = DisguiseAttackState.CatLying
    )

    private val beePreset = livingPreset.copy(
        attackState = DisguiseAttackState.BeeAggressive
    )

    private val foxPreset = livingPreset.copy(
        supportsMainHandEquipment = true
    )

    private val snowGolemPreset = livingPreset.copy(
        attackState = DisguiseAttackState.SnowGolemPumpkin
    )

    private val staticLivingPreset = DisguiseCapabilities(
        supportsSneakState = true
    )

    private val shulkerPreset = staticLivingPreset.copy(
        attackState = DisguiseAttackState.ShulkerOpenShell
    )

    private val evokerFangsPreset = DisguiseCapabilities(
        attackAnimation = DisguiseAttackAnimation.EntityStatus(ATTACK_ENTITY_STATUS)
    )

    private val noCapabilitiesPreset = DisguiseCapabilities()

    private val itemMetadataPreset = DisguiseCapabilities(
        mirrorsMainHandIntoItemMetadata = true
    )

    private val spawnVelocityOnlyPreset = DisguiseCapabilities(
        requiresSpawnVelocity = true
    )

    fun resolve(type: EntityType, entity: WrapperEntity): DisguiseCapabilities {
        // Manual 1.21.4 capability table: we allow every entity type, but only emit packets
        // that are sane for that disguise group instead of keeping a global hard blacklist.
        val base = when (type) {
            EntityType.PLAYER,
            EntityType.ZOMBIE,
            EntityType.HUSK,
            EntityType.DROWNED,
            EntityType.ZOMBIE_VILLAGER,
            EntityType.ZOMBIFIED_PIGLIN,
            EntityType.GIANT,
            EntityType.SKELETON,
            EntityType.STRAY,
            EntityType.WITHER_SKELETON,
            EntityType.BOGGED,
            EntityType.PILLAGER,
            EntityType.WITCH,
            EntityType.PIGLIN,
            EntityType.PIGLIN_BRUTE -> humanoidCombatPreset

            EntityType.EVOKER,
            EntityType.ILLUSIONER -> humanoidSpellcastingPreset

            EntityType.VINDICATOR -> humanoidAggressivePosePreset

            EntityType.CREAKING -> humanoidStatusAttackPreset

            EntityType.VILLAGER,
            EntityType.WANDERING_TRADER -> humanoidUtilityPreset

            EntityType.ARMOR_STAND -> staticEquipmentPreset

            EntityType.ALLAY,
            EntityType.VEX -> handOnlyPreset

            EntityType.ELDER_GUARDIAN,
            EntityType.CREEPER,
            EntityType.SPIDER,
            EntityType.CAVE_SPIDER,
            EntityType.SILVERFISH,
            EntityType.BLAZE,
            EntityType.BAT,
            EntityType.ENDERMITE,
            EntityType.GUARDIAN,
            EntityType.PIG,
            EntityType.SHEEP,
            EntityType.COW,
            EntityType.CHICKEN,
            EntityType.SQUID,
            EntityType.MOOSHROOM,
            EntityType.OCELOT,
            EntityType.HORSE,
            EntityType.SKELETON_HORSE,
            EntityType.ZOMBIE_HORSE,
            EntityType.DONKEY,
            EntityType.MULE,
            EntityType.RABBIT,
            EntityType.GHAST,
            EntityType.LLAMA,
            EntityType.TRADER_LLAMA,
            EntityType.PARROT,
            EntityType.TURTLE,
            EntityType.PHANTOM,
            EntityType.COD,
            EntityType.SALMON,
            EntityType.TROPICAL_FISH,
            EntityType.DOLPHIN,
            EntityType.STRIDER,
            EntityType.AXOLOTL,
            EntityType.GLOW_SQUID,
            EntityType.GOAT,
            EntityType.TADPOLE,
            EntityType.CAMEL,
            EntityType.BREEZE,
            EntityType.WITHER -> livingPreset

            EntityType.ENDERMAN -> endermanPreset

            EntityType.POLAR_BEAR -> polarBearPreset

            EntityType.PUFFERFISH -> pufferfishPreset

            EntityType.SNIFFER -> snifferPreset

            EntityType.PANDA -> pandaPreset

            EntityType.ARMADILLO -> armadilloPreset

            EntityType.WOLF -> wolfPreset

            EntityType.SLIME,
            EntityType.MAGMA_CUBE -> livingPreset

            EntityType.CAT -> catPreset

            EntityType.BEE -> beePreset

            EntityType.FOX -> foxPreset

            EntityType.SNOW_GOLEM -> snowGolemPreset

            EntityType.FROG -> livingPreset

            EntityType.IRON_GOLEM,
            EntityType.WARDEN,
            EntityType.RAVAGER,
            EntityType.HOGLIN,
            EntityType.ZOGLIN -> livingStatusAttackPreset

            EntityType.ENDER_DRAGON -> staticLivingPreset

            EntityType.SHULKER -> shulkerPreset

            EntityType.EVOKER_FANGS -> evokerFangsPreset

            EntityType.ITEM,
            EntityType.ITEM_FRAME,
            EntityType.GLOW_ITEM_FRAME,
            EntityType.ITEM_DISPLAY -> itemMetadataPreset

            EntityType.WIND_CHARGE,
            EntityType.BREEZE_WIND_CHARGE -> spawnVelocityOnlyPreset

            EntityType.EXPERIENCE_ORB,
            EntityType.AREA_EFFECT_CLOUD,
            EntityType.EGG,
            EntityType.LEASH_KNOT,
            EntityType.PAINTING,
            EntityType.ARROW,
            EntityType.SNOWBALL,
            EntityType.FIREBALL,
            EntityType.SMALL_FIREBALL,
            EntityType.ENDER_PEARL,
            EntityType.EYE_OF_ENDER,
            EntityType.POTION,
            EntityType.EXPERIENCE_BOTTLE,
            EntityType.WITHER_SKULL,
            EntityType.TNT,
            EntityType.FALLING_BLOCK,
            EntityType.FIREWORK_ROCKET,
            EntityType.SPECTRAL_ARROW,
            EntityType.SHULKER_BULLET,
            EntityType.DRAGON_FIREBALL,
            EntityType.COMMAND_BLOCK_MINECART,
            EntityType.MINECART,
            EntityType.CHEST_MINECART,
            EntityType.FURNACE_MINECART,
            EntityType.TNT_MINECART,
            EntityType.HOPPER_MINECART,
            EntityType.SPAWNER_MINECART,
            EntityType.LLAMA_SPIT,
            EntityType.END_CRYSTAL,
            EntityType.TRIDENT,
            EntityType.MARKER,
            EntityType.BLOCK_DISPLAY,
            EntityType.INTERACTION,
            EntityType.TEXT_DISPLAY,
            EntityType.OMINOUS_ITEM_SPAWNER,
            EntityType.ACACIA_BOAT,
            EntityType.ACACIA_CHEST_BOAT,
            EntityType.BAMBOO_RAFT,
            EntityType.BAMBOO_CHEST_RAFT,
            EntityType.BIRCH_BOAT,
            EntityType.BIRCH_CHEST_BOAT,
            EntityType.CHERRY_BOAT,
            EntityType.CHERRY_CHEST_BOAT,
            EntityType.DARK_OAK_BOAT,
            EntityType.DARK_OAK_CHEST_BOAT,
            EntityType.JUNGLE_BOAT,
            EntityType.JUNGLE_CHEST_BOAT,
            EntityType.MANGROVE_BOAT,
            EntityType.MANGROVE_CHEST_BOAT,
            EntityType.OAK_BOAT,
            EntityType.OAK_CHEST_BOAT,
            EntityType.PALE_OAK_BOAT,
            EntityType.PALE_OAK_CHEST_BOAT,
            EntityType.SPRUCE_BOAT,
            EntityType.SPRUCE_CHEST_BOAT,
            EntityType.FISHING_BOBBER,
            EntityType.LIGHTNING_BOLT,
            EntityType.UNKNOWN -> noCapabilitiesPreset
        }

        val objectDataMeta = entity.entityMeta as? ObjectData
        return base.copy(
            // Object entities may require a non-zero spawn velocity packet, but continuous velocity
            // syncing causes heavy client-side jitter for disguise use-cases.
            requiresSpawnVelocity = base.requiresSpawnVelocity || objectDataMeta?.requiresVelocityPacketAtSpawn() == true
        )
    }
}
