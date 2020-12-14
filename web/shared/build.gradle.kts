plugins {
    kotlin("js")
    kotlin("plugin.serialization")
}

kotlin {
    js {
        browser {
        }
    }
}

val kotlinLoggingVersion: String by project.extra
val serializationVersion: String by project.extra

dependencies {
    api(project(":lib"))

    api("io.github.microutils:kotlin-logging-js:$kotlinLoggingVersion")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
}
