package com.ratger.acreative.commands.disguise

import com.destroystokyo.paper.ClientOption
import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.item.ItemStack as PacketItemStack
import com.github.retrooper.packetevents.protocol.player.GameMode
import com.github.retrooper.packetevents.protocol.player.UserProfile
import com.github.retrooper.packetevents.protocol.world.Direction
import com.github.retrooper.packetevents.protocol.world.painting.PaintingVariants
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState
import com.github.retrooper.packetevents.protocol.world.states.type.StateType
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes
import com.github.retrooper.packetevents.util.Vector3d
import com.github.retrooper.packetevents.util.Vector3i
import com.ratger.acreative.utils.PacketItemConversionSupport
import com.ratger.acreative.utils.PlayerDisplayNameResolver
import com.ratger.acreative.utils.ViewerTeamPacketSupport
import me.tofaa.entitylib.EntityLib
import me.tofaa.entitylib.meta.EntityMeta
import me.tofaa.entitylib.meta.Metadata
import me.tofaa.entitylib.meta.display.BlockDisplayMeta
import me.tofaa.entitylib.meta.display.ItemDisplayMeta
import me.tofaa.entitylib.meta.display.TextDisplayMeta
import me.tofaa.entitylib.meta.other.BoatMeta
import me.tofaa.entitylib.meta.other.FallingBlockMeta
import me.tofaa.entitylib.meta.other.FireworkRocketMeta
import me.tofaa.entitylib.meta.other.FishingHookMeta
import me.tofaa.entitylib.meta.other.InteractionMeta
import me.tofaa.entitylib.meta.other.ItemFrameMeta
import me.tofaa.entitylib.meta.other.PaintingMeta
import me.tofaa.entitylib.meta.other.PrimedTntMeta
import me.tofaa.entitylib.meta.projectile.ItemEntityMeta
import me.tofaa.entitylib.meta.types.LivingEntityMeta
import me.tofaa.entitylib.meta.types.PlayerMeta
import me.tofaa.entitylib.spigot.ExtraConversionUtil
import me.tofaa.entitylib.wrapper.WrapperEntity
import me.tofaa.entitylib.wrapper.WrapperLivingEntity
import me.tofaa.entitylib.wrapper.WrapperPlayer
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.inventory.MainHand
import org.bukkit.inventory.ItemStack
import java.nio.charset.StandardCharsets
import java.util.Optional
import java.util.UUID

class DisguiseEntityFactory {
    private companion object {
        const val LOOPING_TNT_FUSE_TICKS = 80
        const val NO_GRAVITY_METADATA_INDEX: Byte = 5
        const val PAINTING_VARIANT_METADATA_INDEX: Byte = 8
        val STATIC_PROJECTILE_STABILIZATION = DisguiseMotionStabilization(
            disablesGravity = true,
            sendsZeroVelocity = true,
            followUpTicks = 1L
        )
        val POWERED_PROJECTILE_STABILIZATION = STATIC_PROJECTILE_STABILIZATION.copy(
            sendsZeroProjectilePower = true
        )
    }

    sealed interface NameMode {
        data object Owner : NameMode
        data class Fixed(val component: Component) : NameMode
    }

    data class CreatedDisguise(
        val entity: WrapperEntity,
        val type: EntityType,
        val capabilities: DisguiseCapabilities,
        val nameMode: NameMode,
        val viewerTeam: ViewerTeamPacketSupport.Definition? = null,
        val renderProfile: DisguiseRenderProfile = DisguiseRenderProfile()
    )

    fun createMobDisguise(
        owner: Player,
        type: EntityType,
        preferredMainHand: ItemStack?,
        textDisplayText: Component? = null
    ): CreatedDisguise? {
        val packetType = type.toPacketEventsType() ?: return null
        val entity = createWrapper(packetType)
        val renderProfile = resolveRenderProfile(type)
        applyMetaDefaults(
            entity = entity,
            owner = owner,
            preferredMainHand = preferredMainHand,
            visibleText = PlayerDisplayNameResolver.resolve(owner),
            type = type,
            renderProfile = renderProfile,
            textDisplayText = textDisplayText
        )

        return CreatedDisguise(
            entity = entity,
            type = type,
            capabilities = DisguiseCapabilityResolver.resolve(type, entity),
            nameMode = NameMode.Owner,
            renderProfile = renderProfile
        )
    }

    fun createPlayerDisguise(
        owner: Player,
        target: Player,
        preferredMainHand: ItemStack?
    ): CreatedDisguise {
        val targetProfile = ExtraConversionUtil.getProfileFromBukkitPlayer(target)
        val fakeProfile = createFakePlayerProfile(
            ownerId = owner.uniqueId,
            targetId = target.uniqueId,
            textures = targetProfile?.textureProperties.orEmpty()
        )
        val entityId = EntityLib.getPlatform().entityIdProvider.provide(fakeProfile.uuid, EntityTypes.PLAYER)
        val visibleName = PlayerDisplayNameResolver.resolve(target)
        val wrapper = WrapperPlayer(fakeProfile, entityId).apply {
            isInTablist = false
            displayName = visibleName
            gameMode = GameMode.CREATIVE
        }

        applyPlayerMeta(wrapper, target)
        val renderProfile = resolveRenderProfile(EntityType.PLAYER)
        applyMetaDefaults(
            entity = wrapper,
            owner = owner,
            preferredMainHand = preferredMainHand,
            visibleText = visibleName,
            type = EntityType.PLAYER,
            renderProfile = renderProfile
        )

        return CreatedDisguise(
            entity = wrapper,
            type = EntityType.PLAYER,
            capabilities = DisguiseCapabilityResolver.resolve(EntityType.PLAYER, wrapper),
            nameMode = NameMode.Fixed(visibleName),
            viewerTeam = ViewerTeamPacketSupport.Definition.hidden(
                teamName = fakeTeamName(fakeProfile.name),
                entry = fakeProfile.name
            ),
            renderProfile = renderProfile
        )
    }

    private fun createWrapper(packetType: com.github.retrooper.packetevents.protocol.entity.type.EntityType): WrapperEntity {
        if (packetType == EntityTypes.PAINTING) {
            return PaintingWrapperEntity(packetType)
        }
        if (packetType == EntityTypes.WIND_CHARGE || packetType == EntityTypes.BREEZE_WIND_CHARGE) {
            return WindChargeWrapperEntity(packetType)
        }
        val metaClass = EntityMeta.getMetaClass(packetType)
        return if (LivingEntityMeta::class.java.isAssignableFrom(metaClass)) {
            WrapperLivingEntity(packetType)
        } else {
            WrapperEntity(packetType)
        }
    }

    private fun applyPlayerMeta(entity: WrapperPlayer, source: Player) {
        val meta = entity.entityMeta as? PlayerMeta ?: return
        val skinParts = source.getClientOption(ClientOption.SKIN_PARTS)
        meta.isCapeEnabled = skinParts.hasCapeEnabled()
        meta.isJacketEnabled = skinParts.hasJacketEnabled()
        meta.isLeftSleeveEnabled = skinParts.hasLeftSleeveEnabled()
        meta.isRightSleeveEnabled = skinParts.hasRightSleeveEnabled()
        meta.isLeftLegEnabled = skinParts.hasLeftPantsEnabled()
        meta.isRightLegEnabled = skinParts.hasRightPantsEnabled()
        meta.isHatEnabled = skinParts.hasHatsEnabled()
        meta.isRightHandMain = source.getClientOption(ClientOption.MAIN_HAND) == MainHand.RIGHT
    }

    private fun applyMetaDefaults(
        entity: WrapperEntity,
        owner: Player,
        preferredMainHand: ItemStack?,
        visibleText: Component,
        type: EntityType,
        renderProfile: DisguiseRenderProfile,
        textDisplayText: Component? = null
    ) {
        val meta = entity.entityMeta
        val item = resolveMirroredItem(type, preferredMainHand)

        if (renderProfile.motionStabilization.disablesGravity) {
            applyNoGravity(meta)
        }

        when (meta) {
            is ItemEntityMeta -> meta.item = item
            is ItemDisplayMeta -> meta.item = item
            is ItemFrameMeta -> {
                meta.item = item
                meta.orientation = ItemFrameMeta.Orientation.SOUTH
                meta.rotation = me.tofaa.entitylib.extras.Rotation.NONE
            }
            is BlockDisplayMeta -> meta.blockId = resolveBlockDisplayStateId(preferredMainHand)
            is FallingBlockMeta -> {
                meta.blockStateId = resolveBlockDisplayStateId(preferredMainHand)
                meta.spawnPosition = Vector3i(owner.location.blockX, owner.location.blockY, owner.location.blockZ)
            }
            is TextDisplayMeta -> meta.text = textDisplayText ?: visibleText
            is PaintingMeta -> {
                meta.direction = Direction.SOUTH
                randomEnumConstant<PaintingMeta.Type>()?.let { chosenType ->
                    meta.setType(chosenType)
                    applyPaintingVariant(meta, chosenType)
                }
            }
            is BoatMeta -> resolveBoatType(entity.entityType)?.let(meta::setType)
            is FireworkRocketMeta -> {
                meta.fireworkItem = resolveMirroredItem(EntityType.FIREWORK_ROCKET, preferredMainHand)
                meta.shooter = owner.entityId
            }
            is FishingHookMeta -> meta.setShooter(owner.entityId)
            is InteractionMeta -> {
                meta.width = 0.6f
                meta.height = 1.8f
                meta.isResponsive = false
            }
            is PrimedTntMeta -> meta.fuseTime = LOOPING_TNT_FUSE_TICKS
        }
    }

    private fun applyNoGravity(meta: EntityMeta) {
        // The shaded EntityLib helper crashes on newer protocol mappings for this flag,
        // but the base entity metadata slot remains stable for our supported versions.
        meta.setIndex(NO_GRAVITY_METADATA_INDEX, EntityDataTypes.BOOLEAN, true)
    }

    private fun applyPaintingVariant(meta: PaintingMeta, type: PaintingMeta.Type) {
        val variant = resolvePaintingVariant(type) ?: return
        meta.setIndex(PAINTING_VARIANT_METADATA_INDEX, EntityDataTypes.PAINTING_VARIANT, variant)
    }

    private fun resolvePaintingVariant(type: PaintingMeta.Type) = run {
        val byEnumName = PaintingVariants.getByName(type.name.lowercase())
        if (byEnumName != null) {
            byEnumName
        } else {
            val legacyName = type.getName().removePrefix("minecraft:")
            PaintingVariants.getByName(legacyName)
                ?: PaintingVariants.getByName("minecraft:$legacyName")
        }
    }

    private fun resolveBoatType(disguisedType: com.github.retrooper.packetevents.protocol.entity.type.EntityType): BoatMeta.Type? {
        val disguisedName = disguisedType.toString().uppercase()
        val key = when {
            disguisedType == EntityTypes.BOAT -> "OAK"
            disguisedName.contains("BAMBOO") -> "BAMBOO"
            disguisedName.contains("PALE_OAK") -> "PALE_OAK"
            disguisedName.contains("CHERRY") -> "CHERRY"
            disguisedName.contains("MANGROVE") -> "MANGROVE"
            disguisedName.contains("DARK_OAK") -> "DARK_OAK"
            disguisedName.contains("JUNGLE") -> "JUNGLE"
            disguisedName.contains("BIRCH") -> "BIRCH"
            disguisedName.contains("SPRUCE") -> "SPRUCE"
            disguisedName.contains("ACACIA") -> "ACACIA"
            else -> "OAK"
        }
        return runCatching { java.lang.Enum.valueOf(BoatMeta.Type::class.java, key) }.getOrNull()
    }

    fun resolveMirroredItem(type: EntityType, preferredMainHand: ItemStack?): PacketItemStack {
        val preferredItem = preferredMainHand
            ?.takeUnless { it.type.isAir }
            ?.let(PacketItemConversionSupport::toPacket)
            ?: packetItem(Material.AIR)

        return when (type) {
            EntityType.ARROW -> packetItem(Material.ARROW)
            EntityType.SPECTRAL_ARROW -> packetItem(Material.SPECTRAL_ARROW)
            EntityType.EGG -> packetItem(Material.EGG)
            EntityType.ENDER_PEARL -> packetItem(Material.ENDER_PEARL)
            EntityType.EXPERIENCE_BOTTLE -> packetItem(Material.EXPERIENCE_BOTTLE)
            EntityType.FIREWORK_ROCKET -> packetItem(Material.FIREWORK_ROCKET)
            EntityType.ITEM -> if (preferredMainHand == null || preferredMainHand.type.isAir) {
                packetItem(Material.OAK_PLANKS)
            } else {
                PacketItemConversionSupport.toPacket(preferredMainHand)
            }
            EntityType.ITEM_FRAME,
            EntityType.GLOW_ITEM_FRAME -> preferredItem
            EntityType.ITEM_DISPLAY -> if (preferredMainHand == null || preferredMainHand.type.isAir) {
                packetItem(Material.OAK_PLANKS)
            } else {
                PacketItemConversionSupport.toPacket(preferredMainHand)
            }
            EntityType.POTION -> packetItem(Material.SPLASH_POTION)
            EntityType.SMALL_FIREBALL,
            EntityType.FIREBALL,
            EntityType.DRAGON_FIREBALL -> packetItem(Material.FIRE_CHARGE)
            EntityType.TRIDENT -> packetItem(Material.TRIDENT)
            else -> preferredItem
        }
    }

    fun resolveBlockDisplayStateId(preferredMainHand: ItemStack?): Int {
        val stateType = preferredMainHand
            ?.type
            ?.takeIf { it.isBlock }
            ?.let { StateTypes.getByName(it.key.toString()) }
            ?: StateTypes.STONE
        return defaultBlockStateId(stateType)
    }

    private fun packetItem(material: Material): PacketItemStack {
        return PacketItemConversionSupport.toPacket(ItemStack(material))
    }

    private fun defaultBlockStateId(stateType: StateType): Int {
        return WrappedBlockState.getDefaultState(
            PacketEvents.getAPI().serverManager.version.toClientVersion(),
            stateType
        ).globalId
    }

    private fun resolveRenderProfile(type: EntityType): DisguiseRenderProfile {
        return when (type) {
            EntityType.END_CRYSTAL -> DisguiseRenderProfile(
                locationOffset = DisguiseLocationOffset(y = 1.0)
            )

            EntityType.BLOCK_DISPLAY -> DisguiseRenderProfile(
                locationOffset = DisguiseLocationOffset(x = 0.5, z = 0.5),
                mirrorsMainHandIntoBlockState = true
            )

            EntityType.FALLING_BLOCK -> DisguiseRenderProfile(
                mirrorsMainHandIntoBlockState = true,
                respawnsOnMirroredBlockStateChange = true,
                motionStabilization = STATIC_PROJECTILE_STABILIZATION
            )

            EntityType.ITEM_DISPLAY -> DisguiseRenderProfile(
                locationOffset = DisguiseLocationOffset(y = 0.5)
            )

            EntityType.TNT -> DisguiseRenderProfile(
                keepsPrimedTntLooping = true
            )

            EntityType.ARROW,
            EntityType.SPECTRAL_ARROW,
            EntityType.TRIDENT,
            EntityType.EGG,
            EntityType.SNOWBALL,
            EntityType.ENDER_PEARL,
            EntityType.POTION,
            EntityType.EXPERIENCE_BOTTLE,
            EntityType.EYE_OF_ENDER,
            EntityType.FIREWORK_ROCKET,
            EntityType.SHULKER_BULLET,
            EntityType.LLAMA_SPIT,
            EntityType.FISHING_BOBBER -> DisguiseRenderProfile(
                motionStabilization = STATIC_PROJECTILE_STABILIZATION
            )

            EntityType.FIREBALL,
            EntityType.SMALL_FIREBALL,
            EntityType.DRAGON_FIREBALL,
            EntityType.WIND_CHARGE,
            EntityType.BREEZE_WIND_CHARGE -> DisguiseRenderProfile(
                motionStabilization = POWERED_PROJECTILE_STABILIZATION
            )

            EntityType.WITHER_SKULL -> DisguiseRenderProfile(
                yawOffset = 180f,
                motionStabilization = POWERED_PROJECTILE_STABILIZATION
            )

            else -> DisguiseRenderProfile()
        }
    }

    private fun EntityType.toPacketEventsType(): com.github.retrooper.packetevents.protocol.entity.type.EntityType? {
        return EntityTypes.getByName("minecraft:${name.lowercase()}")
    }

    private inline fun <reified T : Enum<T>> randomEnumConstant(): T? {
        val values = enumValues<T>()
        if (values.isEmpty()) return null
        return values.random()
    }

    private fun createFakePlayerProfile(
        ownerId: UUID,
        targetId: UUID,
        textures: List<com.github.retrooper.packetevents.protocol.player.TextureProperty>
    ): UserProfile {
        // Keep the skin from the target player, but decouple the profile name so we can control nametag visibility ourselves.
        val disguiseUuid = UUID.nameUUIDFromBytes(
            "acreative:disguise:$ownerId:$targetId".toByteArray(StandardCharsets.UTF_8)
        )
        val profileName = "NPC_${disguiseUuid.toString().replace("-", "").take(12)}"
        return UserProfile(disguiseUuid, profileName, textures)
    }

    private fun fakeTeamName(profileName: String): String {
        return "dis_${profileName.removePrefix("NPC_").take(12)}"
    }

    private class PaintingWrapperEntity(
        packetType: com.github.retrooper.packetevents.protocol.entity.type.EntityType
    ) : WrapperEntity(packetType) {
        override fun getObjectData(): Int {
            val direction = (entityMeta as? PaintingMeta)?.direction ?: Direction.SOUTH
            return direction.ordinal
        }
    }

    private data class AllocatedIdentity(
        val entityId: Int,
        val uuid: UUID
    )

    private class WindChargeWrapperEntity private constructor(
        packetType: com.github.retrooper.packetevents.protocol.entity.type.EntityType,
        identity: AllocatedIdentity
    ) : WrapperEntity(
        identity.entityId,
        identity.uuid,
        packetType,
        EntityMeta(identity.entityId, Metadata(identity.entityId))
    ) {
        constructor(packetType: com.github.retrooper.packetevents.protocol.entity.type.EntityType) : this(
            packetType,
            allocateIdentity(packetType)
        )

        override fun getObjectData(): Int = 0

        override fun createVeloPacket(): Optional<Vector3d> {
            val currentVelocity = velocity ?: return Optional.empty()
            return if (currentVelocity.x == 0.0 && currentVelocity.y == 0.0 && currentVelocity.z == 0.0) {
                Optional.empty()
            } else {
                Optional.of(currentVelocity)
            }
        }

        companion object {
            private fun allocateIdentity(
                packetType: com.github.retrooper.packetevents.protocol.entity.type.EntityType
            ): AllocatedIdentity {
                val uuid = EntityLib.getPlatform().entityUuidProvider.provide(packetType)
                val entityId = EntityLib.getPlatform().entityIdProvider.provide(uuid, packetType)
                return AllocatedIdentity(entityId = entityId, uuid = uuid)
            }
        }
    }
}
