plugins {
    id("world.phantasmal.js")
}

kotlin {
    js {
        binaries.executable()
    }
}

dependencies {
    api(project(":web:shared"))

    testImplementation(project(":test-utils"))
}
