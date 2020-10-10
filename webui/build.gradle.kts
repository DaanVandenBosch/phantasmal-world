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

    testImplementation(kotlin("test-js"))
}
