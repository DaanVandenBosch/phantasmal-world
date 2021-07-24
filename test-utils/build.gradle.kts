plugins {
    id("world.phantasmal.multiplatform")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":core"))
                api(kotlin("test-common"))
                api(kotlin("test-annotations-common"))
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
