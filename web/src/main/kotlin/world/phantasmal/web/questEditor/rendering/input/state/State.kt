package world.phantasmal.web.questEditor.rendering.input.state

import mu.KotlinLogging
import world.phantasmal.web.questEditor.rendering.input.Evt

private val logger = KotlinLogging.logger {}

abstract class State {
    init {
        logger.trace { "Transitioning to ${this::class.simpleName}." }
    }

    abstract fun processEvent(event: Evt): State

    abstract fun beforeRender()

    /**
     * When this method is called, the state object should stop doing what it's doing as soon as
     * possible.
     */
    abstract fun cancel()
}
