package com.ratger.acreative.commands.admin.npc

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.logging.Logger

class NpcJsonStorage(
    private val folder: File,
    private val equipmentCodec: NpcEquipmentCodec,
    private val logger: Logger
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun loadAll(): List<NpcProfile> {
        if (!folder.exists()) {
            folder.mkdirs()
            return emptyList()
        }

        return folder.listFiles { file -> file.isFile && file.extension.equals("json", ignoreCase = true) }
            .orEmpty()
            .sortedBy { it.nameWithoutExtension.lowercase() }
            .mapNotNull(::readProfileSafely)
    }

    fun saveAll(profiles: Collection<NpcProfile>) {
        if (!folder.exists()) {
            folder.mkdirs()
        }
        val expectedFileNames = profiles
            .map { "${it.name}.json".lowercase() }
            .toSet()

        profiles.sortedBy { it.name.lowercase() }.forEach { profile ->
            writeProfileAtomically(profile)
        }

        folder.listFiles { file -> file.isFile && file.extension.equals("json", ignoreCase = true) }
            .orEmpty()
            .filter { it.name.lowercase() !in expectedFileNames }
            .forEach { staleFile ->
                if (!staleFile.delete()) {
                    logger.warning("Failed to delete stale npc profile ${staleFile.name}")
                }
            }
    }

    private fun readProfileSafely(file: File): NpcProfile? {
        return runCatching { readProfile(file) }
            .onFailure { logger.warning("Failed to load npc profile ${file.name}: ${it.message}") }
            .getOrNull()
    }

    private fun readProfile(file: File): NpcProfile? {
        val root = json.parseToJsonElement(file.readText(Charsets.UTF_8)).jsonObject
        val name = root.string("name") ?: return null
        val locationObject = root["location"]?.jsonObject ?: return null
        val location = NpcLocation(
            worldName = locationObject.string("world") ?: return null,
            x = locationObject.double("x") ?: return null,
            y = locationObject.double("y") ?: return null,
            z = locationObject.double("z") ?: return null,
            yaw = locationObject.float("yaw") ?: 0f,
            pitch = locationObject.float("pitch") ?: 0f
        )
        val visualNick = root.string("visualNick") ?: name
        val skin = root["skin"]?.jsonObject?.let { skinObject ->
            val textureValue = skinObject.string("value") ?: return@let null
            NpcSkin(
                textureValue = textureValue,
                textureSignature = skinObject.string("signature")
            )
        }
        val equipmentObject = root["equipment"]?.jsonObject
        val equipment = NpcEquipment(
            helmet = equipmentCodec.deserialize(equipmentObject?.string("helmet")),
            chestplate = equipmentCodec.deserialize(equipmentObject?.string("chestplate")),
            leggings = equipmentCodec.deserialize(equipmentObject?.string("leggings")),
            boots = equipmentCodec.deserialize(equipmentObject?.string("boots")),
            mainHand = equipmentCodec.deserialize(equipmentObject?.string("mainHand")),
            offHand = equipmentCodec.deserialize(equipmentObject?.string("offHand"))
        )
        return NpcProfile(
            name = name,
            location = location,
            visualNick = visualNick,
            skin = skin,
            equipment = equipment
        )
    }

    private fun toJson(profile: NpcProfile): JsonObject = buildJsonObject {
        put("schemaVersion", JsonPrimitive(SCHEMA_VERSION))
        put("name", JsonPrimitive(profile.name))
        put("location", buildJsonObject {
            put("world", JsonPrimitive(profile.location.worldName))
            put("x", JsonPrimitive(profile.location.x))
            put("y", JsonPrimitive(profile.location.y))
            put("z", JsonPrimitive(profile.location.z))
            put("yaw", JsonPrimitive(profile.location.yaw))
            put("pitch", JsonPrimitive(profile.location.pitch))
        })
        put("visualNick", JsonPrimitive(profile.visualNick))
        profile.skin?.let { skin ->
            put("skin", buildJsonObject {
                put("value", JsonPrimitive(skin.textureValue))
                skin.textureSignature?.let { put("signature", JsonPrimitive(it)) }
            })
        }
        put("equipment", buildJsonObject {
            equipmentCodec.serialize(profile.equipment.helmet)?.let { put("helmet", JsonPrimitive(it)) }
            equipmentCodec.serialize(profile.equipment.chestplate)?.let { put("chestplate", JsonPrimitive(it)) }
            equipmentCodec.serialize(profile.equipment.leggings)?.let { put("leggings", JsonPrimitive(it)) }
            equipmentCodec.serialize(profile.equipment.boots)?.let { put("boots", JsonPrimitive(it)) }
            equipmentCodec.serialize(profile.equipment.mainHand)?.let { put("mainHand", JsonPrimitive(it)) }
            equipmentCodec.serialize(profile.equipment.offHand)?.let { put("offHand", JsonPrimitive(it)) }
        })
    }

    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.double(key: String): Double? = string(key)?.toDoubleOrNull()

    private fun JsonObject.float(key: String): Float? = string(key)?.toFloatOrNull()

    private fun writeProfileAtomically(profile: NpcProfile) {
        val targetPath = File(folder, "${profile.name}.json").toPath()
        val payload = json.encodeToString(JsonObject.serializer(), toJson(profile))
        val tempPath = Files.createTempFile(folder.toPath(), "${profile.name}.", ".json.tmp")

        try {
            Files.writeString(tempPath, payload, Charsets.UTF_8)
            try {
                Files.move(
                    tempPath,
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                )
            } catch (_: Exception) {
                Files.move(
                    tempPath,
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        } finally {
            runCatching { Files.deleteIfExists(tempPath) }
        }
    }

    private companion object {
        const val SCHEMA_VERSION = 1
    }
}
