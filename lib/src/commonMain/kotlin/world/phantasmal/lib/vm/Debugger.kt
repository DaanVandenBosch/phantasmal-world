package world.phantasmal.lib.vm

/**
 * Attaches to [vm] as its [ExecutionInterceptor].
 */
class Debugger(private val vm: VirtualMachine) : ExecutionInterceptor {
    init {
        vm.executionInterceptor = this
    }

    override fun intercept(instruction: InstructionReference): Boolean {
        TODO()
    }
}
