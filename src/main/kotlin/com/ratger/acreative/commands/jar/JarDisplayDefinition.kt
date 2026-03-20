package com.ratger.acreative.commands.jar

internal data class JarDisplayPart(
    val textureValue: String,
    val matrix: FloatArray
)

internal object JarDisplayDefinition {
    const val TEXTURE_A = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGYxODgyZTUwNjQ0M2ZkOGM0M2NmZWY3YjQxMjRjOTg5N2M5ZWJiYjVjNjYxZjdmYzc2YWMxN2JlNTM5MzBkZWUifX19"
    const val TEXTURE_B = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGI2YzBmYzg1NGNiYjYzZjA1NDM4NDQ5YmRmMjZkNzRkYmNlNmQyYzFjM2VjMDY3NGRmZmI4MDJiZmU0ZGQ5MCJ9fX0="
    const val TEXTURE_C = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTYzNzU2Mzg4ZGU1N2ExOTNlZTljNTY1NDc0MTdlZTIxYjRjODY4YmNkYzU2ZDUwM2UwMjkyYzI4OWI0MTU4ZCJ9fX0="
    const val TEXTURE_D = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzI4ZjA3YzQ4NWNmZWYyYmE3YWI2ZGJiYjdlNDgyNWZhZDk3ZmVjZWJhZjBlOWRmZDJmMmZhZTI1NzQ0Y2FhYWUifX19"
    const val TEXTURE_E = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWQ1MzdhZGY2OWE5NDRiMWQ3M2U2ZjlmMTUxOTRmNThjMDEyNGYxYThjNDljMTgzYWRmYjYzYTcyNTk1NDhkNSJ9fX0="
    const val TEXTURE_F = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzFlM2UwZGMwNmQ3ZjNhZThjYjY1NmIxMjFkYmQ4M2YyZjM1YmI5ODcyMWFhNzY1MjM1ZmE0Y2YzNmZiMDVhYiJ9fX0="
    const val TEXTURE_G = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTRmMzM4OWFjNmJhZmFhZDcyMDFjNTQxNTgxODExZGQyYzU1YWIyYjcwYmUyZTMzZGYyMThiMjM1Y2ZjNGQ0YSJ9fX0="
    const val TEXTURE_H = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGJkMDU3N2ZiODQ3MjZlMGI5YzhkYmFkZjcxMmRjM2IzZWIxMDViOTY5YjZkMDM1MjgwMWY4NWRjNDA5Yzc2NCJ9fX0="
    const val TEXTURE_I = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODY5MTBiZTNkYmU2ODg4YWYxMTk4Yzk0MjU5MjY4YjNiYzAzMmY5MzdlYjQwMjQ2NWZlMGY2MDAzNjBhMGFiYyJ9fX0="
    const val TEXTURE_J = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjI2OTA2NTI2ZWZjZWI4MDA4ZWQzNDNhNzRmMDI1YjQ0MzEzYTI0Y2EyYmFiYmFmOWIwZmExNzg5NTYxYTBjOSJ9fX0="
    const val TEXTURE_K = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTFiMzZlZmQ0NzMzNDFhNWUxNWU3MWY4MjRiMDNmOTM4MTI1YmRiMmI4OTQ3N2VkYjAyZmRlNmY2ZDdhZTZjNSJ9fX0="
    const val TEXTURE_L = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjkxODczYzY2ODE2ZjJlZWFmNjNjMjcyMGViMzEzMTA1N2MxZTRkYTVlNGQ0NTRiMGY4MTljMzczMGFlZjk0MiJ9fX0="

    val parts: List<JarDisplayPart> = listOf(
        JarDisplayPart(TEXTURE_A, floatArrayOf(1.1765f, 0f, 0f, -1.06e-8f, 0f, 0f, -18824f, -5000.125f, 0f, 1.1765f, 0f, 0.29375f, 0f, 0f, 0f, 1f)),
        JarDisplayPart(TEXTURE_A, floatArrayOf(1.1765f, 0f, 0f, -1.06e-8f, 0f, 0f, -18824f, -4999.125f, 0f, 1.1765f, 0f, 0.29375f, 0f, 0f, 0f, 1f)),
        JarDisplayPart(TEXTURE_B, floatArrayOf(-0.9412f, 0f, 0f, 0.2500000085f, 0f, 0.9412f, 0f, 0.48530625f, 0f, 0f, -18824f, -5000.4375f, 0f, 0f, 0f, 1f)),
        JarDisplayPart(TEXTURE_C, floatArrayOf(-0.9412f, 0f, 0f, -0.2499999915f, 0f, 0.9412f, 0f, 0.48530625f, 0f, 0f, -18824f, -5000.4375f, 0f, 0f, 0f, 1f)),
        JarDisplayPart(TEXTURE_D, floatArrayOf(-0.9412f, 0f, 0f, -0.2499999915f, 0f, 0.9412f, 0f, 0.98530625f, 0f, 0f, -18824f, -5000.4375f, 0f, 0f, 0f, 1f)),
        JarDisplayPart(TEXTURE_E, floatArrayOf(-0.9412f, 0f, 0f, 0.2500000085f, 0f, 0.9412f, 0f, 0.98530625f, 0f, 0f, -18824f, -5000.4375f, 0f, 0f, 0f, 1f)),
        JarDisplayPart(TEXTURE_F, floatArrayOf(-0.9412f, 0f, 0f, 0.2500000085f, 0f, 0.9412f, 0f, 0.48530625f, 0f, 0f, -18824f, -4999.8125f, 0f, 0f, 0f, 1f)),
        JarDisplayPart(TEXTURE_G, floatArrayOf(-0.9412f, 0f, 0f, -0.2499999915f, 0f, 0.9412f, 0f, 0.48530625f, 0f, 0f, -18824f, -4999.8125f, 0f, 0f, 0f, 1f)),
        JarDisplayPart(TEXTURE_H, floatArrayOf(-0.9412f, 0f, 0f, -0.2499999915f, 0f, 0.9412f, 0f, 0.98530625f, 0f, 0f, -18824f, -4999.8125f, 0f, 0f, 0f, 1f)),
        JarDisplayPart(TEXTURE_I, floatArrayOf(-0.9412f, 0f, 0f, 0.2500000085f, 0f, 0.9412f, 0f, 0.98530625f, 0f, 0f, -18824f, -4999.8125f, 0f, 0f, 0f, 1f)),
        JarDisplayPart(TEXTURE_B, floatArrayOf(0f, 0f, 18824f, 5000.4375f, 0f, 0.9412f, 0f, 0.48530625f, -0.9412f, 0f, 0f, 0.2500000085f, 0f, 0f, 0f, 1f)),
        JarDisplayPart(TEXTURE_C, floatArrayOf(0f, 0f, 18824f, 5000.4375f, 0f, 0.9412f, 0f, 0.48530625f, -0.9412f, 0f, 0f, -0.2499999915f, 0f, 0f, 0f, 1f)),
        JarDisplayPart(TEXTURE_D, floatArrayOf(0f, 0f, 18824f, 5000.4375f, 0f, 0.9412f, 0f, 0.98530625f, -0.9412f, 0f, 0f, -0.2499999915f, 0f, 0f, 0f, 1f)),
        JarDisplayPart(TEXTURE_E, floatArrayOf(0f, 0f, 18824f, 5000.4375f, 0f, 0.9412f, 0f, 0.98530625f, -0.9412f, 0f, 0f, 0.2500000085f, 0f, 0f, 0f, 1f)),
        JarDisplayPart(TEXTURE_F, floatArrayOf(0f, 0f, 18824f, 4999.8125f, 0f, 0.9412f, 0f, 0.48530625f, -0.9412f, 0f, 0f, 0.2500000085f, 0f, 0f, 0f, 1f)),
        JarDisplayPart(TEXTURE_G, floatArrayOf(0f, 0f, 18824f, 4999.8125f, 0f, 0.9412f, 0f, 0.48530625f, -0.9412f, 0f, 0f, -0.2499999915f, 0f, 0f, 0f, 1f)),
        JarDisplayPart(TEXTURE_H, floatArrayOf(0f, 0f, 18824f, 4999.8125f, 0f, 0.9412f, 0f, 0.98530625f, -0.9412f, 0f, 0f, -0.2499999915f, 0f, 0f, 0f, 1f)),
        JarDisplayPart(TEXTURE_I, floatArrayOf(0f, 0f, 18824f, 4999.8125f, 0f, 0.9412f, 0f, 0.98530625f, -0.9412f, 0f, 0f, 0.2500000085f, 0f, 0f, 0f, 1f)),
        JarDisplayPart(TEXTURE_J, floatArrayOf(0.75f, 0f, 0f, 0f, 0f, 0.25f, 0f, 1.125f, 0f, 0f, 0.75f, 0f, 0f, 0f, 0f, 1f)),
        JarDisplayPart(TEXTURE_K, floatArrayOf(1f, 0f, 0f, 0f, 0f, 0.25f, 0f, 1.25f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f)),
        JarDisplayPart(TEXTURE_L, floatArrayOf(0.75f, 0f, 0f, 0f, 0f, 0.125f, 0f, 1.3125f, 0f, 0f, 0.75f, 0f, 0f, 0f, 0f, 1f))
    )
}
