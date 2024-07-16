package world.phantasmal

plugins {
    kotlin("multiplatform")
    id("world.phantasmal.js")
    id("world.phantasmal.karma-resources")
}

val coroutinesVersion: String by project.ext
val kotlinLoggingVersion: String by project.extra
val log4jVersion: String by project.extra

kotlin {
    jvmToolchain(11)

    jvm {}

    sourceSets {
        commonMain {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
                api("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
            }
        }

        getByName("jvmMain") {
            dependencies {
                runtimeOnly("org.apache.logging.log4j:log4j-slf4j-impl:$log4jVersion")
            }
        }
    }
}
