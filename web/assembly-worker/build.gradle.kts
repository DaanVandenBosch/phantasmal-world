plugins {
    kotlin("js")
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
        binaries.executable()
    }
}

dependencies {
    api(project(":web:shared"))

    testImplementation(kotlin("test-js"))
    testImplementation(project(":test-utils"))
}
