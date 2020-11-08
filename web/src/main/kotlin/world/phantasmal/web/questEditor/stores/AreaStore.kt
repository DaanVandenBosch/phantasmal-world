package world.phantasmal.web.questEditor.stores

import kotlinx.coroutines.CoroutineScope
import world.phantasmal.lib.fileFormats.quest.Episode
import world.phantasmal.web.questEditor.loading.AreaAssetLoader
import world.phantasmal.web.questEditor.models.AreaModel
import world.phantasmal.web.questEditor.models.AreaVariantModel
import world.phantasmal.web.questEditor.models.SectionModel
import world.phantasmal.webui.stores.Store
import world.phantasmal.lib.fileFormats.quest.getAreasForEpisode as getAreasForEpisodeLib

class AreaStore(
    scope: CoroutineScope,
    private val areaAssetLoader: AreaAssetLoader,
) : Store(scope) {
    private val areas: Map<Episode, List<AreaModel>>

    init {
        areas = Episode.values()
            .map { episode ->
                episode to getAreasForEpisodeLib(episode).map { area ->
                    val variants = mutableListOf<AreaVariantModel>()
                    val areaModel = AreaModel(area.id, area.name, area.order, variants)

                    area.areaVariants.forEach { variant ->
                        variants.add(AreaVariantModel(variant.id, areaModel))
                    }

                    areaModel
                }
            }
            .toMap()
    }

    fun getAreasForEpisode(episode: Episode): List<AreaModel> =
        areas.getValue(episode)

    fun getArea(episode: Episode, areaId: Int): AreaModel? =
        areas.getValue(episode).find { it.id == areaId }

    fun getVariant(episode: Episode, areaId: Int, variantId: Int): AreaVariantModel? =
        getArea(episode, areaId)?.areaVariants?.getOrNull(variantId)

    suspend fun getSections(episode: Episode, variant: AreaVariantModel): List<SectionModel> =
        areaAssetLoader.loadSections(episode, variant)
}
