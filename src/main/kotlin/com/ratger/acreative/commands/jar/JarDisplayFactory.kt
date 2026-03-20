package com.ratger.acreative.commands.jar

import com.destroystokyo.paper.profile.ProfileProperty
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Display
import org.bukkit.entity.EntityType
import org.bukkit.entity.ItemDisplay
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.joml.Matrix4f
import java.util.Base64
import java.util.UUID

internal class JarDisplayFactory(private val hooker: FunctionHooker) {

    fun createDisplayParts(targetUuid: UUID, visualOrigin: Location): MutableList<ItemDisplay> {
        val target = Bukkit.getPlayer(targetUuid)
        val world = visualOrigin.world ?: return mutableListOf()

        return JarDisplayDefinition.parts.map { part ->
            val display = world.spawnEntity(visualOrigin, EntityType.ITEM_DISPLAY) as ItemDisplay
            display.setItemStack(createHead(part.textureUrl))
            display.itemDisplayTransform = ItemDisplay.ItemDisplayTransform.NONE
            display.billboard = Display.Billboard.FIXED
            display.interpolationDelay = 0
            display.interpolationDuration = 0
            display.teleportDuration = 0
            display.isSilent = true
            display.setGravity(false)
            display.setTransformationMatrix(
                Matrix4f(
                    decodeMatrixComponent(part.matrix[0]),
                    decodeMatrixComponent(part.matrix[1]),
                    decodeMatrixComponent(part.matrix[2]),
                    decodeMatrixComponent(part.matrix[3]),
                    decodeMatrixComponent(part.matrix[4]),
                    decodeMatrixComponent(part.matrix[5]),
                    decodeMatrixComponent(part.matrix[6]),
                    decodeMatrixComponent(part.matrix[7]),
                    decodeMatrixComponent(part.matrix[8]),
                    decodeMatrixComponent(part.matrix[9]),
                    decodeMatrixComponent(part.matrix[10]),
                    decodeMatrixComponent(part.matrix[11]),
                    decodeMatrixComponent(part.matrix[12]),
                    decodeMatrixComponent(part.matrix[13]),
                    decodeMatrixComponent(part.matrix[14]),
                    decodeMatrixComponent(part.matrix[15])
                )
            )

            world.players.forEach { viewer ->
                if (!viewer.isOnline || target == null) return@forEach
                if (hooker.utils.isHiddenFromPlayer(viewer, target)) {
                    viewer.hideEntity(hooker.plugin, display)
                } else {
                    viewer.showEntity(hooker.plugin, display)
                }
            }

            display
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

    private fun decodeMatrixComponent(value: Float): Float {
        return if (kotlin.math.abs(value) > FIXED_POINT_THRESHOLD) value / MATRIX_FIXED_POINT_SCALE else value
    }

    private companion object {
        const val FIXED_POINT_THRESHOLD = 100f
        const val MATRIX_FIXED_POINT_SCALE = 20000f
    }
}
