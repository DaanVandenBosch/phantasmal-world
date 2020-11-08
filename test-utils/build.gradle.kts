plugins {
    kotlin("multiplatform")
}

val coroutinesVersion: String by project.ext

kotlin {
    js {
        browser {}
    }

    jvm()

    sourceSets {
        commonMain {
            dependencies {
                api(project(":core"))
                api(kotlin("test-common"))
                api(kotlin("test-annotations-common"))
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
            }
        }

        named("jsMain") {
            dependencies {
                api(kotlin("test-js"))
            }
        }

        named("jvmMain") {
            dependencies {
                api(kotlin("test"))
            }
        }
    }
}
