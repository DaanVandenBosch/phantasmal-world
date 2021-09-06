package world.phantasmal

plugins {
    kotlin("js")
    id("world.phantasmal.common")
    id("world.phantasmal.karma-resources")
}

kotlin {
    js {
        compilations.configureEach {
            EXPERIMENTAL_ANNOTATIONS.forEach(languageSettings::optIn)
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
