package world.phantasmal.testUtils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import world.phantasmal.core.disposable.Disposer

open class TestContext(val disposer: Disposer) {
    val scope: CoroutineScope = object : CoroutineScope {
        override val coroutineContext = Job()
    }
}
