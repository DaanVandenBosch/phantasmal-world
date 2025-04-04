package world.phantasmal.web.questEditor.rendering

import kotlinx.coroutines.CancellationException
import mu.KotlinLogging
import world.phantasmal.psolib.Episode
import world.phantasmal.web.questEditor.loading.AreaAssetLoader
import world.phantasmal.web.questEditor.models.AreaVariantModel

private val logger = KotlinLogging.logger {}

class AreaMeshManager(
    private val renderContext: QuestRenderContext,
    private val areaAssetLoader: AreaAssetLoader,
) {
    suspend fun load(episode: Episode?, areaVariant: AreaVariantModel?) {
        renderContext.clearCollisionGeometry()
        renderContext.clearRenderGeometry()

        if (episode == null || areaVariant == null) {
            return
        }

        try {
            renderContext.collisionGeometry =
                areaAssetLoader.loadCollisionGeometry(episode, areaVariant)
            renderContext.renderGeometry =
                areaAssetLoader.loadRenderGeometry(episode, areaVariant)
        } catch (e: CancellationException) {
            // Do nothing.
        } catch (e: Exception) {
            logger.error(e) {
                "Couldn't load models for area ${areaVariant.area.id}, variant ${areaVariant.id}."
            }
        }
    }
}
