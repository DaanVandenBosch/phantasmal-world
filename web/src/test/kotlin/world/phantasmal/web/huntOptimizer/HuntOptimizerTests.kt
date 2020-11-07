package world.phantasmal.web.huntOptimizer

import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import kotlinx.coroutines.cancel
import world.phantasmal.core.disposable.disposable
import world.phantasmal.testUtils.TestSuite
import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.web.core.PwTool
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.test.TestApplicationUrl
import kotlin.test.Test

class HuntOptimizerTests : TestSuite() {
    @Test
    fun initialization_and_shutdown_should_succeed_without_throwing() = test {
        val httpClient = HttpClient {
            install(JsonFeature) {
                serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
                    ignoreUnknownKeys = true
                })
            }
        }
        disposer.add(disposable { httpClient.cancel() })

        val uiStore = disposer.add(UiStore(scope, TestApplicationUrl("/${PwTool.HuntOptimizer}")))

        disposer.add(
            HuntOptimizer(
                scope,
                AssetLoader(basePath = "", httpClient),
                uiStore
            )
        )
    }
}
