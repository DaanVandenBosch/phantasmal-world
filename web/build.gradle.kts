plugins {
    id("world.phantasmal.js")
}

kotlin {
    js {
        binaries.executable()
    }
}

dependencies {
    implementation(project(":psolib"))
}
