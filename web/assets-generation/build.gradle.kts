import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

val jvmVersion: String by project.extra

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = jvmVersion
    }
}

dependencies {
    implementation(project(":lib"))
    implementation(project(":web:shared"))
}

tasks.register<JavaExec>("generateAssets") {
    val outputFile = File(buildDir, "generatedAssets")
    outputs.dir(outputFile)

    classpath = sourceSets.main.get().runtimeClasspath
    main = "world.phantasmal.web.assetsGeneration.Main"
    args = listOf(outputFile.absolutePath)
}
