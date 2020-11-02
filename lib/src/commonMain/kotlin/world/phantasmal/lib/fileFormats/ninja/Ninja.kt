package world.phantasmal.lib.fileFormats.ninja

import world.phantasmal.core.Failure
import world.phantasmal.core.PwResult
import world.phantasmal.core.Success
import world.phantasmal.lib.cursor.Cursor
import world.phantasmal.lib.fileFormats.Vec3
import world.phantasmal.lib.fileFormats.parseIff
import world.phantasmal.lib.fileFormats.vec3F32

private const val NJCM: Int = 0x4D434A4E

fun parseNj(cursor: Cursor): PwResult<List<NinjaObject<NjcmModel>>> =
    parseNinja(cursor, ::parseNjcmModel, mutableMapOf())

fun parseXj(cursor: Cursor): PwResult<List<NinjaObject<XjModel>>> =
    parseNinja(cursor, { _, _ -> XjModel() }, Unit)

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
    val evalFlags = cursor.uInt()
    val noTranslate = (evalFlags and 0b1u) != 0u
    val noRotate = (evalFlags and 0b10u) != 0u
    val noScale = (evalFlags and 0b100u) != 0u
    val hidden = (evalFlags and 0b1000u) != 0u
    val breakChildTrace = (evalFlags and 0b10000u) != 0u
    val zxyRotationOrder = (evalFlags and 0b100000u) != 0u
    val skip = (evalFlags and 0b1000000u) != 0u
    val shapeSkip = (evalFlags and 0b10000000u) != 0u

    val modelOffset = cursor.int()
    val pos = cursor.vec3F32()
    val rotation = Vec3(
        angleToRad(cursor.int()),
        angleToRad(cursor.int()),
        angleToRad(cursor.int()),
    )
    val scale = cursor.vec3F32()
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
