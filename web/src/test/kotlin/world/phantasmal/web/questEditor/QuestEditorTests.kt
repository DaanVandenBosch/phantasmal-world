package world.phantasmal.web.questEditor

import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import kotlinx.coroutines.cancel
import world.phantasmal.core.disposable.disposable
import world.phantasmal.testUtils.TestSuite
import world.phantasmal.web.core.stores.PwTool
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.externals.Engine
import world.phantasmal.web.test.TestApplicationUrl
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

        val uiStore = disposer.add(UiStore(scope, TestApplicationUrl("/${PwTool.QuestEditor}")))

        disposer.add(
            QuestEditor(
                scope,
                uiStore,
                createEngine = { Engine(it) }
            )
        )
    }
}
