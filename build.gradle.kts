import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("js") version "1.4.32" apply false
    kotlin("multiplatform") version "1.4.32" apply false
    kotlin("plugin.serialization") version "1.4.32" apply false
}

tasks.wrapper {
    gradleVersion = "7.0"
}

subprojects {
    project.extra["coroutinesVersion"] = "1.4.3"
    project.extra["junitVersion"] = "5.7.1"
    project.extra["kotlinLoggingVersion"] = "2.0.6"
    project.extra["ktorVersion"] = "1.5.3"
    project.extra["log4jVersion"] = "2.14.1"
    project.extra["serializationVersion"] = "1.1.0"
    project.extra["slf4jVersion"] = "1.7.30"

    repositories {
        mavenCentral()
        maven(url = "https://kotlin.bintray.com/kotlinx/")
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "11"
            freeCompilerArgs += listOf(
                "-Xjvm-default=all"
            )
        }
    }

    tasks.withType<Kotlin2JsCompile> {
        kotlinOptions.freeCompilerArgs += listOf(
            "-Xopt-in=kotlin.RequiresOptIn",
            "-Xopt-in=kotlin.ExperimentalUnsignedTypes",
            "-Xopt-in=kotlin.contracts.ExperimentalContracts",
            "-Xopt-in=kotlin.time.ExperimentalTime"
        )
    }
}
