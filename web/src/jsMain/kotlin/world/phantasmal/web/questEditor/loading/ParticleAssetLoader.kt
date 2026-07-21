package world.phantasmal.web.questEditor.loading

import kotlinx.serialization.Serializable
import world.phantasmal.core.Success
import world.phantasmal.psolib.Endianness
import world.phantasmal.psolib.cursor.cursor
import world.phantasmal.psolib.fileFormats.ninja.parseXvm
import world.phantasmal.psolib.fileFormats.particle.GLOBAL_PARTICLE_EFFECT_COUNT
import world.phantasmal.psolib.fileFormats.particle.MAP_PARTICLE_EFFECT_COUNT
import world.phantasmal.psolib.fileFormats.particle.ParticleEffectData
import world.phantasmal.psolib.fileFormats.particle.parseParticleEffectDataList
import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.web.core.rendering.conversion.xvrTextureToThree
import world.phantasmal.web.externals.three.Texture
import world.phantasmal.webui.DisposableContainer

data class ParticleAssets(
    val globalEffects: List<ParticleEffectData>,
    val mapEffects: List<ParticleEffectData>,
    val texturesById: Map<Int, ParticleTexture>,
) {
    fun effect(id: Int): ParticleEffectData? = when (id) {
        in 0 until GLOBAL_PARTICLE_EFFECT_COUNT -> globalEffects.getOrNull(id)
        in GLOBAL_PARTICLE_EFFECT_COUNT until GLOBAL_PARTICLE_EFFECT_COUNT + MAP_PARTICLE_EFFECT_COUNT ->
            mapEffects.getOrNull(id - GLOBAL_PARTICLE_EFFECT_COUNT)
        else -> null
    }
}

@Serializable
data class EffectNtMetadata(
    val flags: Int,
    /** Zero-based texture slot inside effect_nt.xvm; this is not the XVR resource ID. */
    val textureIndex: Int,
    val width: Float,
    val height: Float,
    val rendererType: Int,
)

data class ParticleTexture(val texture: Texture, val metadata: EffectNtMetadata)

/** Loads the same global and current-map particle tables selected by the PSOBB client. */
class ParticleAssetLoader(private val assetLoader: AssetLoader) : DisposableContainer() {
    private var globalEffects: List<ParticleEffectData>? = null
    private var texturesById: Map<Int, ParticleTexture>? = null
    private val mapEffects = mutableMapOf<Int, List<ParticleEffectData>>()

    suspend fun load(mapId: Int): ParticleAssets {
        val global = globalEffects ?: parseParticleEffectDataList(
            assetLoader.loadArrayBuffer("/particles/particleentry.dat")
                .cursor(Endianness.Little),
            GLOBAL_PARTICLE_EFFECT_COUNT,
        ).also { globalEffects = it }

        val textures = texturesById ?: run {
            val metadata = assetLoader.load<List<EffectNtMetadata>>(
                "/particles/effect_nt_metadata.json",
            )
            val parsed = parseXvm(
                assetLoader.loadArrayBuffer("/particles/effect_nt.xvm")
                    .cursor(Endianness.Little),
            )
            require(parsed is Success)
            require(parsed.value.textures.size == metadata.size)
            require(metadata.map { it.textureIndex } == metadata.indices.toList())
            parsed.value.textures.mapIndexed { index, xvr ->
                // particleentry.textureId contains the XVR ID, while the REL metadata uses an
                // ordinal texture slot. Preserve both namespaces explicitly here.
                xvr.id to ParticleTexture(xvrTextureToThree(xvr), metadata[index])
            }.toMap()
                .also { texturesById = it }
        }

        val local = mapEffects[mapId] ?: run {
            parseParticleEffectDataList(
                assetLoader.loadArrayBuffer("/particles/particleentrya${mapId.toString().padStart(2, '0')}.dat")
                    .cursor(Endianness.Little),
                MAP_PARTICLE_EFFECT_COUNT,
            ).also { mapEffects[mapId] = it }
        }

        return ParticleAssets(global, local, textures)
    }

    override fun dispose() {
        texturesById?.values?.forEach { it.texture.dispose() }
        texturesById = null
        globalEffects = null
        mapEffects.clear()
        super.dispose()
    }
}
