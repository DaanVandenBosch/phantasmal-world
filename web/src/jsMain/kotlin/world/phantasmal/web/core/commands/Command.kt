package world.phantasmal.web.core.commands

interface Command {
    val description: String
    fun execute()
    fun undo()
}
