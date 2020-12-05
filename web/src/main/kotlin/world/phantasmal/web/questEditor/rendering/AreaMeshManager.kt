package world.phantasmal.web.questEditor.rendering

import kotlinx.coroutines.CancellationException
import mu.KotlinLogging
import world.phantasmal.lib.fileFormats.quest.Episode
import world.phantasmal.web.questEditor.loading.AreaAssetLoader
import world.phantasmal.web.questEditor.models.AreaVariantModel

private val logger = KotlinLogging.logger {}

class AreaMeshManager(
    private val renderContext: QuestRenderContext,
    private val areaAssetLoader: AreaAssetLoader,
) {
    suspend fun load(episode: Episode?, areaVariant: AreaVariantModel?) {
        renderContext.clearCollisionGeometry()

        if (episode == null || areaVariant == null) {
            return
        }

        try {
            renderContext.collisionGeometry =
                areaAssetLoader.loadCollisionGeometry(episode, areaVariant)
        } catch (e: CancellationException) {
            // Do nothing.
        } catch (e: Exception) {
            logger.error(e) {
                "Couldn't load models for area ${areaVariant.area.id}, variant ${areaVariant.id}."
            }
        }
    }
}
