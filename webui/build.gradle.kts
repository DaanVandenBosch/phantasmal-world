import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

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

    tasks.withType<Kotlin2JsCompile> {
        kotlinOptions.freeCompilerArgs += listOf(
            "-Xopt-in=kotlin.RequiresOptIn",
            "-Xopt-in=kotlin.contracts.ExperimentalContracts",
            "-Xopt-in=kotlin.time.ExperimentalTime"
        )
    }
}
