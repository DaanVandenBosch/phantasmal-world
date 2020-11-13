package world.phantasmal.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * This plugin adds a karmaResources task as dependency to the browserTest and jsBrowserTest tasks
 * as a workaround for https://youtrack.jetbrains.com/issue/KT-42923.
 */
class PwJsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val karmaResources = target.tasks.create("karmaResources", KarmaResourcesTask::class.java)

        target.tasks.configureEach { task ->
            if (task.name == "browserTest" || task.name == "jsBrowserTest") {
                task.dependsOn(karmaResources)
            }
        }
    }
}
