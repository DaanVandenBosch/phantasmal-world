package world.phantasmal.web.questEditor.rendering

import mu.KotlinLogging
import world.phantasmal.lib.fileFormats.quest.Episode
import world.phantasmal.web.questEditor.loading.AreaAssetLoader
import world.phantasmal.web.questEditor.models.AreaVariantModel

private val logger = KotlinLogging.logger {}

class AreaMeshManager(
    private val renderer: QuestRenderer,
    private val areaAssetLoader: AreaAssetLoader,
) {
    suspend fun load(episode: Episode?, areaVariant: AreaVariantModel?) {
        renderer.clearCollisionGeometry()

        if (episode == null || areaVariant == null) {
            return
        }

        try {
            renderer.collisionGeometry = areaAssetLoader.loadCollisionGeometry(episode, areaVariant)
        } catch (e: Exception) {
            logger.error(e) {
                "Couldn't load models for area ${areaVariant.area.id}, variant ${areaVariant.id}."
            }
        }
    }
}
