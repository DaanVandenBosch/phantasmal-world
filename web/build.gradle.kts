plugins {
    id("world.phantasmal.js")
}

val serializationVersion: String by project.extra

kotlin {
    js {
        browser {
            commonWebpackConfig {
                cssSupport { enabled.set(true) }
            }
            runTask {
                devServerProperty.set(
                    devServerProperty.get().copy(
                        open = false,
                        port = 1623,
                    )
                )
            }
        }
        binaries.executable()
    }

    sourceSets {
        getByName("jsMain") {
            dependencies {
                implementation(kotlin("reflect"))
                implementation(project(":psolib"))
                implementation(project(":webui"))
                implementation(project(":web:shared"))

                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
                implementation(npm("golden-layout", "^1.5.9"))
                implementation(npm("monaco-editor", "0.26.1"))
                implementation(npm("three", "^0.128.0"))
                implementation(npm("javascript-lp-solver", "0.4.17"))

                implementation(devNpm("file-loader", "^6.2.0"))
                implementation(devNpm("monaco-editor-webpack-plugin", "4.1.1"))
            }
        }

        getByName("jsTest") {
            dependencies {
                implementation(project(":test-utils"))
            }
        }
    }
}

val copyAssemblyWorkerJsTask = tasks.register<Copy>("copyAssemblyWorkerJs") {
    dependsOn(":web:assembly-worker:jsBrowserDistribution")

    val workerDist =
        project(":web:assembly-worker").layout.buildDirectory.get().asFile.resolve("dist/js/productionExecutable")

    from(workerDist.resolve("assembly-worker.js"), workerDist.resolve("assembly-worker.js.map"))
    into(layout.buildDirectory.get().asFile.resolve("processedResources/js/main"))
}

// TODO: Figure out how to make this work with --continuous.
tasks.named("jsProcessResources").configure { dependsOn(copyAssemblyWorkerJsTask) }

tasks.register<Copy>("generateAssets") {
    dependsOn(":web:assets-generation:generateAssets")

    from("assets-generation/build/generatedAssets")
    into("src/main/resources/assets")
}
