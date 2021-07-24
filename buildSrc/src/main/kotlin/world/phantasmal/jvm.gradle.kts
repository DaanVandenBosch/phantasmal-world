package world.phantasmal

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("world.phantasmal.common")
}

val junitVersion: String by project.extra
val log4jVersion: String by project.extra

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = freeCompilerArgs + EXPERIMENTAL_ANNOTATION_COMPILER_ARGS
    }
}

dependencies {
    runtimeOnly("org.apache.logging.log4j:log4j-slf4j-impl:$log4jVersion")

    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
