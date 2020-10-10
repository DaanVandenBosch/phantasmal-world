package world.phantasmal.observable

interface Emitter<T> : Observable<T> {
    fun emit(event: ChangeEvent<T>)
}
