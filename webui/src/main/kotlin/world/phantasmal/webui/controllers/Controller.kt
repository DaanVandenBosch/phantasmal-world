package world.phantasmal.webui.controllers

import kotlinx.coroutines.CoroutineScope
import world.phantasmal.webui.DisposableContainer

abstract class Controller(protected val scope: CoroutineScope) :
    DisposableContainer(),
    CoroutineScope by scope
