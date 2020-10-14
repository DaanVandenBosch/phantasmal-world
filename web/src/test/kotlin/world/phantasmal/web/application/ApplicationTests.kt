package world.phantasmal.web.application

import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import kotlinx.browser.document
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import world.phantasmal.core.disposable.DisposableScope
import world.phantasmal.core.disposable.disposable
import world.phantasmal.testUtils.TestSuite
import world.phantasmal.web.core.HttpAssetLoader
import world.phantasmal.web.core.UiDispatcher
import world.phantasmal.web.core.stores.PwTool
import world.phantasmal.web.externals.Engine
import world.phantasmal.web.test.TestApplicationUrl
import kotlin.test.Test

class ApplicationTests : TestSuite() {
    @Test
    fun initialization_and_shutdown_should_succeed_without_throwing() {
        (listOf(null) + PwTool.values().toList()).forEach { tool ->
            val scope = DisposableScope()

            try {
                val httpClient = HttpClient {
                    install(JsonFeature) {
                        serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
                            ignoreUnknownKeys = true
                        })
                    }
                }
                scope.disposable { httpClient.cancel() }

                Application(
                    scope,
                    crScope = CoroutineScope(UiDispatcher),
                    rootElement = document.body!!,
                    assetLoader = HttpAssetLoader(httpClient, basePath = ""),
                    applicationUrl = TestApplicationUrl(if (tool == null) "" else "/${tool.slug}"),
                    createEngine = { Engine(it) }
                )
            } finally {
                scope.dispose()
            }
        }
    }
}
