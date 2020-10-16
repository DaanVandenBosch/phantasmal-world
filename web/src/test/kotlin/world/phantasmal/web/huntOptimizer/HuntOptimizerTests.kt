package world.phantasmal.web.huntOptimizer

import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import kotlinx.coroutines.cancel
import world.phantasmal.core.disposable.disposable
import world.phantasmal.testUtils.TestSuite
import world.phantasmal.web.core.HttpAssetLoader
import world.phantasmal.web.core.stores.PwTool
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.test.TestApplicationUrl
import kotlin.test.Test

class HuntOptimizerTests : TestSuite() {
    @Test
    fun initialization_and_shutdown_should_succeed_without_throwing() {
        val httpClient = HttpClient {
            install(JsonFeature) {
                serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
                    ignoreUnknownKeys = true
                })
            }
        }
        scope.disposable { httpClient.cancel() }

        HuntOptimizer(
            scope,
            assetLoader = HttpAssetLoader(httpClient, basePath = ""),
            uiStore = UiStore(scope, TestApplicationUrl("/${PwTool.HuntOptimizer}"))
        )
    }
}
