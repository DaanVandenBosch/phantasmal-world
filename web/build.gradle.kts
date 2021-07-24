plugins {
    id("world.phantasmal.js")
    kotlin("plugin.serialization")
}

kotlin {
    js {
        browser {
            commonWebpackConfig {
                cssSupport.enabled = true
            }
            runTask {
                devServer = devServer!!.copy(
                    open = false,
                    port = 1623,
                )
            }
        }
        binaries.executable()
    }
}

val ktorVersion: String by project.extra
val serializationVersion: String by project.extra

dependencies {
    implementation(project(":lib"))
    implementation(project(":webui"))
    implementation(project(":web:shared"))

    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-serialization:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.2.1")
    implementation(npm("golden-layout", "^1.5.9"))
    implementation(npm("monaco-editor", "0.26.1"))
    implementation(npm("three", "^0.128.0"))
    implementation(npm("javascript-lp-solver", "0.4.17"))

    implementation(devNpm("file-loader", "^6.2.0"))
    implementation(devNpm("monaco-editor-webpack-plugin", "4.1.1"))

    testImplementation(project(":test-utils"))
}

val copyAssemblyWorkerJsTask = tasks.register<Copy>("copyAssemblyWorkerJs") {
    dependsOn(":web:assembly-worker:build")

    val workerDist = project(":web:assembly-worker").buildDir.resolve("distributions")

    from(workerDist.resolve("assembly-worker.js"), workerDist.resolve("assembly-worker.js.map"))
    into(buildDir.resolve("processedResources/js/main"))
}

// TODO: Figure out how to make this work with --continuous.
tasks.getByName("processResources").dependsOn(copyAssemblyWorkerJsTask)

tasks.register<Copy>("generateAssets") {
    dependsOn(":web:assets-generation:generateAssets")

    from("assets-generation/build/generatedAssets")
    into("src/main/resources/assets")
}
