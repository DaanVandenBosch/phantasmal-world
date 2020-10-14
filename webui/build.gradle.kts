plugins {
    kotlin("js")
}

kotlin {
    js {
        browser {}
    }
}

val coroutinesVersion: String by project.ext

dependencies {
    api(project(":core"))
    api(project(":observable"))
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

    testImplementation(kotlin("test-js"))
    testImplementation(project(":test-utils"))
}
