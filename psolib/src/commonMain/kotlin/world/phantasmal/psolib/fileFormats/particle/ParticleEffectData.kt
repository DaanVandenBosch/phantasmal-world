package world.phantasmal.psolib.fileFormats.particle

import world.phantasmal.psolib.cursor.Cursor

/** One 0x98-byte PSOBB `particleentry.dat` emitter template. */
data class ParticleEffectData(
    val name: String,
    val particleType: Int,
    val textureId: Int,
    val xVariation: Float,
    val yVariation: Float,
    val zVariation: Float,
    val initialScale: Float,
    val randomScaleRange: Float,
    /** Per-frame multiplier applied to both sprite dimensions. */
    val scaleMultiplier: Float,
    /** Base horizontal speed; the client field is misspelled `vacume`. */
    val horizontalSpeed: Float,
    /** Base vertical speed. */
    val verticalSpeed: Float,
    val randomHorizontalSpeedRange: Float,
    val randomVerticalSpeedRange: Float,
    /** Client field `creat`; 0 for constant emission, otherwise sine phase degrees per frame. */
    val emissionModulationDegreesPerFrame: Float,
    /** Client field `number`; emission-rate magnitude. */
    val emissionRate: Float,
    val lifetimeFrames: Int,
    val randomLifetimeFrames: Int,
    /** Per-frame motion multiplier: XZ velocity for type 0, interpolation factor for type 1. */
    val motionMultiplier: Float,
    /** Per-frame addition to vertical velocity. */
    val verticalVelocityDelta: Float,
    /** Normalized-lifetime interval over which a particle fades in. */
    val fadeInFraction: Float,
    /** Normalized-lifetime interval over which a particle fades out. */
    val fadeOutFraction: Float,
    val radius: Float,
    /** Type-dependent motion parameter (`opt1` in the client structure). */
    val motionOption1: Float,
    /** Type-dependent motion parameter (`opt2` in the client structure). */
    val motionOption2: Float,
    val redDelta: Float,
    val greenDelta: Float,
    val blueDelta: Float,
    val alpha: Float,
    val red: Float,
    val green: Float,
    val blue: Float,
    val textureFlags: Int,
    val normal: Int,
    val jump: Int,
    val reverse: Int,
)

const val PARTICLE_EFFECT_DATA_SIZE = 0x98
const val GLOBAL_PARTICLE_EFFECT_COUNT = 0x200
const val MAP_PARTICLE_EFFECT_COUNT = 0x40

fun parseParticleEffectData(cursor: Cursor): ParticleEffectData {
    require(cursor.bytesLeft >= PARTICLE_EFFECT_DATA_SIZE) { "Truncated particle effect data." }
    val start = cursor.position
    val result = ParticleEffectData(
        name = cursor.stringAscii(16),
        particleType = cursor.int(),
        textureId = cursor.int(),
        xVariation = cursor.float(),
        yVariation = cursor.float(),
        zVariation = cursor.float(),
        initialScale = cursor.float(),
        randomScaleRange = cursor.float(),
        scaleMultiplier = cursor.float(),
        horizontalSpeed = cursor.float(),
        verticalSpeed = cursor.float(),
        randomHorizontalSpeedRange = cursor.float(),
        randomVerticalSpeedRange = cursor.float(),
        emissionModulationDegreesPerFrame = cursor.float(),
        emissionRate = cursor.float(),
        lifetimeFrames = cursor.int(),
        randomLifetimeFrames = cursor.int(),
        motionMultiplier = cursor.float(),
        verticalVelocityDelta = cursor.float(),
        fadeInFraction = cursor.float(),
        fadeOutFraction = cursor.float(),
        radius = cursor.float(),
        motionOption1 = cursor.float(),
        motionOption2 = cursor.float(),
        redDelta = cursor.float(),
        greenDelta = cursor.float(),
        blueDelta = cursor.float(),
        alpha = cursor.float(),
        red = cursor.float(),
        green = cursor.float(),
        blue = cursor.float(),
        textureFlags = cursor.int(),
        normal = cursor.int(),
        jump = cursor.int(),
        reverse = cursor.int(),
    )
    check(cursor.position - start == PARTICLE_EFFECT_DATA_SIZE)
    return result
}

fun parseParticleEffectDataList(cursor: Cursor, expectedCount: Int): List<ParticleEffectData> {
    require(cursor.bytesLeft == expectedCount * PARTICLE_EFFECT_DATA_SIZE) {
        "Expected ${expectedCount * PARTICLE_EFFECT_DATA_SIZE} particle bytes, got ${cursor.bytesLeft}."
    }
    return List(expectedCount) { parseParticleEffectData(cursor) }
}
