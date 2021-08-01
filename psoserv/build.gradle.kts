plugins {
    id("world.phantasmal.jvm")
    kotlin("plugin.serialization")
    application
}

application {
    mainClass.set("world.phantasmal.psoserv.MainKt")
}

val serializationVersion: String by project.extra

dependencies {
    implementation(project(":core"))
    implementation(project(":psolib"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
}
