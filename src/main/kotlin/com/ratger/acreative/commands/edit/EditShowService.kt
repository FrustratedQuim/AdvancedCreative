package com.ratger.acreative.commands.edit

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
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
        out += mini.deserialize("<gray>can_place_on: <white>${runCatching { meta?.placeableKeys?.size ?: 0 }.getOrDefault(0)} entries")
        out += mini.deserialize("<gray>can_break: <white>${runCatching { meta?.destroyableKeys?.size ?: 0 }.getOrDefault(0)} entries")
        out += mini.deserialize("<gray>enchantments: <white>${meta?.enchants?.entries?.joinToString { "${it.key.key.key}:${it.value}" } ?: "<none>"}")
        out += mini.deserialize("<gray>flags: <white>${meta?.itemFlags?.joinToString { it.name } ?: "<none>"}")
        out += mini.deserialize("<gray>plugin-state: <white>${context.snapshot.hasPluginState}")

        if (meta is PotionMeta) {
            out += mini.deserialize("<gray>potion color: <white>${meta.color?.asRGB() ?: "<none>"}")
            meta.customEffects.forEachIndexed { index, effect ->
                out += mini.deserialize("<gray>potion[$index]: <white>${effect.type.key.key} dur=${effect.duration} amp=${effect.amplifier}")
            }
        }

        if (meta is SkullMeta) {
            val textures = meta.playerProfile?.properties?.firstOrNull { it.name == "textures" }?.value
            val preview = textures?.take(24)?.plus("...")
            out += mini.deserialize("<gray>head texture: <white>${if (textures == null) "<none>" else "len=${textures.length}, $preview"}")
        }

        if (meta?.itemFlags?.contains(ItemFlag.HIDE_ATTRIBUTES) == true && meta.itemFlags.contains(ItemFlag.HIDE_ENCHANTS)) {
            out += mini.deserialize("<yellow>warning: tooltip heavily hidden; /edit tooltip ... show для раскрытия")
        }

        return out
    }
}
