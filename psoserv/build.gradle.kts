import org.graalvm.buildtools.gradle.tasks.BuildNativeImageTask

plugins {
    id("world.phantasmal.jvm")
    kotlin("plugin.serialization")
    application
    id("org.graalvm.buildtools.native") version "0.9.2"
}

val mainClassFqn = "world.phantasmal.psoserv.MainKt"
val agentOutputDir = File(buildDir, "agent-output")

application {
    mainClass.set(mainClassFqn)
}

val nativeAgentRun by tasks.registering(JavaExec::class) {
    description = "Run with the GraalVM native-image-agent to produce reflection info etc. for the nativeBuild task."
    group = "application"

    dependsOn(tasks.build)
    outputs.dir(agentOutputDir)

    mainClass.set(mainClassFqn)
    classpath = sourceSets.main.get().runtimeClasspath
    jvmArgs = listOf("-agentlib:native-image-agent=config-output-dir=$agentOutputDir")
    args = listOf(
        "--nostart",
        "--config=graalvm-agent.conf",
    )
}

nativeBuild {
    imageName.set("psoserv")
    mainClass.set(mainClassFqn)
    buildArgs.addAll(
        "--allow-incomplete-classpath",
        "-H:ConfigurationFileDirectories=$agentOutputDir",
    )
}

tasks.withType<BuildNativeImageTask>().configureEach {
    dependsOn(nativeAgentRun)
    inputs.dir(agentOutputDir)
}

val serializationVersion: String by project.extra

dependencies {
    implementation(project(":core"))
    implementation(project(":psolib"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-hocon:$serializationVersion")
}
