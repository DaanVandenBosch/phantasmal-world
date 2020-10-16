package world.phantasmal.lib.fileFormats.ninja

import kotlin.math.PI
import kotlin.math.round

private const val ANGLE_TO_RAD = ((2 * PI) / 0x10000).toFloat()
private const val RAD_TO_ANGLE = (0x10000 / (2 * PI)).toFloat()

fun angleToRad(angle: Int): Float {
    return angle * ANGLE_TO_RAD
}

fun radToAngle(rad: Float): Int {
    return round(rad * RAD_TO_ANGLE).toInt()
}
