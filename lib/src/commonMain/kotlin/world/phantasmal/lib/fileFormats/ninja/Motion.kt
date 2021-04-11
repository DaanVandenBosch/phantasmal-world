package world.phantasmal.lib.fileFormats.ninja

import world.phantasmal.core.isBitSet
import world.phantasmal.lib.cursor.Cursor
import world.phantasmal.lib.fileFormats.Vec3
import world.phantasmal.lib.fileFormats.vec3Float

private const val NMDM = 0x4d444d4e

class NjMotion(
    val motionData: List<NjMotionData>,
    val frameCount: Int,
    val type: Int,
    val interpolation: NjInterpolation,
    val elementCount: Int,
)

enum class NjInterpolation {
    Linear,
    Spline,
    UserFunction,
}

class NjMotionData(
    val tracks: List<NjKeyframeTrack>,
)

sealed class NjKeyframeTrack {
    abstract val keyframes: List<NjKeyframe>

    class Position(
        override val keyframes: List<NjKeyframe.Vector>,
    ) : NjKeyframeTrack()

    class EulerAngles(
        override val keyframes: List<NjKeyframe.Vector>,
    ) : NjKeyframeTrack()

    class Scale(
        override val keyframes: List<NjKeyframe.Vector>,
    ) : NjKeyframeTrack()

    class Quaternion(
        override val keyframes: List<NjKeyframe.Quaternion>,
    ) : NjKeyframeTrack()
}

sealed class NjKeyframe {
    abstract val frame: Int

    /**
     * Used for parallel motion (POS), scale (SCL), vector (VEC) or rotation (ANG).
     */
    class Vector(
        override val frame: Int,
        /**
         * Position, scale, vector, or euler angles in radians.
         */
        val value: Vec3,
    ) : NjKeyframe()

    class Quaternion(
        override val frame: Int,
        val real: Float,
        val imaginary: Vec3,
    ) : NjKeyframe()
}

fun parseNjm(cursor: Cursor): NjMotion =
    if (cursor.int() == NMDM) {
        parseNjmV2(cursor)
    } else {
        cursor.seekStart(0)
        parseNjmBb(cursor)
    }

/**
 * Format used by PSO v2 and for the enemies in PSO:BB.
 */
private fun parseNjmV2(cursor: Cursor): NjMotion {
    val chunkSize = cursor.int()
    return parseMotion(cursor.take(chunkSize), v2Format = true)
}

/**
 * Format used by PSO:BB plymotiondata.rlc.
 */
private fun parseNjmBb(cursor: Cursor): NjMotion {
    cursor.seekEnd(16)
    val offset1 = cursor.int()
    cursor.seekStart(offset1)
    val actionOffset = cursor.int()
    cursor.seekStart(actionOffset)
    return parseAction(cursor)
}

private fun parseAction(cursor: Cursor): NjMotion {
    cursor.seek(4) // Object pointer placeholder.
    val motionOffset = cursor.int()
    cursor.seekStart(motionOffset)
    return parseMotion(cursor, v2Format = false)
}

fun parseMotion(cursor: Cursor, v2Format: Boolean): NjMotion {
    // For v2, try to determine the end of the mData offset table by finding the lowest mDataOffset
    // value. This is usually the value that the first mDataOffset points to. This value is assumed
    // to be the end of the mDataOffset table.
    var mDataTableEnd = if (v2Format) cursor.size else cursor.position

    // Points to an array with an element per bone.
    val mDataTableOffset = cursor.int()
    val frameCount = cursor.int()
    val type = cursor.uShort().toInt()
    val inpFn = cursor.uShort().toInt()
    // Linear, spline or user private fun.
    val interpolation = when (val inp = (inpFn and 0b11000000) ushr 6) {
        0 -> NjInterpolation.Linear
        1 -> NjInterpolation.Spline
        // Default to UserFunction.
        2, 3 -> NjInterpolation.UserFunction
        else -> error("Interpolation bits should be between 0 and 3, inclusive but were ${inp}.")
    }
    val elementCount = inpFn and 0b1111
    val motionDataList = mutableListOf<NjMotionData>()
    var mDataOffset = mDataTableOffset

    while (mDataOffset < mDataTableEnd) {
        cursor.seekStart(mDataOffset)
        mDataOffset += 8 * elementCount

        val tracks = mutableListOf<NjKeyframeTrack>()

        val keyframeOffsets = mutableListOf<Int>()
        val keyframeCounts = mutableListOf<Int>()

        repeat(elementCount) {
            val keyframeOffset = cursor.int()
            keyframeOffsets.add(keyframeOffset)

            if (v2Format && keyframeOffset != 0 && keyframeOffset < mDataTableEnd) {
                mDataTableEnd = keyframeOffset
            }
        }

        repeat(elementCount) {
            keyframeCounts.add(cursor.int())
        }

        // NJD_MTYPE_POS_0
        if (type.isBitSet(0)) {
            cursor.seekStart(keyframeOffsets.removeFirst())
            val count = keyframeCounts.removeFirst()

            tracks.add(NjKeyframeTrack.Position(
                keyframes = parseVectorKeyframes(cursor, count),
            ))
        }

        // NJD_MTYPE_ANG_1
        if (type.isBitSet(1)) {
            cursor.seekStart(keyframeOffsets.removeFirst())
            val count = keyframeCounts.removeFirst()

            tracks.add(NjKeyframeTrack.EulerAngles(
                keyframes = parseEulerAngleKeyframes(cursor, count, frameCount),
            ))
        }

        // NJD_MTYPE_SCL_2
        if (type.isBitSet(2)) {
            cursor.seekStart(keyframeOffsets.removeFirst())
            val count = keyframeCounts.removeFirst()

            tracks.add(NjKeyframeTrack.Scale(
                keyframes = parseVectorKeyframes(cursor, count),
            ))
        }

        // NJD_MTYPE_QUAT_1
        if (type.isBitSet(13)) {
            cursor.seekStart(keyframeOffsets.removeFirst())
            val count = keyframeCounts.removeFirst()

            tracks.add(NjKeyframeTrack.Quaternion(
                keyframes = parseQuaternionKeyframes(cursor, count),
            ))
        }

        // TODO: all NJD_MTYPE's

        motionDataList.add(NjMotionData(tracks))
    }

    return NjMotion(
        motionData = motionDataList,
        frameCount,
        type,
        interpolation,
        elementCount,
    )
}

private fun parseVectorKeyframes(cursor: Cursor, count: Int): List<NjKeyframe.Vector> {
    val frames = mutableListOf<NjKeyframe.Vector>()

    repeat(count) {
        frames.add(NjKeyframe.Vector(
            frame = cursor.int(),
            value = cursor.vec3Float(),
        ))
    }

    return frames
}

private fun parseEulerAngleKeyframes(
    cursor: Cursor,
    keyframeCount: Int,
    frameCount: Int,
): List<NjKeyframe.Vector> {
    val frames = mutableListOf<NjKeyframe.Vector>()
    val startPos = cursor.position

    repeat(keyframeCount) {
        frames.add(NjKeyframe.Vector(
            frame = cursor.uShort().toInt(),
            value = Vec3(
                angleToRad(cursor.uShort().toInt()),
                angleToRad(cursor.uShort().toInt()),
                angleToRad(cursor.uShort().toInt()),
            ),
        ))
    }

    var prev = -1

    for (keyframe in frames) {
        if (keyframe.frame < prev || keyframe.frame >= frameCount) {
            cursor.seekStart(startPos)
            return parseEulerAngleKeyframesWide(cursor, keyframeCount)
        }

        prev = keyframe.frame
    }

    return frames
}

private fun parseEulerAngleKeyframesWide(
    cursor: Cursor,
    keyframeCount: Int,
): List<NjKeyframe.Vector> {
    val frames = mutableListOf<NjKeyframe.Vector>()

    repeat(keyframeCount) {
        frames.add(NjKeyframe.Vector(
            frame = cursor.int(),
            value = Vec3(
                angleToRad(cursor.int()),
                angleToRad(cursor.int()),
                angleToRad(cursor.int()),
            ),
        ))
    }

    return frames
}

private fun parseQuaternionKeyframes(
    cursor: Cursor,
    keyframeCount: Int,
): List<NjKeyframe.Quaternion> {
    val frames = mutableListOf<NjKeyframe.Quaternion>()

    repeat(keyframeCount) {
        frames.add(NjKeyframe.Quaternion(
            frame = cursor.int(),
            real = cursor.float(),
            imaginary = cursor.vec3Float(),
        ))
    }

    return frames
}
