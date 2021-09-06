plugins {
    id("world.phantasmal.js")
}

kotlin {
    js {
        compilations.configureEach {
            languageSettings.optIn("kotlinx.serialization.ExperimentalSerializationApi")
        }

        binaries.executable()
    }
}

dependencies {
    api(project(":web:shared"))

    testImplementation(project(":test-utils"))
}
