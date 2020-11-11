package world.phantasmal.web.core.actions

interface Action {
    val description: String
    fun execute()
    fun undo()
}
