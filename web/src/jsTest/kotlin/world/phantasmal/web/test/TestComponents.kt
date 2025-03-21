package world.phantasmal.web.test

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlinx.datetime.Clock
import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.core.disposable.Disposable
import world.phantasmal.testUtils.TestContext
import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.web.core.persistence.KeyValueStore
import world.phantasmal.web.core.persistence.MemoryKeyValueStore
import world.phantasmal.web.core.rendering.DisposableThreeRenderer
import world.phantasmal.web.core.stores.ApplicationUrl
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.core.undo.UndoManager
import world.phantasmal.web.externals.three.WebGLRenderer
import world.phantasmal.web.huntOptimizer.persistence.HuntMethodPersister
import world.phantasmal.web.huntOptimizer.stores.HuntMethodStore
import world.phantasmal.web.questEditor.loading.AreaAssetLoader
import world.phantasmal.web.questEditor.loading.QuestLoader
import world.phantasmal.web.questEditor.stores.AreaStore
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.web.viewer.loading.AnimationAssetLoader
import world.phantasmal.web.viewer.loading.CharacterClassAssetLoader
import world.phantasmal.web.viewer.stores.ViewerStore

/**
 * Assigning a disposable to any of the properties in this class will add the assigned value to
 * [ctx]'s disposer.
 */
class TestComponents(private val ctx: TestContext) {
    var clock: Clock by default { StubClock() }

    var applicationUrl: ApplicationUrl by default { TestApplicationUrl("") }

    // Asset Loaders

    var assetLoader: AssetLoader by default { AssetLoader(basePath = "/assets") }

    var characterClassAssetLoader: CharacterClassAssetLoader by default {
        CharacterClassAssetLoader(assetLoader)
    }

    var animationAssetLoader: AnimationAssetLoader by default {
        AnimationAssetLoader(assetLoader)
    }

    var areaAssetLoader: AreaAssetLoader by default {
        AreaAssetLoader(assetLoader)
    }

    var questLoader: QuestLoader by default { QuestLoader(assetLoader) }

    // Persistence

    var keyValueStore: KeyValueStore by default { MemoryKeyValueStore() }

    var huntMethodPersister: HuntMethodPersister by default { HuntMethodPersister(keyValueStore) }

    // Undo

    var undoManager: UndoManager by default { UndoManager() }

    // Stores

    var uiStore: UiStore by default { UiStore(applicationUrl) }

    var areaStore: AreaStore by default { AreaStore(areaAssetLoader) }

    var viewerStore: ViewerStore by default {
        ViewerStore(characterClassAssetLoader, animationAssetLoader, uiStore)
    }

    var questEditorStore: QuestEditorStore by default {
        QuestEditorStore(questLoader, uiStore, areaStore, undoManager, initializeNewQuest = false)
    }

    var huntMethodStore: HuntMethodStore by default {
        HuntMethodStore(uiStore, assetLoader, huntMethodPersister)
    }

    // Rendering
    var createThreeRenderer: (HTMLCanvasElement) -> DisposableThreeRenderer by default {
        {
            object : DisposableThreeRenderer {
                override val renderer = NopRenderer().unsafeCast<WebGLRenderer>()
                override fun dispose() {}
            }
        }
    }

    private fun <T> default(defaultValue: () -> T) = LazyDefault(defaultValue)

    private inner class LazyDefault<T>(
        private val defaultValue: () -> T,
    ) : ReadWriteProperty<Any?, T> {

        private var initialized = false
        private var value: T? = null

        override operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            if (!initialized) {
                setValue(defaultValue())
            }

            return value.unsafeCast<T>()
        }

        override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            require(!initialized) {
                "Property ${property.name} is already initialized."
            }

            setValue(value)
        }

        private fun setValue(value: T) {
            if (value is Disposable) {
                ctx.disposer.add(value)
            }

            this.value = value
            initialized = true
        }
    }
}
