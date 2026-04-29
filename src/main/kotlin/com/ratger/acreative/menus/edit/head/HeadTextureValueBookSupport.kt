package com.ratger.acreative.menus.edit.head

import com.ratger.acreative.utils.SeriesCodeGenerator
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.WritableBookMeta
import kotlin.math.min
import java.nio.charset.StandardCharsets
import java.util.Base64

class HeadTextureValueBookSupport {
    private val mini = MiniMessage.miniMessage()
    private val whitespaceRegex = Regex("\\s+")
    private val base64CharsRegex = Regex("^[A-Za-z0-9+/=]+$")

    fun createValueBook(textureValue: String): ItemStack {
        val pages = splitPages(textureValue)
        return ItemStack(Material.WRITABLE_BOOK).apply {
            editMeta(WritableBookMeta::class.java) { meta ->
                meta.customName(mini.deserialize("<!i><#C7A300>✎ <#FFD700>Value текстуры головы"))
                meta.lore(
                    listOf(
                        "<!i><#FFE68A>Используется для <#FFF3E0>/edit",
                        "",
                        "<!i><dark_green>▍ <#00FF40>Серия: <#7BFF00>${SeriesCodeGenerator.generate()}"
                    ).map(mini::deserialize)
                )
                meta.pages = pages
            }
        }
    }

    fun extractTextureValue(bookItem: ItemStack?): String? {
        if (!isBookItem(bookItem)) return null
        val meta = bookItem?.itemMeta as? WritableBookMeta ?: return null
        val joined = meta.pages.joinToString(separator = "")
        val normalized = normalizeWhitespace(joined)
        if (!isValidTextureValue(normalized)) return null
        return normalized
    }

    fun isValidTextureValue(candidate: String?): Boolean {
        val normalized = normalizeWhitespace(candidate)
        if (normalized.isBlank()) return false
        if (!base64CharsRegex.matches(normalized)) return false

        val decoded = runCatching {
            Base64.getDecoder().decode(normalized)
        }.getOrNull() ?: return false

        val payload = decoded.toString(StandardCharsets.UTF_8)
        val lowered = payload.lowercase()

        val hasTexturesJson = lowered.contains("\"textures\"")
        val hasTextureHost = lowered.contains("textures.minecraft.net")
        val hasSkinNode = lowered.contains("\"skin\"")

        return hasTexturesJson && (hasTextureHost || hasSkinNode)
    }

    fun isBookItem(item: ItemStack?): Boolean {
        if (item == null) return false
        return item.type == Material.WRITABLE_BOOK || item.type == Material.WRITTEN_BOOK
    }

    private fun normalizeWhitespace(value: String?): String {
        if (value == null) return ""
        return value.replace(whitespaceRegex, "")
    }

    private fun splitPages(value: String): List<String> {
        if (value.isEmpty()) return listOf("")

        val pageSize = 240
        val pages = ArrayList<String>()
        var start = 0
        while (start < value.length) {
            val end = min(start + pageSize, value.length)
            pages += value.substring(start, end)
            start = end
        }
        return pages
    }

}
