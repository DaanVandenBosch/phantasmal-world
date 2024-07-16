package world.phantasmal.web.core.rendering

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.web.externals.three.WebGLRenderer

interface DisposableThreeRenderer : Disposable {
    val renderer: WebGLRenderer
}
