package world.phantasmal

// This task generates a Karma configuration in karma.config.d that ensures Karma serves files from
// the resources directories.
// This is a workaround for https://youtrack.jetbrains.com/issue/KT-42923.
val karmaResourcesTask: TaskProvider<Task> = tasks.register("karmaResources") {
    doLast {
        val karmaConfigDir = file("karma.config.d")
        karmaConfigDir.mkdir()

        object {}::class.java.getResourceAsStream("karmaConfig.js")!!.use { karmaConfigStream ->
            karmaConfigDir.resolve("karma.config.generated.js").outputStream().use { stream ->
                val writer = stream.writer()
                val path = project.projectDir.absolutePath.replace("\\", "\\\\")
                writer.write("const PROJECT_PATH = '$path';\n\n")
                writer.flush()

                karmaConfigStream.copyTo(stream)
            }
        }
    }
}

tasks.configureEach {
    if (name == "browserTest" || name == "jsBrowserTest") {
        dependsOn(karmaResourcesTask)
    }
}
