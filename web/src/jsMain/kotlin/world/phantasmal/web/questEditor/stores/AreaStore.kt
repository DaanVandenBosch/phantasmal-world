package world.phantasmal.web.questEditor.stores

import world.phantasmal.psolib.Episode
import world.phantasmal.web.questEditor.loading.AreaAssetLoader
import world.phantasmal.web.questEditor.models.AreaModel
import world.phantasmal.web.questEditor.models.AreaVariantModel
import world.phantasmal.web.questEditor.models.SectionModel
import world.phantasmal.webui.stores.Store
import world.phantasmal.psolib.fileFormats.quest.getAreasForEpisode as getAreasForEpisodeLib

class AreaStore(private val areaAssetLoader: AreaAssetLoader) : Store() {
    private val areas: Map<Episode, List<AreaModel>> =
        Episode.entries.associateWith { episode ->
            getAreasForEpisodeLib(episode).map { area ->
                val variants = mutableListOf<AreaVariantModel>()
                val areaModel = AreaModel(area.id, area.name, area.bossArea, area.order, variants)

                area.areaVariants.forEach { variant ->
                    variants.add(AreaVariantModel(variant.id, areaModel, episode))
                }

                areaModel
            }
        }

    fun getAreasForEpisode(episode: Episode): List<AreaModel> =
        areas.getValue(episode)

    fun getArea(episode: Episode, areaId: Int): AreaModel? =
        areas.getValue(episode).find { it.id == areaId }

    fun getVariant(episode: Episode, areaId: Int, variantId: Int): AreaVariantModel? =
        getArea(episode, areaId)?.areaVariants?.getOrNull(variantId)

    suspend fun getSection(
        episode: Episode,
        variant: AreaVariantModel,
        sectionId: Int,
    ): SectionModel? =
        getSections(episode, variant).find { it.id == sectionId }

    suspend fun getSections(episode: Episode, variant: AreaVariantModel): List<SectionModel> =
        areaAssetLoader.loadSections(episode, variant)

    fun getLoadedSections(episode: Episode, variant: AreaVariantModel): List<SectionModel>? =
        areaAssetLoader.getCachedSections(episode, variant)
}
