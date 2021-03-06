plugins {
    kotlin("multiplatform")
}

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
                implementation(project(":core"))
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(project(":test-utils"))
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
