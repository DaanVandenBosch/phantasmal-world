plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

val serializationVersion: String by project.extra

kotlin {
    js(IR) {
        browser {
        }
    }

    jvm()

    sourceSets {
        commonMain {
            dependencies {
                api(project(":lib"))

                api("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
            }
        }
    }
}
