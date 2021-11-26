plugins {
    kotlin("js") version "1.6.0"
}

repositories {
    mavenCentral()
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

        binaries.executable()
    }
}

dependencies {
    implementation(project(":psolib"))
}
