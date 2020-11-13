package world.phantasmal.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * This task generates a Karma configuration in karma.config.d that ensures Karma serves files from
 * the resources directories.
 */
open class KarmaResourcesTask : DefaultTask() {
    private val outputFile = project.file("karma.config.d/karma.config.generated.js")

    init {
        outputs.file(outputFile)
    }

    @TaskAction
    fun generateKarmaConfig() {
        outputFile.outputStream().use { stream ->
            val writer = stream.writer()
            val path = project.projectDir.absolutePath.replace("\\", "\\\\")
            writer.write("const PROJECT_PATH = '$path';\n\n")
            writer.flush()

            KarmaResourcesTask::class.java.getResourceAsStream("/karmaConfig.js").copyTo(stream)
        }
    }
}
