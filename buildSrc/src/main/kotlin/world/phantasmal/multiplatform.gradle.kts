package world.phantasmal

plugins {
    kotlin("multiplatform")
    id("world.phantasmal.common")
    id("world.phantasmal.karma-resources")
}

val coroutinesVersion: String by project.ext
val junitVersion: String by project.extra
val kotlinLoggingVersion: String by project.extra
val log4jVersion: String by project.extra

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

    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }

    sourceSets {
        all {
            EXPERIMENTAL_ANNOTATIONS.forEach(languageSettings::useExperimentalAnnotation)
        }

        commonMain {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
                api("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        getByName("jsTest") {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }

        getByName("jvmMain") {
            dependencies {
                runtimeOnly("org.apache.logging.log4j:log4j-slf4j-impl:$log4jVersion")
            }
        }

        getByName("jvmTest") {
            dependencies {
                implementation(kotlin("test-junit5"))
                runtimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
