package world.phantasmal.lib.fileFormats.ninja

import world.phantasmal.core.Failure
import world.phantasmal.core.PwResult
import world.phantasmal.core.Success
import world.phantasmal.core.isBitSet
import world.phantasmal.lib.cursor.Cursor
import world.phantasmal.lib.fileFormats.Vec3
import world.phantasmal.lib.fileFormats.parseIff
import world.phantasmal.lib.fileFormats.vec3Float

private const val NJCM: Int = 0x4D434A4E

fun parseNj(cursor: Cursor): PwResult<List<NinjaObject<NjModel>>> =
    parseNinja(cursor, ::parseNjModel, mutableMapOf())

fun parseXj(cursor: Cursor): PwResult<List<NinjaObject<XjModel>>> =
    parseNinja(cursor, { c, _ -> parseXjModel(c) }, Unit)

fun parseXjObject(cursor: Cursor): List<NinjaObject<XjModel>> =
    parseSiblingObjects(cursor, { c, _ -> parseXjModel(c) }, Unit)

private fun <Model : NinjaModel, Context> parseNinja(
    cursor: Cursor,
    parseModel: (cursor: Cursor, context: Context) -> Model,
    context: Context,
): PwResult<List<NinjaObject<Model>>> =
    when (val parseIffResult = parseIff(cursor)) {
        is Failure -> parseIffResult
        is Success -> {
            // POF0 and other chunks types are ignored.
            val njcmChunks = parseIffResult.value.filter { chunk -> chunk.type == NJCM }
            val objects: MutableList<NinjaObject<Model>> = mutableListOf()

            for (chunk in njcmChunks) {
                objects.addAll(parseSiblingObjects(chunk.data, parseModel, context))
            }

            Success(objects, parseIffResult.problems)
        }
    }

// TODO: cache model and object offsets so we don't reparse the same data.
private fun <Model : NinjaModel, Context> parseSiblingObjects(
    cursor: Cursor,
    parseModel: (cursor: Cursor, context: Context) -> Model,
    context: Context,
): MutableList<NinjaObject<Model>> {
    val evalFlags = cursor.int()
    val noTranslate = evalFlags.isBitSet(0)
    val noRotate = evalFlags.isBitSet(1)
    val noScale = evalFlags.isBitSet(2)
    val hidden = evalFlags.isBitSet(3)
    val breakChildTrace = evalFlags.isBitSet(4)
    val zxyRotationOrder = evalFlags.isBitSet(5)
    val skip = evalFlags.isBitSet(6)
    val shapeSkip = evalFlags.isBitSet(7)

    val modelOffset = cursor.int()
    val pos = cursor.vec3Float()
    val rotation = Vec3(
        angleToRad(cursor.int()),
        angleToRad(cursor.int()),
        angleToRad(cursor.int()),
    )
    val scale = cursor.vec3Float()
    val childOffset = cursor.int()
    val siblingOffset = cursor.int()

    val model = if (modelOffset == 0) {
        null
    } else {
        cursor.seekStart(modelOffset)
        parseModel(cursor, context)
    }

    val children = if (childOffset == 0) {
        mutableListOf()
    } else {
        cursor.seekStart(childOffset)
        parseSiblingObjects(cursor, parseModel, context)
    }

    val siblings = if (siblingOffset == 0) {
        mutableListOf()
    } else {
        cursor.seekStart(siblingOffset)
        parseSiblingObjects(cursor, parseModel, context)
    }

    val obj = NinjaObject(
        NinjaEvaluationFlags(
            noTranslate,
            noRotate,
            noScale,
            hidden,
            breakChildTrace,
            zxyRotationOrder,
            skip,
            shapeSkip,
        ),
        model,
        pos,
        rotation,
        scale,
        children,
    )

    siblings.add(0, obj)
    return siblings
}
