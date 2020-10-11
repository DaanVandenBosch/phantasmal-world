plugins {
    kotlin("js")
    kotlin("plugin.serialization")
}

kotlin {
    js {
        browser {
            runTask {
                devServer = devServer!!.copy(
                    open = false,
                    port = 1623
                )
            }
        }
        binaries.executable()
    }
}

val coroutinesVersion: String by project.ext
val kotlinLoggingVersion: String by project.extra
val ktorVersion: String by project.extra

dependencies {
    implementation(project(":lib"))
    implementation(project(":webui"))

    implementation("io.github.microutils:kotlin-logging-js:$kotlinLoggingVersion")
    implementation("io.ktor:ktor-client-core-js:$ktorVersion")
    implementation("io.ktor:ktor-client-serialization-js:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core-js:1.0.0")

    testImplementation(kotlin("test-js"))
}
