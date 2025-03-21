package world.phantasmal

import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinTest

plugins {
    kotlin("plugin.serialization") apply false
}

repositories {
    mavenCentral()
}

tasks.withType<KotlinCompilationTask<*>> {
    compilerOptions {
        allWarningsAsErrors.set(true)
        optIn.set(
            listOf(
                "kotlin.contracts.ExperimentalContracts",
                "kotlin.ExperimentalUnsignedTypes",
            )
        )
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JVM_17)
        freeCompilerArgs.addAll(
            "-Xjdk-release=${jvmTarget.get().target}",
            "-Xjvm-default=all",
        )
    }
}

// Non-JVM tests.
tasks.withType<KotlinTest>().configureEach {
    // Always run all tests.
    outputs.upToDateWhen { false }
}

// JVM tests.
tasks.withType<Test>().configureEach {
    // Always run all tests.
    outputs.upToDateWhen { false }

    useJUnitPlatform()
}

project.extra["coroutinesVersion"] = "1.10.1"
project.extra["kotlinLoggingVersion"] = "2.0.11"
project.extra["log4jVersion"] = "2.14.1"
project.extra["serializationVersion"] = "1.8.0"
