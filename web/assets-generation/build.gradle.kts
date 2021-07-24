plugins {
    id("world.phantasmal.jvm")
}

dependencies {
    implementation(project(":lib"))
    implementation(project(":web:shared"))
    implementation("org.jsoup:jsoup:1.13.1")
}

tasks.register<JavaExec>("generateAssets") {
    val outputFile = File(buildDir, "generatedAssets")
    outputs.dir(outputFile)

    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("world.phantasmal.web.assetsGeneration.MainKt")
    args = listOf(outputFile.absolutePath)
}
