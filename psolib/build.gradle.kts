plugins {
    kotlin("multiplatform") version "1.6.0"
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
    }

    jvm {
        compilations.configureEach {
            kotlinOptions {
                jvmTarget = "11"
                freeCompilerArgs = freeCompilerArgs + "-Xjvm-default=all"
            }
        }
    }
}
