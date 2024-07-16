package world.phantasmal

plugins {
    kotlin("multiplatform")
    id("world.phantasmal.common")
    id("world.phantasmal.karma-resources")
}

kotlin {
    js {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
    }

    sourceSets {
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
