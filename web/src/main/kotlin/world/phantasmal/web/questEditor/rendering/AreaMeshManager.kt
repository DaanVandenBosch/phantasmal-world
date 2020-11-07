package world.phantasmal.web.questEditor.rendering

import mu.KotlinLogging
import world.phantasmal.lib.fileFormats.quest.Episode
import world.phantasmal.web.externals.babylon.TransformNode
import world.phantasmal.web.questEditor.loading.AreaAssetLoader
import world.phantasmal.web.questEditor.models.AreaVariantModel

private val logger = KotlinLogging.logger {}

class AreaMeshManager(private val areaAssetLoader: AreaAssetLoader) {
    private var currentGeometry: TransformNode? = null

    suspend fun load(episode: Episode?, areaVariant: AreaVariantModel?) {
        currentGeometry?.setEnabled(false)

        if (episode == null || areaVariant == null) {
            return
        }

        try {
            val geom = areaAssetLoader.loadCollisionGeometry(episode, areaVariant)
            geom.setEnabled(true)
            currentGeometry = geom
        } catch (e: Exception) {
            logger.error(e) {
                "Couldn't load models for area ${areaVariant.area.id}, variant ${areaVariant.id}."
            }
        }
    }
}
