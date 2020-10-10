plugins {
    kotlin("js")
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

val kotlinLoggingVersion: String by project.extra

dependencies {
    implementation(project(":lib"))
    implementation(project(":webui"))

    implementation("io.github.microutils:kotlin-logging-js:$kotlinLoggingVersion")

    testImplementation(kotlin("test-js"))
}
