package world.phantasmal.web.questEditor

import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import kotlinx.coroutines.cancel
import world.phantasmal.core.disposable.disposable
import world.phantasmal.testUtils.TestSuite
import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.web.externals.babylon.Engine
import kotlin.test.Test

class QuestEditorTests : TestSuite() {
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

        disposer.add(
            QuestEditor(
                scope,
                AssetLoader(basePath = "", httpClient),
                createEngine = { Engine(it) }
            )
        )
    }
}
