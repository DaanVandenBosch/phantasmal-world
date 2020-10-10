plugins {
    kotlin("multiplatform")
}

val kotlinLoggingVersion: String by project.extra

kotlin {
    js {
        browser {}
    }

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

        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

tasks.register("test") {
    dependsOn("allTests")
}
