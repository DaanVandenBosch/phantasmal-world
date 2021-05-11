package world.phantasmal.web.assetsGeneration

import world.phantasmal.web.assetsGeneration.ephinea.generateEphineaAssets
import java.io.File

fun main(args: Array<String>) {
    require(args.isNotEmpty()) {
        "Expected at least one argument denoting the directory where assets should be generated."
    }

    val outputDir = File(args.first())
    outputDir.mkdirs()

    generateEphineaAssets(outputDir)
}
