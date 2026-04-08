package com.ratger.acreative.itemedit.head

import com.ratger.acreative.commands.edit.EditTargetResolver
import com.ratger.acreative.itemedit.api.ItemResult
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID

class HeadProfileService(
    private val plugin: JavaPlugin,
    private val targetResolver: EditTargetResolver,
    private val lookupService: LicensedProfileLookupService,
    private val mutationSupport: HeadTextureMutationSupport
) {
    private val mini = MiniMessage.miniMessage()

    fun lookupLicensedProfileAsync(name: String) = lookupService.lookupLicensedProfileAsync(name)

    fun applyFromNameAsync(playerId: UUID, name: String): ItemResult {
        lookupService.lookupLicensedProfileAsync(name).whenComplete { payload, error ->
            Bukkit.getScheduler().runTask(plugin, Runnable {
                val target = Bukkit.getPlayer(playerId) ?: return@Runnable
                if (error != null) {
                    target.sendMessage(mini.deserialize("<red>Ошибка licensed-profile lookup для <white>$name</white>: <gray>${error.message ?: "unknown error"}"))
                    return@Runnable
                }
                if (payload == null) {
                    target.sendMessage(mini.deserialize("<red>Не удалось получить профиль по имени <white>$name</white>."))
                    return@Runnable
                }

                val context = targetResolver.resolve(target) ?: return@Runnable
                if (!context.snapshot.isHead) {
                    target.sendMessage(mini.deserialize("<red>Держите minecraft:player_head в основной руке перед применением результата."))
                    return@Runnable
                }
                val item = context.item.clone()

                when (val applyResult = mutationSupport.applyFromLicensedPayload(item, payload)) {
                    is HeadTextureMutationSupport.MutationResult.Failure -> {
                        target.sendMessage(mini.deserialize("<red>${applyResult.reason}"))
                    }

                    HeadTextureMutationSupport.MutationResult.Success -> {
                        targetResolver.save(target, item)
                        target.sendMessage(mini.deserialize("<green>Текстура головы установлена из официального licensed profile <white>${payload.canonicalName}</white>."))
                    }
                }
            })
        }
        return ItemResult(true, listOf(mini.deserialize("<yellow>Запрошен профиль <white>$name</white>. Применю текстуру после асинхронного обновления.")))
    }
}
