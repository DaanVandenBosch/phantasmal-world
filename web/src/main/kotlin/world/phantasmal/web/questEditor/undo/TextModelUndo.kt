package world.phantasmal.web.questEditor.undo

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.TrackedDisposable
import world.phantasmal.observable.ChangeEvent
import world.phantasmal.observable.Observable
import world.phantasmal.observable.cell.*
import world.phantasmal.observable.emitter
import world.phantasmal.observable.mutateDeferred
import world.phantasmal.web.core.commands.Command
import world.phantasmal.web.core.undo.Undo
import world.phantasmal.web.core.undo.UndoManager
import world.phantasmal.web.externals.monacoEditor.IDisposable
import world.phantasmal.web.externals.monacoEditor.ITextModel

class TextModelUndo(
    undoManager: UndoManager,
    private val description: String,
    model: Cell<ITextModel?>,
) : Undo, TrackedDisposable() {
    private val command = object : Command {
        override val description: String get() = this@TextModelUndo.description

        override fun execute() {
            _didRedo.emit(ChangeEvent(Unit))
        }

        override fun undo() {
            _didUndo.emit(ChangeEvent(Unit))
        }
    }

    private val modelObserver: Disposable
    private var modelChangeObserver: IDisposable? = null

    private val _canUndo: MutableCell<Boolean> = mutableCell(false)
    private val _canRedo: MutableCell<Boolean> = mutableCell(false)
    private val _didUndo = emitter<Unit>()
    private val _didRedo = emitter<Unit>()

    private val currentVersionId = mutableCell<Int?>(null)
    private val savePointVersionId = mutableCell<Int?>(null)

    override val canUndo: Cell<Boolean> = _canUndo
    override val canRedo: Cell<Boolean> = _canRedo

    override val firstUndo: Cell<Command?> = canUndo.map { if (it) command else null }
    override val firstRedo: Cell<Command?> = canRedo.map { if (it) command else null }

    override val atSavePoint: Cell<Boolean> = savePointVersionId eq currentVersionId

    val didUndo: Observable<Unit> = _didUndo
    val didRedo: Observable<Unit> = _didRedo

    init {
        undoManager.addUndo(this)
        modelObserver = model.observeNow(::onModelChange)
    }

    override fun dispose() {
        modelChangeObserver?.dispose()
        modelObserver.dispose()
        super.dispose()
    }

    private fun onModelChange(model: ITextModel?) {
        mutateDeferred {
            if (disposed) return@mutateDeferred

            modelChangeObserver?.dispose()

            if (model == null) {
                reset()
                return@mutateDeferred
            }

            _canUndo.value = false
            _canRedo.value = false

            val initialVersionId = model.getAlternativeVersionId()
            currentVersionId.value = initialVersionId
            savePointVersionId.value = initialVersionId
            var lastVersionId = initialVersionId

            modelChangeObserver = model.onDidChangeContent {
                val versionId = model.getAlternativeVersionId()
                val prevVersionId = currentVersionId.value!!

                if (versionId < prevVersionId) {
                    // Undoing.
                    _canRedo.value = true

                    if (versionId == initialVersionId) {
                        _canUndo.value = false
                    }
                } else {
                    if (versionId <= lastVersionId) {
                        // Redoing.
                        if (versionId == lastVersionId) {
                            _canRedo.value = false
                        }
                    } else {
                        _canRedo.value = false

                        if (prevVersionId > lastVersionId) {
                            lastVersionId = prevVersionId
                        }
                    }

                    _canUndo.value = true
                }

                currentVersionId.value = versionId
            }
        }
    }

    override fun undo(): Boolean =
        if (canUndo.value) {
            command.undo()
            true
        } else {
            false
        }

    override fun redo(): Boolean =
        if (canRedo.value) {
            command.execute()
            true
        } else {
            false
        }

    override fun savePoint() {
        savePointVersionId.value = currentVersionId.value
    }

    override fun reset() {
        _canUndo.value = false
        _canRedo.value = false
        currentVersionId.value = null
        savePointVersionId.value = null
    }
}
