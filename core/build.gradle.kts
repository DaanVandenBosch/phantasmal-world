plugins {
    kotlin("multiplatform")
}

val coroutinesVersion: String by project.ext
val kotlinLoggingVersion: String by project.extra

val jvmVersion: String by project.extra

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = jvmVersion
    }
}

kotlin {
    js {
        browser {}
    }

    jvm()

    sourceSets {
        commonMain {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
                api("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        getByName("jsTest") {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }

        getByName("jvmTest") {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
    }
}
