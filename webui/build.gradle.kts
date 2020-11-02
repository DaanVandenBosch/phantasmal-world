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
    implementation(npm("@fortawesome/fontawesome-free", "^5.13.1"))

    testImplementation(kotlin("test-js"))
    testImplementation(project(":test-utils"))
}
