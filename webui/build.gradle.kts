plugins {
    id("world.phantasmal.js")
}

dependencies {
    api(project(":core"))
    api(project(":observable"))
    implementation(npm("@fortawesome/fontawesome-svg-core", "^1.2.36"))
    implementation(npm("@fortawesome/free-regular-svg-icons", "^5.15.4"))
    implementation(npm("@fortawesome/free-solid-svg-icons", "^5.15.4"))
    implementation(npm("@fortawesome/free-brands-svg-icons", "^5.15.4"))

    testImplementation(project(":test-utils"))
}
