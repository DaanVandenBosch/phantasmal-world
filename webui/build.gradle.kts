plugins {
    kotlin("js")
}

kotlin {
    js {
        browser {}
    }
}

dependencies {
    api(project(":core"))
    api(project(":observable"))
    implementation(npm("@fortawesome/fontawesome-free", "^5.13.1"))

    testImplementation(kotlin("test-js"))
    testImplementation(project(":test-utils"))
}
