package com.ratger.acreative.commands.jar

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.world.Location as PacketLocation
import com.github.retrooper.packetevents.util.Quaternion4f
import com.github.retrooper.packetevents.util.Vector3f
import com.destroystokyo.paper.profile.ProfileProperty
import com.ratger.acreative.core.FunctionHooker
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import me.tofaa.entitylib.meta.display.AbstractDisplayMeta
import me.tofaa.entitylib.meta.display.ItemDisplayMeta
import me.tofaa.entitylib.wrapper.WrapperEntity
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.util.UUID
import java.util.Base64
import kotlin.math.sqrt

internal class JarDisplayFactory(private val hooker: FunctionHooker) {

    fun createDisplayParts(targetUuid: UUID, visualOrigin: Location): MutableList<WrapperEntity> {
        val target = Bukkit.getPlayer(targetUuid)
        val world = visualOrigin.world ?: return mutableListOf()
        val viewers = world.players.filter { viewer ->
            viewer.isOnline && (target == null || !hooker.utils.isHiddenFromPlayer(viewer, target))
        }

        return JarDisplayDefinition.parts.map { part ->
            val wrapper = WrapperEntity(EntityTypes.ITEM_DISPLAY)
            val meta = wrapper.entityMeta as ItemDisplayMeta
            val decomposed = DecomposedTransform.fromMatrix(part.matrix)

            meta.item = SpigotConversionUtil.fromBukkitItemStack(createHead(part.textureUrl))
            meta.displayType = ItemDisplayMeta.DisplayType.NONE
            meta.translation = Vector3f(decomposed.translation[0], decomposed.translation[1], decomposed.translation[2])
            meta.scale = Vector3f(decomposed.scale[0], decomposed.scale[1], decomposed.scale[2])
            meta.leftRotation = decomposed.leftRotation
            meta.rightRotation = decomposed.rightRotation
            meta.billboardConstraints = AbstractDisplayMeta.BillboardConstraints.FIXED
            meta.interpolationDelay = 0
            meta.transformationInterpolationDuration = 0
            meta.positionRotationInterpolationDuration = 0

            wrapper.entityMeta.isSilent = true
            wrapper.entityMeta.setHasNoGravity(true)

            viewers.forEach { wrapper.addViewer(it.uniqueId) }
            wrapper.spawn(PacketLocation(visualOrigin.x, visualOrigin.y, visualOrigin.z, visualOrigin.yaw, visualOrigin.pitch))
            wrapper
        }.toMutableList()
    }

    private fun createHead(textureUrl: String): ItemStack {
        val head = ItemStack(Material.PLAYER_HEAD)
        val meta = head.itemMeta as? SkullMeta ?: return head
        val profile = Bukkit.createProfile(UUID.randomUUID(), "jar_head")
        profile.setProperty(ProfileProperty("textures", encodeTexture(textureUrl)))
        meta.playerProfile = profile
        head.itemMeta = meta
        return head
    }

    private fun encodeTexture(textureUrl: String): String {
        val payload = """{"textures":{"SKIN":{"url":"$textureUrl"}}}"""
        return Base64.getEncoder().encodeToString(payload.toByteArray(Charsets.UTF_8))
    }

    private data class DecomposedTransform(
        val translation: FloatArray,
        val scale: FloatArray,
        val leftRotation: Quaternion4f,
        val rightRotation: Quaternion4f
    ) {
        companion object {
            fun fromMatrix(matrix: FloatArray): DecomposedTransform {
                val m00 = matrix[0]
                val m01 = matrix[1]
                val m02 = matrix[2]
                val m10 = matrix[4]
                val m11 = matrix[5]
                val m12 = matrix[6]
                val m20 = matrix[8]
                val m21 = matrix[9]
                val m22 = matrix[10]

                var sx = sqrt((m00 * m00 + m10 * m10 + m20 * m20).toDouble()).toFloat().coerceAtLeast(0.000001f)
                var sy = sqrt((m01 * m01 + m11 * m11 + m21 * m21).toDouble()).toFloat().coerceAtLeast(0.000001f)
                var sz = sqrt((m02 * m02 + m12 * m12 + m22 * m22).toDouble()).toFloat().coerceAtLeast(0.000001f)

                sx *= signedColumnFactor(m00, m10, m20)
                sy *= signedColumnFactor(m01, m11, m21)
                sz *= signedColumnFactor(m02, m12, m22)

                val r00 = m00 / sx
                val r01 = m01 / sy
                val r02 = m02 / sz
                val r10 = m10 / sx
                val r11 = m11 / sy
                val r12 = m12 / sz
                val r20 = m20 / sx
                val r21 = m21 / sy
                val r22 = m22 / sz

                return DecomposedTransform(
                    translation = floatArrayOf(matrix[3], matrix[7], matrix[11]),
                    scale = floatArrayOf(sx, sy, sz),
                    leftRotation = Quaternion4f(0f, 0f, 0f, 1f),
                    rightRotation = quaternionFromRotationMatrix(r00, r01, r02, r10, r11, r12, r20, r21, r22)
                )
            }

            private fun signedColumnFactor(a: Float, b: Float, c: Float): Float {
                val epsilon = 0.000001f
                return when {
                    kotlin.math.abs(a) > epsilon -> if (a < 0f) -1f else 1f
                    kotlin.math.abs(b) > epsilon -> if (b < 0f) -1f else 1f
                    kotlin.math.abs(c) > epsilon -> if (c < 0f) -1f else 1f
                    else -> 1f
                }
            }

            private fun quaternionFromRotationMatrix(
                m00: Float,
                m01: Float,
                m02: Float,
                m10: Float,
                m11: Float,
                m12: Float,
                m20: Float,
                m21: Float,
                m22: Float
            ): Quaternion4f {
                val trace = m00 + m11 + m22
                return if (trace > 0f) {
                    val s = sqrt((trace + 1.0f).toDouble()).toFloat() * 2f
                    Quaternion4f((m21 - m12) / s, (m02 - m20) / s, (m10 - m01) / s, 0.25f * s)
                } else if (m00 > m11 && m00 > m22) {
                    val s = sqrt((1.0f + m00 - m11 - m22).toDouble()).toFloat() * 2f
                    Quaternion4f(0.25f * s, (m01 + m10) / s, (m02 + m20) / s, (m21 - m12) / s)
                } else if (m11 > m22) {
                    val s = sqrt((1.0f + m11 - m00 - m22).toDouble()).toFloat() * 2f
                    Quaternion4f((m01 + m10) / s, 0.25f * s, (m12 + m21) / s, (m02 - m20) / s)
                } else {
                    val s = sqrt((1.0f + m22 - m00 - m11).toDouble()).toFloat() * 2f
                    Quaternion4f((m02 + m20) / s, (m12 + m21) / s, 0.25f * s, (m10 - m01) / s)
                }
            }
        }
    }
}
