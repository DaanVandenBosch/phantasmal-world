plugins {
    kotlin("multiplatform")
}

val junitVersion: String by project.extra

tasks.withType<Test> {
    useJUnitPlatform()
}

kotlin {
    js {
        browser {}
    }

    jvm()

    sourceSets {
        all {
            languageSettings.useExperimentalAnnotation("kotlin.contracts.ExperimentalContracts")
        }

        commonMain {
            dependencies {
                implementation(project(":core"))
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(project(":test-utils"))
            }
        }

        getByName("jsTest") {
            dependencies {
                implementation(kotlin("test-js"))
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
