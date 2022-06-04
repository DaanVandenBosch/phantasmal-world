package world.phantasmal.web.questEditor.stores

import kotlinx.browser.window
import kotlinx.coroutines.launch
import world.phantasmal.core.Severity
import world.phantasmal.core.disposable.Disposer
import world.phantasmal.core.disposable.disposable
import world.phantasmal.cell.Cell
import world.phantasmal.cell.list.ListCell
import world.phantasmal.cell.mutableCell
import world.phantasmal.cell.mutateDeferred
import world.phantasmal.psolib.asm.assemble
import world.phantasmal.psolib.asm.disassemble
import world.phantasmal.web.core.observable.Observable
import world.phantasmal.web.core.undo.UndoManager
import world.phantasmal.web.externals.monacoEditor.*
import world.phantasmal.web.questEditor.asm.AsmAnalyser
import world.phantasmal.web.questEditor.asm.monaco.*
import world.phantasmal.web.questEditor.models.QuestModel
import world.phantasmal.web.questEditor.undo.TextModelUndo
import world.phantasmal.web.shared.messages.AsmChange
import world.phantasmal.web.shared.messages.AsmRange
import world.phantasmal.web.shared.messages.AssemblyProblem
import world.phantasmal.webui.obj
import world.phantasmal.webui.stores.Store

/**
 * Depends on a global [AsmAnalyser], instantiate at most once.
 */
class AsmStore(
    private val questEditorStore: QuestEditorStore,
    private val undoManager: UndoManager,
) : Store() {
    private val _inlineStackArgs = mutableCell(true)
    private var _textModel = mutableCell<ITextModel?>(null)

    private var setBytecodeIrTimeout: Int? = null

    /**
     * Contains all model-related disposables. All contained disposables are disposed whenever a new
     * model is created.
     */
    private val modelDisposer = addDisposable(Disposer())

    private val undo = addDisposable(TextModelUndo(undoManager, "Script edits", _textModel))

    val inlineStackArgs: Cell<Boolean> = _inlineStackArgs

    val textModel: Cell<ITextModel?> = _textModel

    val editingEnabled: Cell<Boolean> = questEditorStore.questEditingEnabled

    val didUndo: Observable<Unit> = undo.didUndo
    val didRedo: Observable<Unit> = undo.didRedo

    val problems: ListCell<AssemblyProblem> = asmAnalyser.problems

    init {
        observeNow(questEditorStore.currentQuest) { quest ->
            setTextModel(quest, inlineStackArgs.value)
        }

        observeNow(inlineStackArgs) { inlineStackArgs ->
            // Ensure we have the most up-to-date bytecode before we disassemble it again.
            if (setBytecodeIrTimeout != null) {
                setBytecodeIr()
            }

            setTextModel(questEditorStore.currentQuest.value, inlineStackArgs)
        }

        observe(asmAnalyser.mapDesignations) {
            scope.launch { questEditorStore.setMapDesignations(it) }
        }

        observeNow(problems) { problems ->
            textModel.value?.let { model ->
                val markers = Array<IMarkerData>(problems.size) {
                    val problem = problems[it]
                    obj {
                        severity = when (problem.severity) {
                            Severity.Trace, Severity.Debug -> MarkerSeverity.Hint
                            Severity.Info -> MarkerSeverity.Info
                            Severity.Warning -> MarkerSeverity.Warning
                            Severity.Error -> MarkerSeverity.Error
                        }
                        message = problem.message
                        startLineNumber = problem.lineNo
                        startColumn = problem.col
                        endLineNumber = problem.lineNo
                        endColumn = problem.col + problem.len

                        // Hack: because only one warning is generated at the moment, "Unnecessary
                        // section marker.", we can simply add the Unnecessary tag here.
                        if (problem.severity == Severity.Warning) {
                            tags = arrayOf(MarkerTag.Unnecessary)
                        }
                    }
                }
                // Not sure what the "owner" parameter is for.
                setModelMarkers(model, owner = ASM_LANG_ID, markers)
            }
        }
    }

    fun makeUndoCurrent() {
        undoManager.setCurrent(undo)
    }

    fun setInlineStackArgs(inline: Boolean) {
        _inlineStackArgs.value = inline
    }

    private fun setTextModel(quest: QuestModel?, inlineStackArgs: Boolean) {
        mutateDeferred {
            setBytecodeIrTimeout?.let { it ->
                window.clearTimeout(it)
                setBytecodeIrTimeout = null
            }

            modelDisposer.disposeAll()

            quest ?: return@mutateDeferred

            val asm = disassemble(quest.bytecodeIr, inlineStackArgs)
            asmAnalyser.setAsm(asm, inlineStackArgs)

            _textModel.value = createModel(asm.joinToString("\n"), ASM_LANG_ID).also { model ->
                modelDisposer.add(disposable { model.dispose() })

                model.onDidChangeContent { e ->
                    asmAnalyser.updateAsm(e.changes.map {
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

                    setBytecodeIrTimeout?.let(window::clearTimeout)
                    setBytecodeIrTimeout = window.setTimeout(::setBytecodeIr, 1000)

                    // TODO: Update breakpoints.
                }
            }
        }
    }

    private fun setBytecodeIr() {
        if (disposed) return

        setBytecodeIrTimeout = null

        val model = textModel.value ?: return
        val quest = questEditorStore.currentQuest.value ?: return

        assemble(model.getLinesContent().toList(), inlineStackArgs.value)
            .getOrNull()
            ?.let(quest::setBytecodeIr)
    }

    companion object {
        private val asmAnalyser = AsmAnalyser()

        const val ASM_LANG_ID = "psoasm"

        init {
            register(obj { id = ASM_LANG_ID })
            setMonarchTokensProvider(ASM_LANG_ID, AsmMonarchLanguage)
            setLanguageConfiguration(ASM_LANG_ID, AsmLanguageConfiguration)
            registerCompletionItemProvider(ASM_LANG_ID, AsmCompletionItemProvider(asmAnalyser))
            registerSignatureHelpProvider(ASM_LANG_ID, AsmSignatureHelpProvider(asmAnalyser))
            registerHoverProvider(ASM_LANG_ID, AsmHoverProvider(asmAnalyser))
            registerDefinitionProvider(ASM_LANG_ID, AsmDefinitionProvider(asmAnalyser))
            registerDocumentSymbolProvider(ASM_LANG_ID, AsmDocumentSymbolProvider(asmAnalyser))
            registerDocumentHighlightProvider(
                ASM_LANG_ID,
                AsmDocumentHighlightProvider(asmAnalyser)
            )
            // TODO: Add semantic highlighting with registerDocumentSemanticTokensProvider (or
            //  registerDocumentRangeSemanticTokensProvider?).
            //  Enable when calling editor.create with 'semanticHighlighting.enabled': true.
            //  See: https://github.com/microsoft/monaco-editor/issues/1833#issuecomment-588108427
        }
    }
}
