plugins {
    id("world.phantasmal.multiplatform")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":core"))
            }
        }

        commonTest {
            dependencies {
                implementation(project(":test-utils"))
            }
        }
    }
}
