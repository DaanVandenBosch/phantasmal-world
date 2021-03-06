plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

val serializationVersion: String by project.extra

val jvmVersion: String by project.extra

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = jvmVersion
    }
}

kotlin {
    js {
        browser {
        }
    }

    jvm()

    sourceSets {
        commonMain {
            dependencies {
                api(project(":lib"))

                api("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
            }
        }
    }
}
