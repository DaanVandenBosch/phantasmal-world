package world.phantasmal.webui

import kotlin.math.roundToInt

fun Double.toRoundedString(decimals: Int): String =
    if (decimals <= 0) roundToInt().toString()
    else {
        var multiplied = this
        repeat(decimals) { multiplied *= 10 }
        val str = multiplied.roundToInt().toString()
        val intPart = str.dropLast(decimals)
        val decPart = str.takeLast(decimals).padStart(decimals, '0')

        if (intPart.isEmpty()) "0.$decPart"
        else "$intPart.$decPart"
    }
