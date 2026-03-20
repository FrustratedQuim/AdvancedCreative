package com.ratger.acreative.commands.jar

internal data class JarDisplayPart(
    val textureUrl: String,
    val matrix: FloatArray
)

internal object JarDisplayDefinition {
    const val TEXTURE_A = "http://textures.minecraft.net/texture/4f1882e506443fd8c43cfef7b4124c9897c9ebbb5c661f7fc76ac17be53930dee"
    const val TEXTURE_B = "http://textures.minecraft.net/texture/4b6c0fc854cbb63f05438449bdf26d74dbce6d2c1c3ec0674dffb802bfe4dd90"
    const val TEXTURE_C = "http://textures.minecraft.net/texture/a63756388de57a193ee9c56547417ee21b4c868bcdc56d503e0292c289b4158d"
    const val TEXTURE_D = "http://textures.minecraft.net/texture/c28f07c485cfef2ba7ab6dbbb7e4825fad97fecebaf0e9dfd2f2fae25744caaae"
    const val TEXTURE_E = "http://textures.minecraft.net/texture/1d537adf69a944b1d73e6f9f15194f58c0124f1a8c49c183adfb63a7259548d5"
    const val TEXTURE_F = "http://textures.minecraft.net/texture/71e3e0dc06d7f3ae8cb656b121dbd83f2f35bb98721aa765235fa4cf36fb05ab"
    const val TEXTURE_G = "http://textures.minecraft.net/texture/a4f3389ac6bafaad7201c541581811dd2c55ab2b70be2e33df218b235cfc4d4a"
    const val TEXTURE_H = "http://textures.minecraft.net/texture/4bd0577fb84726e0b9c8dbadf712dc3b3eb105b969b6d0352801f85dc409c764"
    const val TEXTURE_I = "http://textures.minecraft.net/texture/86910be3dbe6888af1198c94259268b3bc032f937eb402465fe0f600360a0abc"
    const val TEXTURE_J = "http://textures.minecraft.net/texture/b26906526efceb8008ed343a74f025b44313a24ca2babbaf9b0fa1789561a0c9"
    const val TEXTURE_K = "http://textures.minecraft.net/texture/91b36efd473341a5e15e71f824b03f938125bdb2b89477edb02fde6f6d7ae6c5"
    const val TEXTURE_L = "http://textures.minecraft.net/texture/291873c66816f2eeaf63c2720eb3131057c1e4da5e4d454b0f819c3730aef942"

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
