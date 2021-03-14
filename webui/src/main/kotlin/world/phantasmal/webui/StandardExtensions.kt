package world.phantasmal.webui

import kotlin.math.roundToInt

fun Double.toRoundedString(decimals: Int): String =
    if (decimals <= 0) roundToInt().toString()
    else {
        var multiplied = this
        repeat(decimals) { multiplied *= 10 }
        val str = multiplied.roundToInt().toString()

        if (this < 1) "0.$str"
        else "${str.dropLast(decimals)}.${str.takeLast(decimals)}"
    }
