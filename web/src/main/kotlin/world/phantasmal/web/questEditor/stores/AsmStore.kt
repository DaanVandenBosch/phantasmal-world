package world.phantasmal.web.questEditor.stores

import kotlinx.coroutines.launch
import world.phantasmal.core.disposable.Disposer
import world.phantasmal.core.disposable.disposable
import world.phantasmal.lib.asm.AssemblyProblem
import world.phantasmal.lib.asm.disassemble
import world.phantasmal.observable.ChangeEvent
import world.phantasmal.observable.Observable
import world.phantasmal.observable.emitter
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.list.ListVal
import world.phantasmal.observable.value.mutableVal
import world.phantasmal.web.core.undo.SimpleUndo
import world.phantasmal.web.core.undo.UndoManager
import world.phantasmal.web.externals.monacoEditor.*
import world.phantasmal.web.questEditor.asm.*
import world.phantasmal.webui.obj
import world.phantasmal.webui.stores.Store

class AsmStore(
    questEditorStore: QuestEditorStore,
    private val undoManager: UndoManager,
) : Store() {
    private val _inlineStackArgs = mutableVal(true)
    private var _textModel = mutableVal<ITextModel?>(null)

    /**
     * Contains all model-related disposables. All contained disposables are disposed whenever a new
     * model is created.
     */
    private val modelDisposer = addDisposable(Disposer())

    private val _didUndo = emitter<Unit>()
    private val _didRedo = emitter<Unit>()
    private val undo = SimpleUndo(
        undoManager,
        "Script edits",
        { _didUndo.emit(ChangeEvent(Unit)) },
        { _didRedo.emit(ChangeEvent(Unit)) },
    )

    val inlineStackArgs: Val<Boolean> = _inlineStackArgs

    val textModel: Val<ITextModel?> = _textModel

    val editingEnabled: Val<Boolean> = questEditorStore.questEditingEnabled

    val didUndo: Observable<Unit> = _didUndo
    val didRedo: Observable<Unit> = _didRedo

    val problems: ListVal<AssemblyProblem> = AsmAnalyser.problems

    init {
        observe(questEditorStore.currentQuest, inlineStackArgs) { quest, inlineStackArgs ->
            modelDisposer.disposeAll()

            quest?.let {
                val asm = disassemble(quest.bytecodeIr, inlineStackArgs)
                scope.launch { AsmAnalyser.setAsm(asm, inlineStackArgs) }

                _textModel.value = createModel(asm.joinToString("\n"), ASM_LANG_ID).also { model ->
                    modelDisposer.add(disposable { model.dispose() })

                    setupUndoRedo(model)

                    model.onDidChangeContent { e ->
                        scope.launch {
                            AsmAnalyser.updateAsm(e.changes.map {
                                AsmChange(
                                    AsmRange(
                                        it.range.startLineNumber,
                                        it.range.startColumn,
                                        it.range.endLineNumber,
                                        it.range.endColumn,
                                    ),
                                    it.text,
                                )
                            })
                        }
                        // TODO: Update breakpoints.
                    }
                }
            }
        }

        observe(AsmAnalyser.bytecodeIr) {
            questEditorStore.currentQuest.value?.setBytecodeIr(it)
        }

        observe(AsmAnalyser.mapDesignations) {
            questEditorStore.currentQuest.value?.setMapDesignations(it)
        }
    }

    fun makeUndoCurrent() {
        undoManager.setCurrent(undo)
    }

    fun setInlineStackArgs(inline: Boolean) {
        _inlineStackArgs.value = inline
    }

    private fun setupUndoRedo(model: ITextModel) {
        val initialVersion = model.getAlternativeVersionId()
        var currentVersion = initialVersion
        var lastVersion = initialVersion

        model.onDidChangeContent {
            val version = model.getAlternativeVersionId()

            if (version < currentVersion) {
                // Undoing.
                undo.canRedo.value = true

                if (version == initialVersion) {
                    undo.canUndo.value = false
                }
            } else {
                // Redoing.
                if (version <= lastVersion) {
                    if (version == lastVersion) {
                        undo.canRedo.value = false
                    }
                } else {
                    undo.canRedo.value = false

                    if (currentVersion > lastVersion) {
                        lastVersion = currentVersion
                    }
                }

                undo.canUndo.value = true
            }

            currentVersion = version
        }
    }

    companion object {
        const val ASM_LANG_ID = "psoasm"

        init {
            register(obj { id = ASM_LANG_ID })
            setMonarchTokensProvider(ASM_LANG_ID, AsmMonarchLanguage)
            setLanguageConfiguration(ASM_LANG_ID, AsmLanguageConfiguration)
            registerCompletionItemProvider(ASM_LANG_ID, AsmCompletionItemProvider)
            registerSignatureHelpProvider(ASM_LANG_ID, AsmSignatureHelpProvider)
            registerHoverProvider(ASM_LANG_ID, AsmHoverProvider)
            registerDefinitionProvider(ASM_LANG_ID, AsmDefinitionProvider)
        }
    }
}
