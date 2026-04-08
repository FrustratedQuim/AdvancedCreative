package com.ratger.acreative.itemedit.head

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.CompletableFuture

class LicensedProfileLookupService {
    data class LicensedProfilePayload(
        val uuid: UUID,
        val canonicalName: String,
        val textureValue: String,
        val textureSignature: String?
    )

    private val httpClient: HttpClient = HttpClient.newBuilder().build()

    fun lookupLicensedProfileAsync(name: String): CompletableFuture<LicensedProfilePayload> {
        return CompletableFuture.supplyAsync { lookupLicensedProfile(name) }
    }

    fun lookupLicensedProfile(name: String): LicensedProfilePayload {
        val encoded = URLEncoder.encode(name, StandardCharsets.UTF_8)
        val nameLookup = lookupNameViaMinecraftServices(encoded) ?: lookupNameViaMojang(encoded)
            ?: throw IllegalStateException("Профиль по имени не найден.")
        val session = lookupSessionProfile(nameLookup.uuid)
        val textures = session.texturesValue ?: throw IllegalStateException("Session profile не содержит textures.")
        return LicensedProfilePayload(
            uuid = session.uuid,
            canonicalName = session.name ?: nameLookup.canonicalName,
            textureValue = textures,
            textureSignature = session.texturesSignature
        )
    }

    private fun lookupNameViaMinecraftServices(encodedName: String): NameLookupPayload? {
        val response = sendJsonGet("https://api.minecraftservices.com/minecraft/profile/lookup/name/$encodedName")
        if (response.statusCode() == 404) return null
        if (response.statusCode() !in 200..299) {
            throw IllegalStateException("minecraftservices lookup HTTP ${response.statusCode()}")
        }
        return parseNameLookupPayload(response.body())
    }

    private fun lookupNameViaMojang(encodedName: String): NameLookupPayload? {
        val response = sendJsonGet("https://api.mojang.com/users/profiles/minecraft/$encodedName")
        if (response.statusCode() == 204 || response.statusCode() == 404) return null
        if (response.statusCode() !in 200..299) {
            throw IllegalStateException("mojang lookup HTTP ${response.statusCode()}")
        }
        return parseNameLookupPayload(response.body())
    }

    private fun lookupSessionProfile(uuid: UUID): SessionProfilePayload {
        val undashed = uuid.toString().replace("-", "")
        val response = sendJsonGet("https://sessionserver.mojang.com/session/minecraft/profile/$undashed")
        if (response.statusCode() == 404) throw IllegalStateException("Session profile не найден.")
        if (response.statusCode() !in 200..299) throw IllegalStateException("sessionserver HTTP ${response.statusCode()}")
        return parseSessionProfilePayload(response.body())
    }

    private fun sendJsonGet(url: String): HttpResponse<String> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept", "application/json")
            .GET()
            .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
    }

    private fun parseNameLookupPayload(json: String): NameLookupPayload? {
        val idRaw = jsonString(json, "id") ?: return null
        val canonicalName = jsonString(json, "name") ?: return null
        val uuid = parseUuid(idRaw) ?: return null
        return NameLookupPayload(uuid, canonicalName)
    }

    private fun parseSessionProfilePayload(json: String): SessionProfilePayload {
        val idRaw = jsonString(json, "id") ?: throw IllegalStateException("Session profile id отсутствует.")
        val uuid = parseUuid(idRaw) ?: throw IllegalStateException("Session profile id невалидный.")
        val canonicalName = jsonString(json, "name")
        val texturesObject = Regex("\\{[^{}]*\"name\"\\s*:\\s*\"textures\"[^{}]*}", setOf(RegexOption.DOT_MATCHES_ALL))
            .find(json)
            ?.value
        val textureValue = texturesObject?.let { jsonString(it, "value") }
        val textureSignature = texturesObject?.let { jsonString(it, "signature") }
        return SessionProfilePayload(uuid, canonicalName, textureValue, textureSignature)
    }

    private fun parseUuid(raw: String): UUID? {
        val normalized = raw.trim()
        return runCatching {
            if (normalized.contains('-')) UUID.fromString(normalized)
            else UUID.fromString(normalized.replaceFirst(
                Regex("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)$"),
                "$1-$2-$3-$4-$5"
            ))
        }.getOrNull()
    }

    private fun jsonString(source: String, field: String): String? {
        val escapedField = Regex.escape(field)
        val regex = Regex("\"$escapedField\"\\s*:\\s*\"([^\"]*)\"")
        return regex.find(source)?.groupValues?.getOrNull(1)
    }

    private data class NameLookupPayload(
        val uuid: UUID,
        val canonicalName: String
    )

    private data class SessionProfilePayload(
        val uuid: UUID,
        val name: String?,
        val texturesValue: String?,
        val texturesSignature: String?
    )
}
