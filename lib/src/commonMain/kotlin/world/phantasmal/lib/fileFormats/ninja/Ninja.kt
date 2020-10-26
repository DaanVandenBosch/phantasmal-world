package world.phantasmal.lib.fileFormats.ninja

import world.phantasmal.core.Failure
import world.phantasmal.core.PwResult
import world.phantasmal.core.Success
import world.phantasmal.lib.cursor.Cursor
import world.phantasmal.lib.fileFormats.Vec3
import world.phantasmal.lib.fileFormats.parseIff
import world.phantasmal.lib.fileFormats.vec3F32

private const val NJCM: Int = 0x4D434A4E

class NjObject<Model>(
    val evaluationFlags: NjEvaluationFlags,
    val model: Model?,
    val position: Vec3,
    /**
     * Euler angles in radians.
     */
    val rotation: Vec3,
    val scale: Vec3,
    val children: List<NjObject<Model>>,
)

class NjEvaluationFlags(
    val noTranslate: Boolean,
    val noRotate: Boolean,
    val noScale: Boolean,
    val hidden: Boolean,
    val breakChildTrace: Boolean,
    val zxyRotationOrder: Boolean,
    val skip: Boolean,
    val shapeSkip: Boolean,
)

fun parseNj(cursor: Cursor): PwResult<List<NjObject<NjcmModel>>> =
    parseNinja(cursor, ::parseNjcmModel, mutableMapOf())

private fun <Model, Context> parseNinja(
    cursor: Cursor,
    parse_model: (cursor: Cursor, context: Context) -> Model,
    context: Context,
): PwResult<List<NjObject<Model>>> =
    when (val parseIffResult = parseIff(cursor)) {
        is Failure -> parseIffResult
        is Success -> {
            // POF0 and other chunks types are ignored.
            val njcmChunks = parseIffResult.value.filter { chunk -> chunk.type == NJCM }
            val objects: MutableList<NjObject<Model>> = mutableListOf()

            for (chunk in njcmChunks) {
                objects.addAll(parseSiblingObjects(chunk.data, parse_model, context))
            }

            Success(objects, parseIffResult.problems)
        }
    }

// TODO: cache model and object offsets so we don't reparse the same data.
private fun <Model, Context> parseSiblingObjects(
    cursor: Cursor,
    parse_model: (cursor: Cursor, context: Context) -> Model,
    context: Context,
): List<NjObject<Model>> {
    val evalFlags = cursor.u32()
    val noTranslate = (evalFlags and 0b1u) != 0u
    val noRotate = (evalFlags and 0b10u) != 0u
    val noScale = (evalFlags and 0b100u) != 0u
    val hidden = (evalFlags and 0b1000u) != 0u
    val breakChildTrace = (evalFlags and 0b10000u) != 0u
    val zxyRotationOrder = (evalFlags and 0b100000u) != 0u
    val skip = (evalFlags and 0b1000000u) != 0u
    val shapeSkip = (evalFlags and 0b10000000u) != 0u

    val modelOffset = cursor.i32()
    val pos = cursor.vec3F32()
    val rotation = Vec3(
        angleToRad(cursor.i32()),
        angleToRad(cursor.i32()),
        angleToRad(cursor.i32()),
    )
    val scale = cursor.vec3F32()
    val childOffset = cursor.i32()
    val siblingOffset = cursor.i32()

    val model = if (modelOffset == 0) {
        null
    } else {
        cursor.seekStart(modelOffset)
        parse_model(cursor, context)
    }

    val children = if (childOffset == 0) {
        emptyList()
    } else {
        cursor.seekStart(childOffset)
        parseSiblingObjects(cursor, parse_model, context)
    }

    val siblings = if (siblingOffset == 0) {
        emptyList()
    } else {
        cursor.seekStart(siblingOffset)
        parseSiblingObjects(cursor, parse_model, context)
    }

    val obj = NjObject(
        NjEvaluationFlags(
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

    return listOf(obj) + siblings
}
