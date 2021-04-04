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

fun parseNj(cursor: Cursor): PwResult<List<NjObject>> =
    parseNinja(cursor, ::parseNjModel, ::NjObject, mutableMapOf())

fun parseXj(cursor: Cursor): PwResult<List<XjObject>> =
    parseNinja(cursor, { c, _ -> parseXjModel(c) }, ::XjObject, Unit)

fun parseXjObject(cursor: Cursor): List<XjObject> =
    parseSiblingObjects(cursor, { c, _ -> parseXjModel(c) }, ::XjObject, Unit)

private typealias CreateObject<Model, Obj> = (
    evaluationFlags: NinjaEvaluationFlags,
    model: Model?,
    position: Vec3,
    rotation: Vec3,
    scale: Vec3,
    children: MutableList<Obj>,
) -> Obj

private fun <Model : NinjaModel, Obj : NinjaObject<Model, Obj>, Context> parseNinja(
    cursor: Cursor,
    parseModel: (cursor: Cursor, context: Context) -> Model,
    createObject: CreateObject<Model, Obj>,
    context: Context,
): PwResult<List<Obj>> =
    when (val parseIffResult = parseIff(cursor)) {
        is Failure -> parseIffResult
        is Success -> {
            // POF0 and other chunks types are ignored.
            val njcmChunks = parseIffResult.value.filter { chunk -> chunk.type == NJCM }
            val objects: MutableList<Obj> = mutableListOf()

            for (chunk in njcmChunks) {
                objects.addAll(parseSiblingObjects(chunk.data, parseModel, createObject, context))
            }

            Success(objects, parseIffResult.problems)
        }
    }

// TODO: cache model and object offsets so we don't reparse the same data.
private fun <Model : NinjaModel, Obj : NinjaObject<Model, Obj>, Context> parseSiblingObjects(
    cursor: Cursor,
    parseModel: (cursor: Cursor, context: Context) -> Model,
    createObject: CreateObject<Model, Obj>,
    context: Context,
): MutableList<Obj> {
    val evalFlags = cursor.int()
    val noTranslate = evalFlags.isBitSet(0)
    val noRotate = evalFlags.isBitSet(1)
    val noScale = evalFlags.isBitSet(2)
    val hidden = evalFlags.isBitSet(3)
    val breakChildTrace = evalFlags.isBitSet(4)
    val zxyRotationOrder = evalFlags.isBitSet(5)
    val skip = evalFlags.isBitSet(6)
    val shapeSkip = evalFlags.isBitSet(7)
    val clip = evalFlags.isBitSet(8)
    val modifier = evalFlags.isBitSet(9)

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
        parseSiblingObjects(cursor, parseModel, createObject, context)
    }

    val siblings = if (siblingOffset == 0) {
        mutableListOf()
    } else {
        cursor.seekStart(siblingOffset)
        parseSiblingObjects(cursor, parseModel, createObject, context)
    }

    val obj = createObject(
        NinjaEvaluationFlags(
            noTranslate,
            noRotate,
            noScale,
            hidden,
            breakChildTrace,
            zxyRotationOrder,
            skip,
            shapeSkip,
            clip,
            modifier,
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
