plugins {
    id("world.phantasmal.multiplatform")
    kotlin("plugin.serialization")
}

val serializationVersion: String by project.extra

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":lib"))

                api("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
            }
        }
    }
}
