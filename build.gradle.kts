import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

plugins {
    kotlin("multiplatform") version "1.4.10" apply false
    kotlin("js") version "1.4.10" apply false
}

tasks.wrapper {
    gradleVersion = "6.6.1"
}

subprojects {
    project.extra["kotlinLoggingVersion"] = "2.0.2"

    repositories {
        jcenter()
    }

    tasks.withType<Kotlin2JsCompile> {
        kotlinOptions.freeCompilerArgs += listOf(
            "-Xopt-in=kotlin.ExperimentalUnsignedTypes",
            "-Xopt-in=kotlin.time.ExperimentalTime"
        )
    }
}
