package world.phantasmal.webui.controllers

import world.phantasmal.core.disposable.Scope
import world.phantasmal.core.disposable.TrackedDisposable

abstract class Controller(protected val scope: Scope) : TrackedDisposable(scope.scope())
