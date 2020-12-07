package world.phantasmal.web.test

import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import kotlinx.coroutines.cancel
import kotlinx.datetime.Clock
import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.disposable
import world.phantasmal.testUtils.TestContext
import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.web.core.rendering.DisposableThreeRenderer
import world.phantasmal.web.core.stores.ApplicationUrl
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.core.undo.UndoManager
import world.phantasmal.web.externals.three.WebGLRenderer
import world.phantasmal.web.questEditor.loading.AreaAssetLoader
import world.phantasmal.web.questEditor.loading.QuestLoader
import world.phantasmal.web.questEditor.stores.AreaStore
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import kotlin.reflect.KProperty

/**
 * Assigning a disposable to any of the properties in this class will add the assigned value to
 * [ctx]'s disposer.
 */
class TestComponents(private val ctx: TestContext) {
    var httpClient: HttpClient by default {
        HttpClient {
            install(JsonFeature) {
                serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
                    ignoreUnknownKeys = true
                })
            }
        }.also {
            ctx.disposer.add(disposable { it.cancel() })
        }
    }

    var clock: Clock by default { StubClock() }

    var applicationUrl: ApplicationUrl by default { TestApplicationUrl("") }

    // Asset Loaders

    var assetLoader: AssetLoader by default { AssetLoader(httpClient, basePath = "/assets") }

    var areaAssetLoader: AreaAssetLoader by default {
        AreaAssetLoader(assetLoader)
    }

    var questLoader: QuestLoader by default { QuestLoader(assetLoader) }

    // Undo

    var undoManager: UndoManager by default { UndoManager() }

    // Stores

    var uiStore: UiStore by default { UiStore(applicationUrl) }

    var areaStore: AreaStore by default { AreaStore(areaAssetLoader) }

    var questEditorStore: QuestEditorStore by default {
        QuestEditorStore(uiStore, areaStore, undoManager)
    }

    // Rendering
    var createThreeRenderer: (HTMLCanvasElement) -> DisposableThreeRenderer by default {
        { canvas ->
            object : DisposableThreeRenderer {
                override val renderer = NopRenderer(canvas).unsafeCast<WebGLRenderer>()
                override fun dispose() {}
            }
        }
    }

    private fun <T> default(defaultValue: () -> T) = LazyDefault {
        val value = defaultValue()

        if (value is Disposable) {
            ctx.disposer.add(value)
        }

        value
    }

    private inner class LazyDefault<T>(private val defaultValue: () -> T) {
        private var initialized = false
        private var value: T? = null

        operator fun getValue(thisRef: Any?, prop: KProperty<*>): T {
            if (!initialized) {
                val value = defaultValue()

                if (value is Disposable) {
                    ctx.disposer.add(value)
                }

                this.value = value
                initialized = true
            }

            return value.unsafeCast<T>()
        }

        operator fun setValue(thisRef: Any?, prop: KProperty<*>, value: T) {
            require(!initialized) {
                "Property ${prop.name} is already initialized."
            }

            if (value is Disposable) {
                ctx.disposer.add(value)
            }

            this.value = value
            initialized = true
        }
    }
}
