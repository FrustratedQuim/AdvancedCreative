package com.ratger.acreative.commands.freeze

import org.bukkit.command.CommandSender
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.core.MessageKey
import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.world.Location
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes
import com.github.retrooper.packetevents.util.Quaternion4f
import com.github.retrooper.packetevents.util.Vector3f
import com.ratger.acreative.core.FunctionHooker
import me.tofaa.entitylib.meta.display.BlockDisplayMeta
import me.tofaa.entitylib.wrapper.WrapperEntity
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import com.ratger.acreative.utils.PlayerStateManager.PlayerStateType
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class FreezeManager(private val hooker: FunctionHooker) {

    val frozenPlayers = ConcurrentHashMap<Player, MutableList<WrapperEntity>>()
    private val freezeTaskIds = ConcurrentHashMap<Player, Int>()
    val hiddenFreezeBlocks = ConcurrentHashMap<UUID, MutableMap<UUID, MutableList<WrapperEntity>>>()

    fun prepareToFreezePlayer(initiator: Player, targetName: String?) {
        if (targetName == null || !initiator.hasPermission("advancedcreative.freeze.other")) {
            hooker.playerStateManager.activateState(initiator, PlayerStateType.FROZEN)
            freezePlayer(initiator, initiator)
            return
        }

        val target = Bukkit.getPlayer(targetName)
        if (target == null) {
            hooker.messageManager.sendChat(initiator, MessageKey.ERROR_UNKNOWN_PLAYER)
            return
        }

        hooker.playerStateManager.activateState(target, PlayerStateType.FROZEN)
        freezePlayer(target, initiator)
    }

    fun freezePlayer(player: Player, initiator: Player? = null) {
        if (player.gameMode == GameMode.SPECTATOR) {
            hooker.playerStateManager.deactivateState(player, PlayerStateType.FROZEN)
            return
        }
        if (frozenPlayers.containsKey(player)) {
            unfreezePlayer(player)
            return
        }
        val blocks = spawnFreezeBlocks(player)
        frozenPlayers[player] = blocks

        if (hooker.utils.isGlowing(player)) {
            blocks.forEach { it.entityMeta.isGlowing = true }
        }

        for (hider in Bukkit.getOnlinePlayers()) {
            if (hider != player && hooker.utils.isHiddenFromPlayer(hider, player)) {
                val hiddenBlocks = hiddenFreezeBlocks.computeIfAbsent(hider.uniqueId) { ConcurrentHashMap() }
                hiddenBlocks[player.uniqueId] = blocks
                blocks.forEach { it.removeViewer(hider.uniqueId) }
            }
        }

        val taskId = hooker.tickScheduler.runRepeating(0L, 20L) {
            if (!frozenPlayers.containsKey(player) || !player.isOnline) {
                unfreezePlayer(player)
                return@runRepeating
            }
            player.freezeTicks = player.maxFreezeTicks * 2
        }
        freezeTaskIds[player] = taskId

        if (initiator == null || initiator == player) {
            hooker.messageManager.sendChat(player, MessageKey.SUCCESS_FREEZE_SELF)
        } else {
            hooker.messageManager.sendChat(
                initiator,
                MessageKey.SUCCESS_FREEZE,
                variables = mapOf("target" to player.name)
            )
        }
    }

    fun unfreezePlayer(player: Player) {
        frozenPlayers[player]?.forEach {
            it.entityMeta.isGlowing = false
            it.remove()
        }
        frozenPlayers.remove(player)
        player.freezeTicks = 0

        for (hider in Bukkit.getOnlinePlayers()) {
            val hiddenBlocks = hiddenFreezeBlocks[hider.uniqueId]
            if (hiddenBlocks != null && hiddenBlocks.containsKey(player.uniqueId)) {
                if (!hooker.utils.isHiddenFromPlayer(hider, player)) {
                    hiddenBlocks[player.uniqueId]?.forEach { block ->
                        if (block.isSpawned) {
                            block.addViewer(hider.uniqueId)
                        }
                    }
                }
                hiddenBlocks.remove(player.uniqueId)
                if (hiddenBlocks.isEmpty()) {
                    hiddenFreezeBlocks.remove(hider.uniqueId)
                }
            }
        }

        freezeTaskIds[player]?.let { taskId ->
            hooker.tickScheduler.cancel(taskId)
            freezeTaskIds.remove(player)
        }
        hooker.playerStateManager.deactivateState(player, PlayerStateType.FROZEN)
    }

    private fun spawnFreezeBlocks(player: Player): MutableList<WrapperEntity> {
        val location = player.location
        val blocks = mutableListOf<WrapperEntity>()

        for (viewer in location.world?.players?.filter { it.isOnline } ?: emptyList()) {
            if (viewer != player && hooker.utils.isHiddenFromPlayer(viewer, player)) {
                continue
            }
            viewer.playSound(
                location,
                org.bukkit.Sound.BLOCK_ANVIL_LAND,
                1f,
                Random.nextDouble(0.8, 1.2).toFloat()
            )
        }

        val scale = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_SCALE)?.value ?: 1.0
        val blockCount = 4
        val api = PacketEvents.getAPI()
        val blockState = WrappedBlockState.getDefaultState(
            api.serverManager.version.toClientVersion(),
            StateTypes.ICE
        )

        val hitboxRadius = 0.05 * scale
        val centerBias = -0.1 * scale
        val minY = 0.2 * scale
        val maxY = 1.8 * scale
        val availableHeight = maxY - minY
        val sectorHeight = availableHeight / blockCount

        val allCombos = listOf(
            Pair(1, 1),
            Pair(1, -1),
            Pair(-1, -1),
            Pair(-1, 1)
        ).shuffled()
        val possibleCombos = allCombos.take(blockCount).shuffled()

        val usedOffsets = mutableListOf<Pair<Double, Double>>()

        val baseDirection = if (player.isGliding) {
            player.location.direction.normalize()
        } else {
            org.bukkit.util.Vector(0.0, 1.0, 0.0)
        }

        repeat(blockCount) { i ->
            val baseDist = minY + i * sectorHeight
            val offsetAlongDir = baseDist + Random.nextDouble(-0.1, 0.1) * scale

            val combo = possibleCombos[i]
            var offsetX: Double
            var offsetZ: Double
            var attempts = 0

            do {
                offsetX = combo.first * Random.nextDouble(0.0, hitboxRadius)
                offsetZ = combo.second * Random.nextDouble(0.0, hitboxRadius)
                attempts++
            } while (
                attempts < 20 &&
                usedOffsets.any { (ux, uz) ->
                    val dx = offsetX - ux
                    val dz = offsetZ - uz
                    sqrt(dx * dx + dz * dz) < 0.15 * scale
                }
            )

            usedOffsets.add(offsetX to offsetZ)

            val blockLoc = location.clone()
                .add(baseDirection.clone().multiply(offsetAlongDir + centerBias))
                .add(offsetX, 0.0, offsetZ)

            val yaw = Random.nextFloat() * 360f
            val pitch = Random.nextFloat() * 180f - 90f
            val size = (Random.nextDouble(0.5, 0.6) * scale).toFloat()

            val entity = WrapperEntity(EntityTypes.BLOCK_DISPLAY)
            val blockMeta = entity.entityMeta as BlockDisplayMeta
            blockMeta.blockId = blockState.globalId
            blockMeta.scale = Vector3f(size, size, size)

            val yawRad = Math.toRadians(yaw.toDouble()).toFloat()
            val pitchRad = Math.toRadians(pitch.toDouble()).toFloat()
            val cy = cos(yawRad * 0.5f)
            val sy = sin(yawRad * 0.5f)
            val cp = cos(pitchRad * 0.5f)
            val sp = sin(pitchRad * 0.5f)
            blockMeta.rightRotation = Quaternion4f(
                sp * cy,
                cp * sy,
                -sp * sy,
                cp * cy
            )

            val packetLoc = Location(
                blockLoc.x,
                blockLoc.y,
                blockLoc.z,
                blockLoc.yaw,
                blockLoc.pitch
            )

            entity.addViewer(player.uniqueId)
            for (viewer in location.world?.players?.filter { it.isOnline && it != player && !hooker.utils.isHiddenFromPlayer(it, player) } ?: emptyList()) {
                entity.addViewer(viewer.uniqueId)
            }
            entity.spawn(packetLoc)

            blocks.add(entity)
        }
        return blocks
    }

    fun updateIceGlowing(player: Player, isGlowing: Boolean) {
        frozenPlayers[player]?.forEach { block ->
            block.entityMeta.isGlowing = isGlowing && !hiddenFreezeBlocks.any { it.value.containsKey(player.uniqueId) }
        }
    }
}


class FreezeCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.FREEZE) {
    override fun handle(player: Player, args: Array<out String>) = hooker.freezeManager.prepareToFreezePlayer(player, args.firstOrNull())

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return if (sender.hasPermission("advancedcreative.freeze.other")) {
            if (args.size == 1 || args.size == 2) {
                Bukkit.getOnlinePlayers()
                    .map { it.name }
                    .filter { it.startsWith(args[args.size - 1], ignoreCase = true) }
            } else {
                emptyList()
            }
        } else {
            emptyList()
        }
    }
}
