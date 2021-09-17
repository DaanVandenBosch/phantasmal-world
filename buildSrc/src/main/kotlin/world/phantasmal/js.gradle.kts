package world.phantasmal

plugins {
    kotlin("js")
    id("world.phantasmal.common")
}

kotlin {
    js(IR) {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
    }
}

dependencies {
    testImplementation(kotlin("test-js"))
}
