package com.ratger.acreative.commands.jar

import com.destroystokyo.paper.profile.ProfileProperty
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Display
import org.bukkit.entity.EntityType
import org.bukkit.entity.ItemDisplay
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.joml.Matrix4f
import java.nio.FloatBuffer
import java.util.UUID

internal class JarDisplayFactory(private val hooker: FunctionHooker) {

    data class JarDisplayGroup(
        val rootAnchor: BlockDisplay,
        val parts: MutableList<ItemDisplay>
    )

    fun createDisplayParts(targetUuid: UUID, visualOrigin: Location): JarDisplayGroup {
        val target = Bukkit.getPlayer(targetUuid)
        val world = requireNotNull(visualOrigin.world) { "Jar visual origin must have world" }

        val rootAnchor = world.spawnEntity(visualOrigin, EntityType.BLOCK_DISPLAY) as BlockDisplay
        rootAnchor.setBlock(Material.AIR.createBlockData())
        rootAnchor.billboard = Display.Billboard.FIXED
        rootAnchor.interpolationDelay = 0
        rootAnchor.interpolationDuration = 0
        rootAnchor.teleportDuration = 0
        rootAnchor.isSilent = true
        rootAnchor.setGravity(false)

        world.players.forEach { viewer ->
            if (!viewer.isOnline || target == null) return@forEach
            if (hooker.utils.isHiddenFromPlayer(viewer, target)) {
                viewer.hideEntity(hooker.plugin, rootAnchor)
            } else {
                viewer.showEntity(hooker.plugin, rootAnchor)
            }
        }

        val parts = JarDisplayDefinition.parts.mapIndexed { index, part ->
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
            rootAnchor.addPassenger(display)

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

        return JarDisplayGroup(rootAnchor = rootAnchor, parts = parts)
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
