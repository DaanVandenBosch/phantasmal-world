plugins {
    kotlin("multiplatform")
}

val kotlinLoggingVersion: String by project.extra

kotlin {
    js {
        browser {}
    }

    jvm()

    sourceSets {
        commonMain {
            dependencies {
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
