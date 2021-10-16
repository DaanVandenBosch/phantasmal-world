import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin

tasks.wrapper {
    gradleVersion = "7.1.1"
}

// Temporary fix for issue with incompatible webpack-cli and @webpack-cli versions.
rootProject.plugins.withType<NodeJsRootPlugin> {
    rootProject.the<NodeJsRootExtension>().versions.webpackCli.version = "4.9.0"
}
