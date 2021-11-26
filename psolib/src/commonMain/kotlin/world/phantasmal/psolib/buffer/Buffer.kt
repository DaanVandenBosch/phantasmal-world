package world.phantasmal.psolib.buffer

expect class Buffer() {
    val size: Int

    fun copy(size: Int = this.size)
}
