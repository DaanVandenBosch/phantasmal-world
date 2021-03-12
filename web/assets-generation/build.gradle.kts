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

val kotlinLoggingVersion: String by project.extra
val log4jVersion: String by project.extra

dependencies {
    implementation(project(":lib"))
    implementation(project(":web:shared"))
    implementation("org.jsoup:jsoup:1.13.1")
    implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
    runtimeOnly("org.apache.logging.log4j:log4j-slf4j-impl:$log4jVersion")
}

tasks.register<JavaExec>("generateAssets") {
    val outputFile = File(buildDir, "generatedAssets")
    outputs.dir(outputFile)

    classpath = sourceSets.main.get().runtimeClasspath
    main = "world.phantasmal.web.assetsGeneration.MainKt"
    args = listOf(outputFile.absolutePath)
}
