plugins {
    id("world.phantasmal.jvm")
}

kotlin {
    sourceSets.configureEach {
        languageSettings.optIn("kotlinx.serialization.ExperimentalSerializationApi")
    }
}

dependencies {
    implementation(project(":psolib"))
    implementation(project(":web:shared"))
    implementation("org.jsoup:jsoup:1.13.1")
}

tasks.register<JavaExec>("generateAssets") {
    val outputFile = layout.buildDirectory.get().asFile.resolve("generatedAssets")
    outputs.dir(outputFile)

    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("world.phantasmal.web.assetsGeneration.MainKt")
    args = listOf(outputFile.absolutePath)
}

tasks.register<JavaExec>("extractNpcModels") {
    description = "Extract NPC models from PSO BB game data (data.gsl)"

    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("world.phantasmal.web.assetsGeneration.ExtractNpcModelsKt")

    // Usage: ./gradlew :web:assets-generation:extractNpcModels --args="<pso-data-dir> <output-dir>"
    // Example: --args="D:/PSO/EphineaPSO2/data web/src/jsMain/resources/assets/npcs"
}
