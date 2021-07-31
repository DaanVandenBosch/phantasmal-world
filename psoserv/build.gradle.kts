plugins {
    id("world.phantasmal.jvm")
    application
}

application {
    mainClass.set("world.phantasmal.psoserv.MainKt")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":psolib"))
}
