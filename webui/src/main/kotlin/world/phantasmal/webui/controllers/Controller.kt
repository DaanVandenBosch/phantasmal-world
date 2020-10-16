package world.phantasmal.webui.controllers

import kotlinx.coroutines.CoroutineScope
import world.phantasmal.core.disposable.Scope
import world.phantasmal.core.disposable.TrackedDisposable

abstract class Controller(protected val scope: Scope) :
    TrackedDisposable(scope.scope()),
    CoroutineScope by scope
