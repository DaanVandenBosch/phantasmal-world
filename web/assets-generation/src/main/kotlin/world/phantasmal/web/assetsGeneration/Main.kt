package world.phantasmal.web.assetsGeneration

import java.io.File

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.isNotEmpty()) {
            "Expected at least one argument denoting the directory where assets should be generated."
        }

        val outputDir = File(args.first())
        outputDir.mkdirs()

        Ephinea.generateAssets(outputDir)
    }
}
