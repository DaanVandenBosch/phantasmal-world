package world.phantasmal.web.questEditor.widgets

import kotlinx.browser.document
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.Node
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import world.phantasmal.core.disposable.disposable
import world.phantasmal.cell.mutateDeferred
import world.phantasmal.psolib.Endianness
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.compression.prs.prsCompress
import world.phantasmal.psolib.cursor.cursor
import world.phantasmal.web.externals.monacoEditor.*
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import world.phantasmal.web.questEditor.asm.DataLabelType
import world.phantasmal.web.questEditor.asm.SymbolChatTriggerInfo
import world.phantasmal.web.questEditor.asm.SymbolChatTriggerStage
import world.phantasmal.web.questEditor.asm.monaco.EditorHistory
import world.phantasmal.web.questEditor.controllers.AsmEditorController
import world.phantasmal.web.questEditor.controllers.DataEditorController
import world.phantasmal.web.questEditor.controllers.DataLabelEntry
import world.phantasmal.web.questEditor.loading.SymbolChatColliRepository
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.obj
import world.phantasmal.webui.widgets.Widget

private val logger = KotlinLogging.logger {}

class AsmEditorWidget(
    private val ctrl: AsmEditorController,
    private val dataEditorCtrl: DataEditorController,
    private val asmWidget: AsmWidget,
    private val symbolChatColliRepository: SymbolChatColliRepository,
) : Widget() {
    // State for the inline SC preview. We keep at most one Monaco view
    // zone open — the one corresponding to whichever
    // `set_symbol_chat_collision` line the cursor is currently on.
    //
    // We resolve cursor-line → trigger by scanning Monaco text for the
    // opcode mnemonic and matching by ordinal position instead of reading
    // the instruction's srcLoc. srcLoc is only populated for bytecode that
    // came from assembling source text; bytecode parsed directly from a
    // .qst binary has no source locations, and that's where the user loads
    // most of their quests from.
    private var activeScTriggerZoneId: String? = null
    private var activeScTriggerLine: Int = -1
    private var triggers: List<SymbolChatTriggerInfo> = emptyList()
    private lateinit var editor: IStandaloneCodeEditor

    override fun Node.createElement() =
        div {
            className = "pw-quest-editor-asm-editor"

            editor = create(this, obj {
                theme = "phantasmal-world"
                scrollBeyondLastLine = false
                autoIndent = "full"
                fontSize = 13
                wordWrap = "on"
                wrappingIndent = "indent"
                renderIndentGuides = false
                folding = false
                wordBasedSuggestions = false
                occurrencesHighlight = true
                fixedOverflowWidgets = true
            })

            addDisposable(disposable { editor.dispose() })

            observeNow(ctrl.textModel) { editor.setModel(it) }

            observeNow(ctrl.readOnly) { editor.updateOptions(obj { readOnly = it }) }

            addDisposable(size.observeChange { (size) ->
                if (size.width > .0 && size.height > .0) {
                    editor.layout(obj {
                        width = size.width
                        height = size.height
                    })
                }
            })

            // Add VSCode keybinding for command palette.
            val quickCommand = editor.getAction("editor.action.quickCommand")

            editor.addAction(object : IActionDescriptor {
                override var id = "editor.action.quickCommand"
                override var label = "Command Palette"
                override var keybindings =
                    arrayOf(KeyMod.CtrlCmd or KeyMod.Shift or KeyCode.KEY_P)

                override fun run(editor: ICodeEditor, vararg args: dynamic) {
                    quickCommand.run()
                }
            })

            // ---------- Dynamic "Edit <Type>..." context menu item ----------
            //
            // Monaco's `addAction` API does not support changing an action's
            // label after registration, nor does it expose a public hook to
            // manipulate the context menu right before it opens. To work
            // around this, we dispose and re-register the action every time
            // the cursor moves:
            //
            //   1. Dispose the previous action (if any).
            //   2. Look up the data label type at the new cursor line via
            //      `dataEditorCtrl.dataLabelAtLine()`.
            //   3. If a typed label is found, register a new action whose
            //      label text reflects the specific type (e.g. "Edit NPC
            //      Data...", "Edit Symbol Chat...").
            //   4. If no typed label is found (untyped data or code), skip
            //      registration — the menu item simply won't appear.
            //
            // The callback is lightweight (one label lookup + one dispose /
            // addAction call), so the per-cursor-move overhead is negligible.
            // We also re-run when `dataLabels` changes (e.g. user marks a
            // label via "Mark label as: ...") so the menu updates immediately.
            var editDataDisposable: dynamic = null

            fun refreshEditDataAction() {
                editDataDisposable?.dispose()
                editDataDisposable = null

                val lineNo = editor.getPosition()?.lineNumber?.toInt() ?: return
                val entry = dataEditorCtrl.dataLabelAtLine(lineNo) ?: return

                // Map each DataLabelType to a user-facing menu label.
                // ImageData is excluded — it has its own "Change image..." /
                // "Save image..." actions instead.
                val typeLabel = when (entry.type) {
                    DataLabelType.NpcData -> "Edit NPC Data..."
                    DataLabelType.PhysicalData -> "Edit Physical Data..."
                    DataLabelType.AttackData -> "Edit Attack Data..."
                    DataLabelType.ResistData -> "Edit Resist Data..."
                    DataLabelType.MovementData -> "Edit Movement Data..."
                    DataLabelType.ImageData -> return
                    DataLabelType.FloatData -> "Edit Float Data..."
                    DataLabelType.VectorData -> "Edit Vector Data..."
                    DataLabelType.SymbolChatData -> "Edit Symbol Chat..."
                    DataLabelType.SymbolChatHexData -> "Edit Symbol Chat..."
                }

                val descriptor = object : IActionDescriptor {
                    override var id = "pw.editData"
                    override var label = typeLabel
                    override var keybindings = emptyArray<Int>()

                    override fun run(editor: ICodeEditor, vararg args: dynamic) {
                        openDataDialog(entry)
                    }
                }
                descriptor.asDynamic().contextMenuGroupId = "pw-data"
                descriptor.asDynamic().contextMenuOrder = 1.0
                editDataDisposable = editor.addAction(descriptor)
            }

            // Refresh on cursor movement and when label types change.
            val cursorForMark = editor.asDynamic().onDidChangeCursorPosition {
                refreshEditDataAction()
            }
            addDisposable(disposable { cursorForMark.dispose() })
            observeNow(dataEditorCtrl.dataLabels) { refreshEditDataAction() }
            addDisposable(disposable { editDataDisposable?.dispose() })

            // "Change image..." — replace current image data segment's bytes
            // with the contents of a user-picked file. Only enabled for
            // segments referenced by call_image_data (gated below).
            val changeImageDescriptor = object : IActionDescriptor {
                override var id = "pw.changeImage"
                override var label = "Change image..."
                override var keybindings = emptyArray<Int>()

                override fun run(editor: ICodeEditor, vararg args: dynamic) {
                    val lineNo = editor.getPosition()?.lineNumber?.toInt() ?: return
                    // Only act on labels that are referenced by call_image_data —
                    // PRS-compressing arbitrary data segments would corrupt them.
                    val entry = dataEditorCtrl.dataLabelAtLine(lineNo) ?: return
                    if (entry.type != DataLabelType.ImageData) {
                        logger.warn {
                            "Change image: label ${entry.labelId} is not an image segment (type=${entry.type})"
                        }
                        return
                    }
                    pickFileAndReplaceSegment(entry.labelId)
                }
            }
            changeImageDescriptor.asDynamic().contextMenuGroupId = "pw-data"
            changeImageDescriptor.asDynamic().contextMenuOrder = 2.0
            val changeImageAction = editor.addAction(changeImageDescriptor)
            addDisposable(disposable { changeImageAction.dispose() })

            // "Save image..." — dump current data segment's bytes to a file
            // download.
            val saveImageDescriptor = object : IActionDescriptor {
                override var id = "pw.saveImage"
                override var label = "Save image..."
                override var keybindings = emptyArray<Int>()

                override fun run(editor: ICodeEditor, vararg args: dynamic) {
                    val lineNo = editor.getPosition()?.lineNumber?.toInt() ?: return
                    val labelId = dataEditorCtrl.dataSegmentLabelAtLine(lineNo) ?: return
                    saveSegmentAsFile(labelId)
                }
            }
            saveImageDescriptor.asDynamic().contextMenuGroupId = "pw-data"
            saveImageDescriptor.asDynamic().contextMenuOrder = 3.0
            val saveImageAction = editor.addAction(saveImageDescriptor)
            addDisposable(disposable { saveImageAction.dispose() })

            // "Mark label as..." actions — manual override for label types
            // that have no opcode-based detection (Float, Symbol Chat) or to
            // force-override the auto-detected type.
            fun addMarkAction(actionId: String, label: String, order: Double, type: DataLabelType?) {
                val descriptor = object : IActionDescriptor {
                    override var id = actionId
                    override var label = label
                    override var keybindings = emptyArray<Int>()

                    override fun run(editor: ICodeEditor, vararg args: dynamic) {
                        val lineNo = editor.getPosition()?.lineNumber?.toInt() ?: return
                        val labelId = dataEditorCtrl.dataSegmentLabelAtLine(lineNo) ?: return
                        dataEditorCtrl.setLabelTypeOverride(labelId, type)
                    }
                }
                descriptor.asDynamic().contextMenuGroupId = "pw-data-mark"
                descriptor.asDynamic().contextMenuOrder = order
                val action = editor.addAction(descriptor)
                addDisposable(disposable { action.dispose() })
            }
            // "Delete data segment" — removes the label line + all hex lines
            // as a single edit, so the assembler stays consistent.
            val deleteSegmentDescriptor = object : IActionDescriptor {
                override var id = "pw.deleteSegment"
                override var label = "Delete data segment"
                override var keybindings = emptyArray<Int>()

                override fun run(editor: ICodeEditor, vararg args: dynamic) {
                    val lineNo = editor.getPosition()?.lineNumber?.toInt() ?: return
                    val labelId = dataEditorCtrl.dataSegmentLabelAtLine(lineNo) ?: return
                    dataEditorCtrl.deleteSegment(labelId)
                }
            }
            deleteSegmentDescriptor.asDynamic().contextMenuGroupId = "pw-data"
            deleteSegmentDescriptor.asDynamic().contextMenuOrder = 4.0
            val deleteSegmentAction = editor.addAction(deleteSegmentDescriptor)
            addDisposable(disposable { deleteSegmentAction.dispose() })

            // Vector / Image are auto-detected from opcode args, so they're not
            // listed here. Float and Symbol Chat have no opcode-based detection
            // and must be marked manually.
            addMarkAction("pw.markFloat",      "Mark label as: Float",       1.0, DataLabelType.FloatData)
            addMarkAction("pw.markSymbolChat", "Mark label as: Symbol Chat", 2.0, DataLabelType.SymbolChatData)
            addMarkAction("pw.markAuto",       "Mark label as: Auto-detect", 3.0, null)

            // Inline multi-stage SC preview for `set_symbol_chat_collision`.
            // Behaves as a click-to-show: one view zone is shown beneath the
            // instruction line the cursor is currently on (cursor moves on
            // click), and hidden when the cursor leaves that line. Hover is
            // left to Monaco's built-in opcode signature tooltip so the two
            // don't fight for the same gesture.
            observeNow(dataEditorCtrl.symbolChatTriggers) { newTriggers ->
                triggers = newTriggers
                refreshActiveScTriggerZone()
            }
            val cursorForScPreview = editor.asDynamic().onDidChangeCursorPosition {
                refreshActiveScTriggerZone()
            }
            addDisposable(disposable { cursorForScPreview.dispose() })
            addDisposable(disposable { clearActiveScTriggerZone() })

            // Undo/redo.
            addDisposable(ctrl.didUndo.observe {
                editor.focus()

                mutateDeferred {
                    if (!disposed) {
                        editor.trigger(
                            source = AsmEditorWidget::class.simpleName,
                            handlerId = "undo",
                            payload = undefined,
                        )
                    }
                }
            })

            addDisposable(ctrl.didRedo.observe {
                editor.focus()

                mutateDeferred {
                    if (!disposed) {
                        editor.trigger(
                            source = AsmEditorWidget::class.simpleName,
                            handlerId = "redo",
                            payload = undefined,
                        )
                    }
                }
            })

            editor.onDidFocusEditorWidget(ctrl::makeUndoCurrent)

            // Navigate to a label position when triggered from entity info panel.
            addDisposable(ctrl.goToLabel.observe { range ->
                logger.info { "goToLabel observer fired: line=${range.startLineNo}, col=${range.startCol}" }
                val pos: IPosition = obj { lineNumber = range.startLineNo; column = range.startCol }
                editor.setPosition(pos)
                editor.revealPositionInCenter(pos)
                editor.focus()
            })

            addDisposable(EditorHistory(editor))
        }

    private fun pickFileAndReplaceSegment(labelId: Int) {
        // If the file's first 3 bytes match a known raw-image magic
        // (PVR / GVR / GBI / XVR), PRS-compress the bytes and patch the
        // matching call_image_data size argument. Otherwise the bytes are
        // written verbatim and the size arg is left alone — always
        // compressing would silently corrupt already-PRS payloads.
        val input = document.createElement("input") as HTMLInputElement
        input.type = "file"
        input.style.display = "none"
        input.onchange = { _ ->
            val file = input.files?.item(0)
            if (file != null) {
                scope.launch {
                    val ab = file.asDynamic().arrayBuffer().unsafeCast<kotlin.js.Promise<ArrayBuffer>>().await()
                    val rawBuffer = Buffer.fromArrayBuffer(ab, Endianness.Little)
                    val isRawImage = looksLikeRawImage(rawBuffer)

                    if (isRawImage) {
                        val uncompressedSize = rawBuffer.size
                        val compressedBuffer = prsCompress(rawBuffer.cursor()).buffer()
                        if (!dataEditorCtrl.writeSegmentDataAndPatchImageSize(
                                labelId, compressedBuffer, patchSize = uncompressedSize)) {
                            logger.warn { "Failed to replace data segment for label $labelId" }
                        }
                    } else {
                        // Already-compressed or unknown format — write verbatim,
                        // do not touch the call_image_data size argument.
                        if (!dataEditorCtrl.writeSegmentData(labelId, rawBuffer)) {
                            logger.warn { "Failed to replace data segment for label $labelId" }
                        }
                    }
                }
            }
            input.remove()
        }
        document.body?.appendChild(input)
        input.click()
    }

    /**
     * Returns true if [buf] starts with a known uncompressed PSO image
     * container magic — `PVR`, `GVR`, `GBI` or `XVR` (3 ASCII bytes).
     *
     * TODO(image-preview): show an inline preview of image data segments
     * (Change/Save image flow). Current state per format:
     *   - XVR: psolib has [parseXvr] in `Texture.kt` and a GPU upload
     *     path in `XvrTextureConversion.kt` (used by NPC/enemy model
     *     textures). Reusing it here would render an HTMLCanvas thumb.
     *   - PVR / GVR / GBI: no parser at all. Needs pixel-format tables
     *     (RGB565 / ARGB1555 / ARGB4444 / palette4 / palette8 / YUV422),
     *     a Dreamcast twiddle/untwiddle (Z-order swizzle), VQ codebook
     *     decoding for compressed PVRs and DXT1 for GameCube GVRs.
     *     Reference: Puyo Tools (C#).
     * Currently we only sniff the magic byte to gate PRS compression.
     */
    private fun looksLikeRawImage(buf: Buffer): Boolean {
        if (buf.size < 3) return false
        val b0 = buf.getUByte(0).toInt()
        val b1 = buf.getUByte(1).toInt()
        val b2 = buf.getUByte(2).toInt()
        // 'P'=0x50 'V'=0x56 'R'=0x52 'G'=0x47 'B'=0x42 'I'=0x49 'X'=0x58
        if (b1 == 0x56 && b2 == 0x52 && (b0 == 0x50 || b0 == 0x47 || b0 == 0x58)) return true // PVR/GVR/XVR
        if (b0 == 0x47 && b1 == 0x42 && b2 == 0x49) return true // GBI
        return false
    }

    private fun saveSegmentAsFile(labelId: Int) {
        val buf = dataEditorCtrl.readSegmentData(labelId) ?: return
        val bytes = Uint8Array(buf.size)
        for (i in 0 until buf.size) bytes.asDynamic()[i] = buf.getUByte(i).toInt()
        val blob = Blob(arrayOf(bytes), BlobPropertyBag(type = "application/octet-stream"))
        val url = URL.createObjectURL(blob)
        val a = document.createElement("a") as HTMLAnchorElement
        a.href = url
        a.download = "${labelId}.bin"
        a.style.display = "none"
        document.body?.appendChild(a)
        a.click()
        a.remove()
        URL.revokeObjectURL(url)
    }

    private fun openDataDialog(entry: DataLabelEntry) {
        asmWidget.initialLabelId.value = entry.labelId
        when (entry.type) {
            DataLabelType.NpcData -> asmWidget.npcDataDialogVisible.value = true
            DataLabelType.PhysicalData -> asmWidget.physicalDataDialogVisible.value = true
            DataLabelType.AttackData -> asmWidget.attackDataDialogVisible.value = true
            DataLabelType.ResistData -> asmWidget.resistDataDialogVisible.value = true
            DataLabelType.MovementData -> asmWidget.movementDataDialogVisible.value = true
            // Image labels are opaque binary blobs — there is no structured
            // editor for them. The user uses "Change image..." / "Save image..."
            // instead, which work on the raw bytes.
            DataLabelType.ImageData -> Unit
            DataLabelType.FloatData -> asmWidget.floatDataDialogVisible.value = true
            DataLabelType.VectorData -> asmWidget.vectorDataDialogVisible.value = true
            DataLabelType.SymbolChatData -> asmWidget.symbolChatDialogVisible.value = true
            DataLabelType.SymbolChatHexData -> asmWidget.symbolChatHexDialogVisible.value = true
        }
    }

    override fun focus() {
        editor.focus()
    }

    /**
     * Reconciles the single inline view zone with the current cursor line:
     * - If the cursor is on a `set_symbol_chat_collision` line (and that
     *   opcode has a corresponding trigger in the analyzer output), tear
     *   down any existing zone and install a fresh one below that line.
     * - If the cursor is elsewhere and we have an active zone, remove it.
     * - If nothing needs to change, no-op.
     *
     * Called from both the trigger-list observer (the set of triggers may
     * have moved after reassembly) and the cursor-change listener (user
     * clicked a different line).
     */
    private fun refreshActiveScTriggerZone() {
        val cursorLine = editor.getPosition()?.lineNumber?.toInt() ?: -1
        val trigger = findTriggerAtLine(cursorLine)
        if (trigger == null) {
            // Cursor not on any trigger line — make sure no zone is showing.
            clearActiveScTriggerZone()
            return
        }
        if (activeScTriggerZoneId != null && activeScTriggerLine == cursorLine) {
            // Already showing for this line; nothing to do.
            return
        }
        // Swap: remove the old (if any), then add a fresh one for the
        // current trigger.
        editor.asDynamic().changeViewZones { accessor: dynamic ->
            activeScTriggerZoneId?.let { accessor.removeZone(it) }

            val stages = resolveTriggerStages(
                trigger,
                dataEditorCtrl::readSegmentData,
                symbolChatColliRepository,
            )
            val domNode = document.createElement("div") as HTMLElement
            domNode.className = "pw-sc-inline-preview"
            val canvas = document.createElement("canvas") as HTMLCanvasElement
            canvas.className = "pw-sc-inline-preview-canvas"
            domNode.appendChild(canvas)
            SymbolChatStageRenderer.paintStages(canvas, stages)
            wireTriggerCanvasNavigation(canvas, trigger, stages)

            val zone = obj<dynamic> {
                afterLineNumber = cursorLine
                heightInPx = INLINE_ZONE_HEIGHT
                this.domNode = domNode
                // Let mousedown through so click listeners on the canvas
                // still fire — Monaco's suppressMouseDown swallows them.
                suppressMouseDown = false
            }
            activeScTriggerZoneId = accessor.addZone(zone) as String
            activeScTriggerLine = cursorLine
        }
    }

    /**
     * Finds which [SymbolChatTriggerInfo] corresponds to the cursor being on
     * a given line by scanning the Monaco text model for the opcode
     * mnemonic and matching by ordinal order. The Nth `set_symbol_chat_collision`
     * line in the source maps to the Nth trigger returned by the analyzer
     * (both walk segments/instructions in the same order).
     *
     * Using the text model instead of `Instruction.srcLoc` lets this work
     * for bytecode loaded from `.qst` binaries, where source locations are
     * not populated.
     */
    private fun findTriggerAtLine(line: Int): SymbolChatTriggerInfo? {
        if (triggers.isEmpty() || line < 1) return null
        val model = ctrl.textModel.value ?: return null
        val lineCount = model.getLineCount().toInt()
        if (line > lineCount) return null

        val current = model.getLineContent(line).trimStart()
        if (!current.startsWith(TRIGGER_MNEMONIC)) return null

        var ordinal = 0
        for (i in 1 until line) {
            if (model.getLineContent(i).trimStart().startsWith(TRIGGER_MNEMONIC)) {
                ordinal++
            }
        }
        return triggers.getOrNull(ordinal)
    }

    /**
     * Click listener that jumps to the dlabel backing whichever strip the
     * user clicked on. Strips whose spec uses a built-in SC ID (no dlabel)
     * are no-ops — the preset data lives in the client's
     * `symbolchatcolli.prs`, not the script, so there's no label to
     * navigate to.
     */
    private fun wireTriggerCanvasNavigation(
        canvas: HTMLCanvasElement,
        trigger: SymbolChatTriggerInfo,
        stages: List<SymbolChatStageRenderer.Stage>,
    ) {
        if (stages.isEmpty()) return
        canvas.style.cursor = "pointer"
        canvas.addEventListener("click", { ev ->
            val clientX = ev.asDynamic().clientX.unsafeCast<Double>()
            val rect = canvas.getBoundingClientRect()
            if (rect.width <= 0) return@addEventListener
            // The canvas is scaled by CSS; map display-space x back to the
            // natural canvas-space x so integer strip indices line up.
            val canvasX = (clientX - rect.left) * (canvas.width / rect.width)
            val stripIdx = (canvasX / SymbolChatRenderer.CANVAS_WIDTH).toInt()
            val clickedStage = stages.getOrNull(stripIdx) ?: return@addEventListener
            val original = trigger.stages.firstOrNull { it.slot == clickedStage.slot }
                ?: return@addEventListener
            val labelId = original.dlabel ?: return@addEventListener
            ctrl.navigateToLabel(labelId)
        })
    }

    private fun clearActiveScTriggerZone() {
        val id = activeScTriggerZoneId ?: return
        editor.asDynamic().changeViewZones { accessor: dynamic ->
            accessor.removeZone(id)
        }
        activeScTriggerZoneId = null
        activeScTriggerLine = -1
    }

    companion object {
        /**
         * View-zone height in pixels for the inline `set_symbol_chat_collision`
         * preview. One strip is 80px tall at natural size but CSS scales it
         * down to ~44px plus padding.
         */
        private const val INLINE_ZONE_HEIGHT = 52

        /** Mnemonic we match on to detect trigger lines in the text model. */
        private const val TRIGGER_MNEMONIC = "set_symbol_chat_collision"

        init {
            defineTheme("phantasmal-world", obj {
                base = "vs-dark"
                inherit = true
                rules = arrayOf(
                    obj { token = ""; foreground = "E0E0E0"; background = "#181818" },
                    obj { token = "tag"; foreground = "99BBFF" },
                    obj { token = "keyword"; foreground = "D0A0FF"; fontStyle = "bold" },
                    obj { token = "predefined"; foreground = "BBFFBB" },
                    obj { token = "number"; foreground = "FFFFAA" },
                    obj { token = "number.hex"; foreground = "FFFFAA" },
                    obj { token = "string"; foreground = "88FFFF" },
                    obj { token = "string.escape"; foreground = "8888FF" },
                )
                colors = obj {
                    this["editor.background"] = "#181818"
                    this["editor.lineHighlightBackground"] = "#202020"
                }
            })

            @Suppress("CssUnusedSymbol")
            // language=css
            style(
                """
                .pw-quest-editor-asm-editor {
                    flex-grow: 1;
                }
                .pw-quest-editor-asm-editor .editor-widget {
                    z-index: 30;
                }
                .pw-sc-inline-preview {
                    display: flex;
                    align-items: center;
                    padding: 2px 8px;
                    background: rgba(40, 40, 40, 0.5);
                    border-left: 2px solid hsl(35, 95%, 50%);
                }
                .pw-sc-inline-preview-canvas {
                    image-rendering: pixelated;
                    height: 44px;
                    background: #181818;
                }
            """.trimIndent()
            )
        }
    }
}
