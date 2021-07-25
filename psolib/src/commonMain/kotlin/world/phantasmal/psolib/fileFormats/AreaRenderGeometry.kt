package world.phantasmal.psolib.fileFormats

import mu.KotlinLogging
import world.phantasmal.core.isBitSet
import world.phantasmal.psolib.cursor.Cursor
import world.phantasmal.psolib.fileFormats.ninja.*

private val logger = KotlinLogging.logger {}

class AreaGeometry(
    val sections: List<AreaSection>,
)

class AreaSection(
    val id: Int,
    val position: Vec3,
    val rotation: Vec3,
    val radius: Float,
    val objects: List<AreaObject.Simple>,
    val animatedObjects: List<AreaObject.Animated>,
)

sealed class AreaObject {
    abstract val offset: Int
    abstract val xjObject: XjObject
    abstract val flags: Int

    class Simple(
        override val offset: Int,
        override val xjObject: XjObject,
        override val flags: Int,
    ) : AreaObject()

    class Animated(
        override val offset: Int,
        override val xjObject: XjObject,
        val njMotion: NjMotion,
        val speed: Float,
        override val flags: Int,
    ) : AreaObject()
}

fun parseAreaRenderGeometry(cursor: Cursor): AreaGeometry {
    val dataOffset = parseRel(cursor, parseIndex = false).dataOffset

    cursor.seekStart(dataOffset)
    val format = cursor.stringAscii(maxByteLength = 4, nullTerminated = true, dropRemaining = true)

    if (format != "fmt2") {
        logger.warn { """Expected format to be "fmt2" but was "$format".""" }
    }

    cursor.seek(4)
    val sectionsCount = cursor.int()
    cursor.seek(4)
    val sectionsOffset = cursor.int()

    val sections = mutableListOf<AreaSection>()

    // Cache keys are offsets.
    val simpleAreaObjectCache = mutableMapOf<Int, List<AreaObject.Simple>>()
    val animatedAreaObjectCache = mutableMapOf<Int, List<AreaObject.Animated>>()
    val njMotionCache = mutableMapOf<Int, NjMotion>()

    for (i in 0 until sectionsCount) {
        cursor.seekStart(sectionsOffset + 52 * i)

        val sectionId = cursor.int()
        val sectionPosition = cursor.vec3Float()
        val sectionRotation = Vec3(
            angleToRad(cursor.int()),
            angleToRad(cursor.int()),
            angleToRad(cursor.int()),
        )

        val radius = cursor.float()

        val simpleAreaObjectsOffset = cursor.int()
        val animatedAreaObjectsOffset = cursor.int()
        val simpleAreaObjectsCount = cursor.int()
        val animatedAreaObjectsCount = cursor.int()
        // Ignore the last 4 bytes.

//        println("section $sectionId (index $i), simple geom at $simpleGeometryTableOffset, animated geom at $animatedGeometryTableOffset")

        @Suppress("UNCHECKED_CAST")
        val simpleObjects = simpleAreaObjectCache.getOrPut(simpleAreaObjectsOffset) {
            parseAreaObjects(
                cursor,
                njMotionCache,
                simpleAreaObjectsOffset,
                simpleAreaObjectsCount,
                animated = false,
            ) as List<AreaObject.Simple>
        }

        @Suppress("UNCHECKED_CAST")
        val animatedObjects = animatedAreaObjectCache.getOrPut(animatedAreaObjectsOffset) {
            parseAreaObjects(
                cursor,
                njMotionCache,
                animatedAreaObjectsOffset,
                animatedAreaObjectsCount,
                animated = true,
            ) as List<AreaObject.Animated>
        }

        sections.add(AreaSection(
            sectionId,
            sectionPosition,
            sectionRotation,
            radius,
            simpleObjects,
            animatedObjects,
        ))
    }

    return AreaGeometry(sections)
}

private fun parseAreaObjects(
    cursor: Cursor,
    njMotionCache: MutableMap<Int, NjMotion>,
    offset: Int,
    count: Int,
    animated: Boolean,
): List<AreaObject> {
    val objectSize = if (animated) 32 else 16
    val objects = mutableListOf<AreaObject>()

    for (i in 0 until count) {
        val objectOffset = offset + objectSize * i
        cursor.seekStart(objectOffset)

        var xjObjectOffset = cursor.int()
        val speed: Float?
        val njMotionOffset: Int?

        if (animated) {
            njMotionOffset = cursor.int()
            cursor.seek(8)
            speed = cursor.float()
        } else {
            speed = null
            njMotionOffset = null
        }

        cursor.seek(8) // Skip slide texture ID offset and swap texture ID offset.

        val flags = cursor.int()

        if (flags.isBitSet(2)) {
            xjObjectOffset = cursor.seekStart(xjObjectOffset).int()
        }

        cursor.seekStart(xjObjectOffset)
        val xjObjects = parseXjObject(cursor)

        if (xjObjects.size > 1) {
            logger.warn {
                "Expected exactly one xjObject at ${xjObjectOffset}, got ${xjObjects.size}."
            }
        }

        val xjObject = xjObjects.first()

        val njMotion = njMotionOffset?.let {
            njMotionCache.getOrPut(njMotionOffset) {
                cursor.seekStart(njMotionOffset)
                parseMotion(cursor, v2Format = false)
            }
        }

        objects.add(
            if (animated) {
                AreaObject.Animated(objectOffset, xjObject, njMotion!!, speed!!, flags)
            } else {
                AreaObject.Simple(objectOffset, xjObject, flags)
            }
        )
    }

    return objects
}
