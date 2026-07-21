package world.phantasmal.psolib.fileFormats.particle

import world.phantasmal.psolib.Endianness
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.cursor.cursor
import world.phantasmal.psolib.test.LibTestSuite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ParticleEffectDataTests : LibTestSuite {
    @Test
    fun parses_client_layout() {
        val data = Buffer.withSize(PARTICLE_EFFECT_DATA_SIZE).apply {
            endianness = Endianness.Little
            setStringAscii(0, "quest smoke", 16)
            setInt(16, 2)
            setInt(20, 1234)
            setFloat(24, 10.5f)
            setFloat(64, 3.25f)
            setFloat(68, 4f)
            setInt(72, 45)
            setInt(76, 15)
            setFloat(80, 0.9f)
            setFloat(120, 0.75f)
            setFloat(124, 1f)
            setFloat(128, 0.5f)
            setFloat(132, 0.25f)
            setInt(136, 0x12)
            setInt(148, 7)
        }

        val effect = parseParticleEffectData(data.cursor())

        assertEquals("quest smoke", effect.name)
        assertEquals(2, effect.particleType)
        assertEquals(1234, effect.textureId)
        assertEquals(10.5f, effect.xVariation)
        assertEquals(3.25f, effect.emissionModulationDegreesPerFrame)
        assertEquals(4f, effect.emissionRate)
        assertEquals(45, effect.lifetimeFrames)
        assertEquals(15, effect.randomLifetimeFrames)
        // Kotlin/JS represents Float as a JavaScript number, while DataView.getFloat32 returns
        // the exact IEEE-754 float32 value (0.899999976...). Compare non-exact decimals with a
        // tolerance so the common test has the same meaning on JVM and JS.
        assertEquals(0.9f, effect.motionMultiplier, 0.000001f)
        assertEquals(0.75f, effect.alpha)
        assertEquals(0x12, effect.textureFlags)
        assertEquals(7, effect.reverse)
    }

    @Test
    fun rejects_wrong_table_size() {
        val data = Buffer.withSize(PARTICLE_EFFECT_DATA_SIZE - 1).apply {
            endianness = Endianness.Little
        }
        assertFailsWith<IllegalArgumentException> {
            parseParticleEffectDataList(data.cursor(), 1)
        }
    }
}
