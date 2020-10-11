package world.phantasmal.web.core

import io.ktor.client.*
import io.ktor.client.request.*
import world.phantasmal.web.core.dto.QuestDto
import world.phantasmal.web.core.models.Server

interface AssetLoader {
    suspend fun getQuests(server: Server): List<QuestDto>
}

class HttpAssetLoader(
    private val httpClient: HttpClient,
    private val basePath: String,
) : AssetLoader {
    override suspend fun getQuests(server: Server): List<QuestDto> =
        httpClient.get("$basePath/assets/quests.${server.slug}.json")
}
