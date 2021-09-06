package world.phantasmal

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("world.phantasmal.common")
}

val junitVersion: String by project.extra
val log4jVersion: String by project.extra

kotlin {
    sourceSets.configureEach {
        EXPERIMENTAL_ANNOTATIONS.forEach(languageSettings::optIn)
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = freeCompilerArgs + "-Xjvm-default=all"
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
