plugins {
    kotlin("js")
}

kotlin {
    js {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        binaries.executable()
    }
}

val kotlinLoggingVersion: String by project.extra

dependencies {
    api(project(":web:shared"))
    implementation("io.github.microutils:kotlin-logging-js:$kotlinLoggingVersion")

    testImplementation(kotlin("test-js"))
    testImplementation(project(":test-utils"))
}
