package world.phantasmal.webui

import kotlin.time.Duration

fun Duration.formatAsHoursAndMinutes(): String =
    toComponents { hours, minutes, _, _ ->
        "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
    }
