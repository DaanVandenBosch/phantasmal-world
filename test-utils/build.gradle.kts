plugins {
    kotlin("multiplatform")
}

kotlin {
    js {
        browser {}
    }

    sourceSets {
        commonMain {
            dependencies {
                api(project(":core"))
                api(kotlin("test-common"))
                api(kotlin("test-annotations-common"))
            }
        }

        val jsMain by getting {
            dependencies {
                api(kotlin("test-js"))
            }
        }
    }
}
