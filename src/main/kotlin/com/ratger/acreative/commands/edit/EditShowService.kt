package com.ratger.acreative.commands.edit

import io.papermc.paper.datacomponent.DataComponentTypes
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.block.Lockable
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.meta.BlockStateMeta
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.inventory.meta.SkullMeta

class EditShowService {
    private val mini = MiniMessage.miniMessage()
    private val plain = PlainTextComponentSerializer.plainText()

    fun render(player: Player, context: EditContext): List<Component> {
        val item = context.item
        val meta = item.itemMeta
        val out = mutableListOf<Component>()
        out += mini.deserialize("<#FFD700><b>/edit show</b> <gray>- ${context.snapshot.type} x${item.amount}")
        out += mini.deserialize("<gray>name: <white>${meta?.displayName()?.let(plain::serialize) ?: "<none>"}")
        out += mini.deserialize("<gray>lore lines: <white>${meta?.lore()?.size ?: 0}")
        (meta?.lore() ?: emptyList()).forEachIndexed { index, line ->
            out += mini.deserialize("<gray>  [$index] <white>${plain.serialize(line)}")
        }
        if (meta is Damageable) {
            val maxDamageText = if (meta.hasMaxDamage()) meta.maxDamage.toString() else "<none>"
            out += mini.deserialize("<gray>damage: <white>${meta.damage}, max_damage: <white>$maxDamageText")
        }
        out += mini.deserialize("<gray>unbreakable: <white>${meta?.isUnbreakable == true}")
        out += mini.deserialize("<gray>glider: <white>${runCatching { meta?.isGlider == true }.getOrDefault(false)}")
        out += mini.deserialize("<gray>item_model: <white>${runCatching { meta?.itemModel?.asString() ?: "<none>" }.getOrDefault("<none>")}")
        out += mini.deserialize("<gray>max_stack_size: <white>${runCatching { if (meta?.hasMaxStackSize() == true) meta.maxStackSize.toString() else "<none>" }.getOrDefault("<none>")}")
        out += mini.deserialize("<gray>rarity: <white>${runCatching { meta?.rarity?.name ?: "<none>" }.getOrDefault("<none>")}")
        out += mini.deserialize("<gray>tooltip_style: <white>${runCatching { meta?.tooltipStyle?.asString() ?: "basic" }.getOrDefault("basic")}")
        out += mini.deserialize("<gray>hide_tooltip: <white>${runCatching { meta?.isHideTooltip == true }.getOrDefault(false)}")
        out += mini.deserialize("<gray>enchantment_glint_override: <white>${runCatching { meta?.enchantmentGlintOverride?.toString() ?: "default" }.getOrDefault("default")}")
        val consumable = item.getData(DataComponentTypes.CONSUMABLE)
        if (consumable == null) {
            out += mini.deserialize("<gray>consumable: <white><none>")
        } else {
            out += mini.deserialize(
                "<gray>consumable: <white>seconds=${consumable.consumeSeconds()}, animation=${consumable.animation()}, particles=${consumable.hasConsumeParticles()}, sound=${consumable.sound() ?: "<default>"}"
            )
            out += mini.deserialize("<gray>consumable effects: <white>${consumable.consumeEffects().size}")
            consumable.consumeEffects().forEachIndexed { index, effect ->
                out += mini.deserialize("<gray>consumable[$index]: <white>${EditConsumeEffectsAdapter.render(effect)}")
            }
        }
        val deathProtection = item.getData(DataComponentTypes.DEATH_PROTECTION)
        if (deathProtection == null) {
            out += mini.deserialize("<gray>death_protection: <white><none>")
        } else {
            out += mini.deserialize("<gray>death_protection: <white>enabled, effects=${deathProtection.deathEffects().size}")
            deathProtection.deathEffects().forEachIndexed { index, effect ->
                out += mini.deserialize("<gray>death_protection[$index]: <white>${EditConsumeEffectsAdapter.render(effect)}")
            }
        }
        val food = item.getData(DataComponentTypes.FOOD)
        if (food == null) {
            out += mini.deserialize("<gray>food: <white><none>")
        } else {
            out += mini.deserialize("<gray>food: <white>nutrition=${food.nutrition()}, saturation=${food.saturation()}, canAlwaysEat=${food.canAlwaysEat()}")
        }
        val remainder = item.getData(DataComponentTypes.USE_REMAINDER)
        if (remainder == null) {
            out += mini.deserialize("<gray>remainder: <white><none>")
        } else {
            val remainderItem = remainder.transformInto()
            val remainderMeta = remainderItem.itemMeta
            out += mini.deserialize("<gray>remainder: <white>${remainderItem.type.key} x${remainderItem.amount}")
            val remainderName = remainderMeta?.displayName()?.let(plain::serialize)
            if (!remainderName.isNullOrBlank()) {
                out += mini.deserialize("<gray>remainder name: <white>$remainderName")
            }
            out += mini.deserialize("<gray>remainder lore lines: <white>${remainderMeta?.lore()?.size ?: 0}")
            out += mini.deserialize("<gray>remainder enchants: <white>${remainderMeta?.enchants?.size ?: 0}")
        }
        val blockStateMeta = meta as? BlockStateMeta
        val lockable = blockStateMeta?.blockState as? Lockable
        if (lockable?.isLocked != true) {
            out += mini.deserialize("<gray>lock: <white><none>")
        } else {
            val lockRaw = lockable.lock
            val lockMaterial = lockRaw.substringBefore('[').takeIf { it.isNotBlank() } ?: "<unknown>"
            out += mini.deserialize("<gray>lock: <white>material=$lockMaterial, amount=1")
        }
        val equippable = EditEquippableSupport.existingView(item)
        if (equippable == null) {
            out += mini.deserialize("<gray>equippable: <white><none>")
        } else {
            out += mini.deserialize(
                "<gray>equippable: <white>slot=${equippable.slot}, dispensable=${equippable.dispensable}, swappable=${equippable.swappable}, damageOnHurt=${equippable.damageOnHurt}"
            )
            out += mini.deserialize("<gray>equippable sound: <white>${equippable.equipSound.key.asString()}")
            out += mini.deserialize("<gray>equippable camera_overlay: <white>${equippable.cameraOverlay?.asString() ?: "<none>"}")
            out += mini.deserialize("<gray>equippable asset_id: <white>${equippable.assetId?.asString() ?: "<none>"}")
            out += mini.deserialize("<gray>equippable allowed_entities: <white>${equippable.allowedEntitiesCount}")
        }
        val tool = item.getData(DataComponentTypes.TOOL)
        if (tool == null) {
            out += mini.deserialize("<gray>tool: <white><none>")
        } else {
            val speedRules = tool.rules().count { it.speed() != null }
            out += mini.deserialize(
                "<gray>tool: <white>default_mining_speed=${tool.defaultMiningSpeed()}, damage_per_block=${tool.damagePerBlock()}, rules=${tool.rules().size}, speed_rules=$speedRules"
            )
        }
        val useCooldown = item.getData(DataComponentTypes.USE_COOLDOWN)
        if (useCooldown == null) {
            out += mini.deserialize("<gray>use_cooldown: <white><none>")
        } else {
            out += mini.deserialize("<gray>use_cooldown: <white>seconds=${useCooldown.seconds()}, group=${useCooldown.cooldownGroup() ?: "<none>"}")
        }
        val container = EditContainerSupport.readContainerContents(item)
        if (container == null) {
            out += mini.deserialize("<gray>container: <white><none>")
        } else {
            val visible = container.contents.take(container.capacity)
            val filled = visible.count { it.type != org.bukkit.Material.AIR && it.amount > 0 }
            out += mini.deserialize("<gray>container: <white>capacity=${container.capacity}, filled=$filled")
            visible.forEachIndexed { index, stack ->
                if (stack.type == org.bukkit.Material.AIR || stack.amount <= 0) return@forEachIndexed
                val stackMeta = stack.itemMeta
                val suffixParts = mutableListOf<String>()
                val customName = stackMeta?.displayName()?.let(plain::serialize)?.takeIf { it.isNotBlank() }
                if (customName != null) suffixParts += "name=$customName"
                val loreSize = stackMeta?.lore()?.size ?: 0
                if (loreSize > 0) suffixParts += "lore=$loreSize"
                val enchants = stackMeta?.enchants?.size ?: 0
                if (enchants > 0) suffixParts += "enchants=$enchants"
                val suffix = if (suffixParts.isEmpty()) "" else " (${suffixParts.joinToString(", ")})"
                out += mini.deserialize("<gray>container[$index]: <white>${stack.type.key.asString()} x${stack.amount}$suffix")
            }
        }
        val trim = (meta as? org.bukkit.inventory.meta.ArmorMeta)?.trim
        if (trim == null) {
            out += mini.deserialize("<gray>trim: <white><none>")
        } else {
            out += mini.deserialize("<gray>trim: <white>pattern=${trim.pattern.key.asString()}, material=${trim.material.key.asString()}")
        }
        fun side(side: DecoratedPotSide): String = EditTrimPotSupport.sherd(item, side)?.key?.asString() ?: "<none>"
        out += mini.deserialize("<gray>pot: <white>back=${side(DecoratedPotSide.BACK)}, left=${side(DecoratedPotSide.LEFT)}, right=${side(DecoratedPotSide.RIGHT)}, front=${side(DecoratedPotSide.FRONT)}")
        out += mini.deserialize("<gray>can_place_on: <white>${runCatching { meta?.placeableKeys?.size ?: 0 }.getOrDefault(0)} entries")
        out += mini.deserialize("<gray>can_break: <white>${runCatching { meta?.destroyableKeys?.size ?: 0 }.getOrDefault(0)} entries")
        out += mini.deserialize("<gray>enchantments: <white>${meta?.enchants?.entries?.joinToString { "${it.key.key.key}:${it.value}" } ?: "<none>"}")
        out += mini.deserialize("<gray>flags: <white>${meta?.itemFlags?.joinToString { it.name } ?: "<none>"}")
        if (meta is PotionMeta) {
            out += mini.deserialize("<gray>potion color: <white>${meta.color?.asRGB() ?: "<none>"}")
            meta.customEffects.forEachIndexed { index, effect ->
                out += mini.deserialize("<gray>potion[$index]: <white>${effect.type.key.key} dur=${effect.duration} amp=${effect.amplifier}")
            }
        }

        if (meta is SkullMeta) {
            val profile = meta.playerProfile
            if (profile == null) {
                out += mini.deserialize("<gray>head: <white><none>")
            } else {
                val name = profile.name ?: "<none>"
                val uuid = profile.uniqueId?.toString() ?: "<none>"
                val textures = profile.properties.firstOrNull { it.name == "textures" }?.value
                if (textures == null) {
                    out += mini.deserialize("<gray>head: <white>name=$name, uuid=$uuid, textures=no")
                } else {
                    out += mini.deserialize("<gray>head: <white>name=$name, uuid=$uuid, textures=yes, texture_base64_length=${textures.length}")
                }
            }
        }

        if (meta?.itemFlags?.contains(ItemFlag.HIDE_ATTRIBUTES) == true && meta.itemFlags.contains(ItemFlag.HIDE_ENCHANTS)) {
            out += mini.deserialize("<yellow>warning: tooltip heavily hidden; /edit tooltip ... show для раскрытия")
        }

        return out
    }
}
