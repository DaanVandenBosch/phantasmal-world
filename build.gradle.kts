import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

plugins {
    kotlin("js") version "1.4.20" apply false
    kotlin("multiplatform") version "1.4.20" apply false
    kotlin("plugin.serialization") version "1.4.20" apply false
}

tasks.wrapper {
    gradleVersion = "6.6.1"
}

subprojects {
    project.extra["coroutinesVersion"] = "1.3.9"
    project.extra["kotlinLoggingVersion"] = "2.0.2"
    project.extra["ktorVersion"] = "1.4.2"
    project.extra["serializationVersion"] = "1.4.20"
    project.extra["slf4jVersion"] = "1.7.30"

    repositories {
        jcenter()
        maven(url = "https://kotlin.bintray.com/kotlinx/")
    }

    tasks.withType<Kotlin2JsCompile> {
        kotlinOptions.freeCompilerArgs += listOf(
            "-Xopt-in=kotlin.RequiresOptIn",
            "-Xopt-in=kotlin.ExperimentalUnsignedTypes",
            "-Xopt-in=kotlin.time.ExperimentalTime"
        )
    }
}
