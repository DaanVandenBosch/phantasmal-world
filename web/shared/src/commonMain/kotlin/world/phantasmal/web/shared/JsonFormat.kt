package world.phantasmal.web.shared

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder

val JSON_FORMAT = Json {
    defaultConfig()
}

val JSON_FORMAT_PRETTY = Json {
    defaultConfig()
    prettyPrint = true
}

private fun JsonBuilder.defaultConfig() {
    ignoreUnknownKeys = true
    classDiscriminator = "#type"
}
