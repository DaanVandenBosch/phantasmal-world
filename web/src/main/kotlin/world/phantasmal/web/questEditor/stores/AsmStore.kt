package world.phantasmal.web.questEditor.stores

import world.phantasmal.lib.assembly.disassemble
import world.phantasmal.observable.ChangeEvent
import world.phantasmal.observable.Observable
import world.phantasmal.observable.emitter
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.mutableVal
import world.phantasmal.observable.value.trueVal
import world.phantasmal.web.core.undo.SimpleUndo
import world.phantasmal.web.core.undo.UndoManager
import world.phantasmal.web.externals.monacoEditor.*
import world.phantasmal.web.questEditor.models.QuestModel
import world.phantasmal.webui.obj
import world.phantasmal.webui.stores.Store
import kotlin.js.RegExp

class AsmStore(
    questEditorStore: QuestEditorStore,
    private val undoManager: UndoManager,
) : Store() {
    private var _textModel = mutableVal<ITextModel?>(null)

    private val _didUndo = emitter<Unit>()
    private val _didRedo = emitter<Unit>()
    private val undo = SimpleUndo(
        undoManager,
        "Script edits",
        { _didUndo.emit(ChangeEvent(Unit)) },
        { _didRedo.emit(ChangeEvent(Unit)) },
    )

    val inlineStackArgs: Val<Boolean> = trueVal()

    val textModel: Val<ITextModel?> = _textModel

    val editingEnabled: Val<Boolean> = questEditorStore.questEditingEnabled

    val didUndo: Observable<Unit> = _didUndo
    val didRedo: Observable<Unit> = _didRedo

    init {
        observe(questEditorStore.currentQuest, inlineStackArgs) { quest, inlineArgs ->
            _textModel.value?.dispose()
            _textModel.value = quest?.let { createModel(quest, inlineArgs) }
        }
    }

    fun makeUndoCurrent() {
        undoManager.setCurrent(undo)
    }

    private fun createModel(quest: QuestModel, inlineArgs: Boolean): ITextModel {
        val assembly = disassemble(quest.bytecodeIr, inlineArgs)
        val model = createModel(assembly.joinToString("\n"), ASM_LANG_ID)
        addModelChangeListener(model)
        return model
    }

    /**
     * Sets up undo/redo, code analysis and breakpoint updates on model change.
     */
    private fun addModelChangeListener(model: ITextModel) {
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

            // TODO: Code analysis and breakpoint update.
        }
    }

    companion object {
        const val ASM_LANG_ID = "psoasm"

        init {
            register(obj { id = ASM_LANG_ID })

            setMonarchTokensProvider(ASM_LANG_ID, obj {
                defaultToken = "invalid"

                tokenizer = obj {
                    this["root"] = arrayOf(
                        // Strings.
                        obj {
                            // Unterminated string.
                            regex = RegExp('"' + """([^"\\]|\.)*$""")
                            action = obj { token = "string.invalid" }
                        },
                        obj {
                            regex = RegExp("\"")
                            action = obj {
                                token = "string.quote"
                                bracket = "@open"
                                next = "@string"
                            }
                        },

                        // Registers.
                        obj {
                            regex = RegExp("""r\d+""")
                            action = obj { token = "predefined" }
                        },

                        // Labels.
                        obj {
                            regex = RegExp("""[^\s]+:""")
                            action = obj { token = "tag" }
                        },

                        // Numbers.
                        obj {
                            regex = RegExp("""0x[0-9a-fA-F]+""")
                            action = obj { token = "number.hex" }
                        },
                        obj {
                            regex = RegExp("""-?\d+(\.\d+)?(e-?\d+)?""")
                            action = obj { token = "number.float" }
                        },
                        obj {
                            regex = RegExp("""-?[0-9]+""")
                            action = obj { token = "number" }
                        },

                        // Section markers.
                        obj {
                            regex = RegExp("""\.[^\s]+""")
                            action = obj { token = "keyword" }
                        },

                        // Identifiers.
                        obj {
                            regex = RegExp("""[a-z][a-z0-9_=<>!]*""")
                            action = obj { token = "identifier" }
                        },

                        // Whitespace.
                        obj {
                            regex = RegExp("""[ \t\r\n]+""")
                            action = obj { token = "white" }
                        },
//                        obj {
//                            regex = RegExp("""\/\*""")
//                            action = obj { token = "comment"; next = "@comment" }
//                        },
                        obj {
                            regex = RegExp("\\/\\/.*$")
                            action = obj { token = "comment" }
                        },

                        // Delimiters.
                        obj {
                            regex = RegExp(",")
                            action = obj { token = "delimiter" }
                        },
                    )

//                    this["comment"] = arrayOf(
//                        obj {
//                            regex = RegExp("""[^/*]+""")
//                            action = obj { token = "comment" }
//                        },
//                        obj {
//                            // Nested comment.
//                            regex = RegExp("""\/\*""")
//                            action = obj { token = "comment"; next = "@push" }
//                        },
//                        obj {
//                            // Nested comment end.
//                            regex = RegExp("""\*/""")
//                            action = obj { token = "comment"; next = "@pop" }
//                        },
//                        obj {
//                            regex = RegExp("""[/*]""")
//                            action = obj { token = "comment" }
//                        },
//                    )

                    this["string"] = arrayOf(
                        obj {
                            regex = RegExp("""[^\\"]+""")
                            action = obj { token = "string" }
                        },
                        obj {
                            regex = RegExp("""\\(?:[n\\"])""")
                            action = obj { token = "string.escape" }
                        },
                        obj {
                            regex = RegExp("""\\.""")
                            action = obj { token = "string.escape.invalid" }
                        },
                        obj {
                            regex = RegExp("\"")
                            action = obj {
                                token = "string.quote"
                                bracket = "@close"
                                next = "@pop"
                            }
                        },
                    )
                }
            })

            setLanguageConfiguration(ASM_LANG_ID, obj {
                indentationRules = obj<IndentationRule> {
                    increaseIndentPattern = RegExp("^\\s*\\d+:")
                    decreaseIndentPattern = RegExp("^\\s*(\\d+|\\.)")
                }
                autoClosingPairs = arrayOf(obj { open = "\""; close = "\"" })
                surroundingPairs = arrayOf(obj { open = "\""; close = "\"" })
                comments = obj<CommentRule> { lineComment = "//" }
            })
        }
    }
}
