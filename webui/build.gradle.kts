plugins {
    id("world.phantasmal.js")
}

dependencies {
    api(project(":core"))
    api(project(":observable"))
    implementation(npm("@fortawesome/fontawesome-free", "^5.13.1"))

    testImplementation(project(":test-utils"))
}
