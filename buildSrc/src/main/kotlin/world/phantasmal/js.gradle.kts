package world.phantasmal

plugins {
    kotlin("js")
    id("world.phantasmal.common")
    id("world.phantasmal.karma-resources")
}

kotlin {
    js {
        compilations.all {
            kotlinOptions {
                freeCompilerArgs = freeCompilerArgs + EXPERIMENTAL_ANNOTATION_COMPILER_ARGS
            }
        }
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
