plugins {
    kotlin("js")
    kotlin("plugin.serialization")
    id("world.phantasmal.gradle.js")
}

kotlin {
    js {
        browser {
            webpackTask {
                cssSupport.enabled = true
            }
            runTask {
                devServer = devServer!!.copy(
                    open = false,
                    port = 1623
                )
                cssSupport.enabled = true
            }
            testTask {
                useKarma {
                    useChromeHeadless()
                    webpackConfig.cssSupport.enabled = true
                }
            }
        }
        binaries.executable()
    }
}

val kotlinLoggingVersion: String by project.extra
val ktorVersion: String by project.extra
val serializationVersion: String by project.extra

dependencies {
    implementation(project(":lib"))
    implementation(project(":webui"))

    implementation("io.github.microutils:kotlin-logging-js:$kotlinLoggingVersion")
    implementation("io.ktor:ktor-client-core-js:$ktorVersion")
    implementation("io.ktor:ktor-client-serialization-js:$ktorVersion")
    implementation("org.jetbrains.kotlin:kotlin-serialization:$serializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.1.1")
    implementation(npm("golden-layout", "^1.5.9"))
    implementation(npm("monaco-editor", "0.20.0"))
    implementation(npm("three", "^0.122.0"))

    implementation(devNpm("file-loader", "^6.0.0"))
    implementation(devNpm("monaco-editor-webpack-plugin", "1.9.0"))

    testImplementation(kotlin("test-js"))
    testImplementation(project(":test-utils"))
}
