package world.phantasmal.web.questEditor.rendering

import kotlinx.browser.document
import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.psolib.asm.dataFlowAnalysis.ParticleSpawn
import world.phantasmal.psolib.asm.dataFlowAnalysis.ParticleSpawnOpcode
import world.phantasmal.psolib.asm.dataFlowAnalysis.ParticleSpawnOrigin
import world.phantasmal.psolib.asm.dataFlowAnalysis.ParticleSpawnSource
import world.phantasmal.psolib.fileFormats.particle.ParticleEffectData
import world.phantasmal.web.externals.three.Camera
import world.phantasmal.web.externals.three.Texture
import world.phantasmal.web.externals.three.Vector3
import world.phantasmal.web.questEditor.loading.EffectNtMetadata
import world.phantasmal.web.questEditor.loading.ParticleAssets
import world.phantasmal.web.questEditor.loading.ParticleTexture
import world.phantasmal.web.test.WebTestSuite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ParticleMarkerManagerTests : WebTestSuite {
    @Test
    fun replacing_floor_spawns_clears_emitters_live_particles_and_scene_nodes() = testAsync {
        var nowMs = 0.0
        val texture = Texture()
        val assets = particleAssets(texture)
        val renderContext = QuestRenderContext(
            canvas = document.createElement("canvas") as HTMLCanvasElement,
            camera = Camera(),
        )
        disposer.add(renderContext)
        val manager = ParticleMarkerManager(
            renderContext = renderContext,
            loadParticleAssets = { assets },
            nowMs = { nowMs },
        )
        disposer.add(manager)

        manager.setSpawns(
            spawns = listOf(spawn(floorId = 1)),
            resolveTemplateMapIds = { setOf(0) },
            resolveEntityPosition = { null },
        )
        assertEquals(1, manager.emitterCount)

        nowMs = 34.0
        manager.beforeRender()
        assertTrue(manager.liveParticleCount > 0)
        assertTrue(renderContext.particleMarkers.children.isNotEmpty())

        manager.setSpawns(
            spawns = emptyList(),
            resolveTemplateMapIds = { emptySet() },
            resolveEntityPosition = { null },
        )

        assertEquals(0, manager.emitterCount)
        assertEquals(0, manager.liveParticleCount)
        assertTrue(renderContext.particleMarkers.children.isEmpty())
        texture.dispose()
    }

    @Test
    fun dat_emitter_follows_its_object_position_each_frame() = testAsync {
        var nowMs = 0.0
        var objectPosition = Vector3(10.0, 20.0, 30.0)
        val texture = Texture()
        val renderContext = QuestRenderContext(
            canvas = document.createElement("canvas") as HTMLCanvasElement,
            camera = Camera(),
        )
        disposer.add(renderContext)
        val manager = ParticleMarkerManager(
            renderContext = renderContext,
            loadParticleAssets = { particleAssets(texture) },
            nowMs = { nowMs },
        )
        disposer.add(manager)
        val spawn = ParticleSpawn(
            origin = ParticleSpawnOrigin.EntityPosition(0x4001, 0),
            particleId = 0,
            lifetimeFrames = null,
            source = ParticleSpawnSource.DatObject(0x0001, 1),
            hasExtendedDrawRange = false,
            executionFloorIds = setOf(1),
        )

        manager.setSpawns(
            spawns = listOf(spawn),
            resolveTemplateMapIds = { setOf(0) },
            resolveEntityPosition = { objectPosition },
        )
        objectPosition = Vector3(100.0, 200.0, 300.0)
        nowMs = 34.0
        manager.beforeRender()

        val particle = renderContext.particleMarkers.children.single()
        assertEquals(100.0, particle.position.x)
        assertEquals(200.0, particle.position.y)
        assertEquals(300.0, particle.position.z)
        texture.dispose()
    }

    private fun spawn(floorId: Int): ParticleSpawn = ParticleSpawn(
        origin = ParticleSpawnOrigin.WorldPosition(100, 200, 300),
        particleId = 0,
        lifetimeFrames = 60,
        source = ParticleSpawnSource.Opcode(ParticleSpawnOpcode.ParticleV3),
        hasExtendedDrawRange = false,
        executionFloorIds = setOf(floorId),
    )

    private fun particleAssets(texture: Texture): ParticleAssets = ParticleAssets(
        globalEffects = listOf(particleEffect()),
        mapEffects = emptyList(),
        texturesById = mapOf(
            0 to ParticleTexture(
                texture = texture,
                metadata = EffectNtMetadata(
                    flags = 0,
                    textureIndex = 0,
                    width = 16f,
                    height = 16f,
                    rendererType = 0,
                ),
            ),
        ),
    )

    private fun particleEffect(): ParticleEffectData = ParticleEffectData(
        name = "test",
        particleType = 0,
        textureId = 0,
        xVariation = 0f,
        yVariation = 0f,
        zVariation = 0f,
        initialScale = 1f,
        randomScaleRange = 0f,
        scaleMultiplier = 1f,
        horizontalSpeed = 0f,
        verticalSpeed = 0f,
        randomHorizontalSpeedRange = 0f,
        randomVerticalSpeedRange = 0f,
        emissionModulationDegreesPerFrame = 0f,
        emissionRate = 1f,
        lifetimeFrames = 30,
        randomLifetimeFrames = 0,
        motionMultiplier = 1f,
        verticalVelocityDelta = 0f,
        fadeInFraction = 0f,
        fadeOutFraction = 0f,
        radius = 0f,
        motionOption1 = 0f,
        motionOption2 = 0f,
        redDelta = 0f,
        greenDelta = 0f,
        blueDelta = 0f,
        alpha = 1f,
        red = 1f,
        green = 1f,
        blue = 1f,
        textureFlags = 0,
        normal = 0,
        jump = 0,
        reverse = 0,
    )
}
