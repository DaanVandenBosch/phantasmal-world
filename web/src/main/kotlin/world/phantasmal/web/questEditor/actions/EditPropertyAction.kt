package world.phantasmal.web.questEditor.actions

import world.phantasmal.web.core.actions.Action

class EditPropertyAction<T>(
    override val description: String,
    private val setter: (T) -> Unit,
    private val newValue: T,
    private val oldValue: T,
) : Action {
    override fun execute() {
        setter(newValue)
    }

    override fun undo() {
        setter(oldValue)
    }
}
