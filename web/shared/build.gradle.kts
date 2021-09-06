plugins {
    id("world.phantasmal.multiplatform")
    kotlin("plugin.serialization")
}

val serializationVersion: String by project.extra

kotlin {
    sourceSets {
        configureEach {
            languageSettings.optIn("kotlinx.serialization.ExperimentalSerializationApi")
        }
        commonMain {
            dependencies {
                api(project(":psolib"))

                api("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
            }
        }
    }
}
