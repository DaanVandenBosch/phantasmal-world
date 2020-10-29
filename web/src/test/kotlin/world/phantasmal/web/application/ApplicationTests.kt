package world.phantasmal.web.application

import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import kotlinx.browser.document
import kotlinx.coroutines.cancel
import world.phantasmal.core.disposable.Disposer
import world.phantasmal.core.disposable.disposable
import world.phantasmal.core.disposable.use
import world.phantasmal.testUtils.TestSuite
import world.phantasmal.web.core.HttpAssetLoader
import world.phantasmal.web.core.stores.PwTool
import world.phantasmal.web.externals.Engine
import world.phantasmal.web.test.TestApplicationUrl
import kotlin.test.Test

class ApplicationTests : TestSuite() {
    @Test
    fun initialization_and_shutdown_should_succeed_without_throwing() = test {
        (listOf(null) + PwTool.values().toList()).forEach { tool ->
            Disposer().use { disposer ->
                val httpClient = HttpClient {
                    install(JsonFeature) {
                        serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
                            ignoreUnknownKeys = true
                        })
                    }
                }
                disposer.add(disposable { httpClient.cancel() })

                val appUrl = TestApplicationUrl(if (tool == null) "" else "/${tool.slug}")

                disposer.add(
                    Application(
                        scope,
                        rootElement = document.body!!,
                        assetLoader = HttpAssetLoader(httpClient, basePath = ""),
                        applicationUrl = appUrl,
                        createEngine = { Engine(it) }
                    )
                )
            }
        }
    }
}
