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
import java.nio.FloatBuffer
import java.util.UUID

internal class JarDisplayFactory(private val hooker: FunctionHooker) {

    fun createDisplayParts(targetUuid: UUID, visualOrigin: Location): MutableList<ItemDisplay> {
        val target = Bukkit.getPlayer(targetUuid)
        val world = visualOrigin.world ?: return mutableListOf()

        return JarDisplayDefinition.parts.mapIndexed { index, part ->
            hooker.plugin.logger.info("[JarDisplay] part #$index texture=${part.textureValue.take(24)}...")

            val display = world.spawnEntity(visualOrigin, EntityType.ITEM_DISPLAY) as ItemDisplay
            display.setItemStack(createHead(part.textureValue))
            display.itemDisplayTransform = ItemDisplay.ItemDisplayTransform.NONE
            display.billboard = Display.Billboard.FIXED
            display.interpolationDelay = 0
            display.interpolationDuration = 0
            display.teleportDuration = 0
            display.isSilent = true
            display.setGravity(false)
            val matrix = Matrix4f().setTransposed(FloatBuffer.wrap(part.matrix))
            display.setTransformationMatrix(matrix)

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

    private fun createHead(textureValue: String): ItemStack {
        val head = ItemStack(Material.PLAYER_HEAD)
        val meta = head.itemMeta as? SkullMeta ?: return head
        val profile = Bukkit.createProfile(UUID.randomUUID(), "jar_head")
        profile.setProperty(ProfileProperty("textures", textureValue))
        meta.playerProfile = profile
        head.itemMeta = meta
        return head
    }
}
