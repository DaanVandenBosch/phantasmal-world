package world.phantasmal

import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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
        jvmTarget.set(JVM_11)
        freeCompilerArgs.addAll(
            "-Xjdk-release=${jvmTarget.get().target}",
            "-Xjvm-default=all",
        )
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

project.extra["coroutinesVersion"] = "1.9.0-RC"
project.extra["kotlinLoggingVersion"] = "2.0.11"
project.extra["ktorVersion"] = "2.3.12"
project.extra["log4jVersion"] = "2.14.1"
project.extra["serializationVersion"] = "1.7.1"
