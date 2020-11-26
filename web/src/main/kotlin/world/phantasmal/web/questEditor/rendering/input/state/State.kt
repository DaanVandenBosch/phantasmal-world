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
     * The state object should stop doing what it's doing and revert to the idle state as soon as
     * possible.
     */
    abstract fun cancel()
}
