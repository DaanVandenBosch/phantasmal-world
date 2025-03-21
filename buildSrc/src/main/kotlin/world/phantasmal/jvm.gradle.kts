package world.phantasmal

plugins {
    kotlin("jvm")
    id("world.phantasmal.common")
}

val log4jVersion: String by project.extra

kotlin {
    jvmToolchain(17)
}

dependencies {
    runtimeOnly("org.apache.logging.log4j:log4j-slf4j-impl:$log4jVersion")

    testImplementation(kotlin("test"))
}
