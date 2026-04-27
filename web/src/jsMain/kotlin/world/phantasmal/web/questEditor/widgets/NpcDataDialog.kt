package world.phantasmal.web.questEditor.widgets

import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.Node
import world.phantasmal.cell.Cell
import world.phantasmal.cell.MutableCell
import world.phantasmal.cell.cell
import world.phantasmal.cell.map
import world.phantasmal.cell.mutableCell
import world.phantasmal.psolib.asm.NpcVisualConfig
import world.phantasmal.web.core.rendering.DisposableThreeRenderer
import world.phantasmal.web.questEditor.rendering.NpcPreviewRenderer
import world.phantasmal.web.viewer.loading.CharacterClassAssetLoader
import world.phantasmal.web.questEditor.asm.DataLabelType
import world.phantasmal.web.questEditor.controllers.DataEditorController
import world.phantasmal.web.questEditor.controllers.DataLabelEntry
import world.phantasmal.webui.dom.*
import world.phantasmal.webui.widgets.*

private val CLASS_NAMES = arrayOf(
    "HUmar", "HUnewearl", "HUcast", "RAmar", "RAcast", "RAcaseal",
    "FOmarl", "FOnewm", "FOnewearl", "HUcaseal", "FOmar", "RAmarl",
)

private val CAST_CLASSES = setOf(2, 4, 5, 9)

// extra_model 0-6 = specific NPC model names (when v2_flags != 0)
private val NPC_NAMES = arrayOf("GM", "Rico", "Sonic", "Knux", "Tails", "Flowen", "Elly")

/**
 * Default value written to [NpcVisualConfig.validationFlags] when switching to an NPC
 * model (extra_model != 0). The original game stores an 8-bit checksum here; 11 is the
 * value produced by qedit when it authors a fresh NPC record, so we mirror that so our
 * output round-trips byte-for-byte with qedit-generated quests.
 */
private const val NPC_MODEL_DEFAULT_FLAG = 11

//                                   HUmr HUnw HUcs RAm  RAcs RAcl FOml FOnm FOnw HUcl FOmr RAml
private val COSTUME_COUNT = intArrayOf(18,  18,   0,  18,   0,   0,  18,  18,  18,   0,  18,  18)
private val SKIN_COUNT    = intArrayOf( 4,   4,  25,   4,  25,  25,   4,   4,   4,  25,   4,   4)
private val FACE_COUNT    = intArrayOf( 5,   5,   0,   5,   0,   0,   5,   5,   5,   0,   5,   5)
private val HEAD_COUNT    = intArrayOf( 1,   1,   5,   1,   5,   5,   1,   1,   1,   5,   1,   1)
private val HAIR_COUNT    = intArrayOf(10,  10,   0,  10,   0,   0,  10,  10,  10,   0,  10,  10)

/**
 * Cycle [current] backward/forward inside `[min, max]`, wrapping at the edges.
 * If the range is empty (`max < min`), the value is left unchanged — the field is
 * considered unavailable for the current class and should also be disabled in the UI.
 */
private fun cycleValue(current: Int, min: Int, max: Int, backward: Boolean): Int {
    if (max < min) return current
    return if (backward) {
        if (current > min) current - 1 else max
    } else {
        if (current < max) current + 1 else min
    }
}

class NpcDataDialog(
    visible: Cell<Boolean>,
    private val ctrl: DataEditorController,
    onDismiss: () -> Unit,
    private val initialLabelId: Cell<Int?> = cell(null),
    private val charClassAssetLoader: CharacterClassAssetLoader? = null,
    private val createThreeRenderer: ((HTMLCanvasElement) -> DisposableThreeRenderer)? = null,
) : Dialog(
    visible = visible,
    title = cell("NPC Data"),
    description = cell(""),
    content = {},
    footer = {},
    onDismiss = onDismiss,
) {
    private val labels = ctrl.labelsOfType(DataLabelType.NpcData)
    private val selectedLabel = mutableCell<DataLabelEntry?>(null)

    private val name = mutableCell("")
    private val nameColor = mutableCell(0)
    private val charClass = mutableCell(0)
    private val sectionId = mutableCell(1)
    private val extraModel = mutableCell(0)
    private val costume = mutableCell(1)
    private val skin = mutableCell(1)
    private val face = mutableCell(1)
    private val head = mutableCell(1)
    private val hair = mutableCell(1)
    private val hairR = mutableCell(0)
    private val hairG = mutableCell(0)
    private val hairB = mutableCell(0)
    private val proportionX = mutableCell(0.0)
    private val proportionY = mutableCell(0.0)
    private val v2Flags = mutableCell(0) // 0 = NPC None (player char), != 0 = NPC model
    private var version = 0
    private var classFlags = 0
    private var previewRenderer: NpcPreviewRenderer? = null

    init {
        val hasPreview = charClassAssetLoader != null && createThreeRenderer != null

        val bodyElement = dialogElement.querySelector(".pw-dialog-body")
        bodyElement?.let { body ->
            body.innerHTML = ""

            if (hasPreview) {
                // Two-column layout: fields left, preview right.
                val wrapper = body.ownerDocument!!.createElement("div") as HTMLElement
                wrapper.className = "pw-npc-dialog-split"

                val left = body.ownerDocument!!.createElement("div") as HTMLElement
                left.className = "pw-npc-dialog-left"
                left.appendChild(addDisposable(NpcDataContent()).element)
                wrapper.appendChild(left)

                val right = body.ownerDocument!!.createElement("div") as HTMLElement
                right.className = "pw-npc-dialog-right"
                previewRenderer = NpcPreviewRenderer(
                    charClassAssetLoader!!,
                    createThreeRenderer!!,
                    charClassCell = charClass,
                    sectionIdCell = sectionId,
                    costumeCell = costume,
                    skinCell = skin,
                    faceCell = face,
                    headCell = head,
                    hairCell = hair,
                    v2FlagsCell = v2Flags,
                    extraModelCell = extraModel,
                )
                addDisposable(previewRenderer!!)
                right.appendChild(previewRenderer!!.canvas)
                wrapper.appendChild(right)

                // Start/stop rendering and set canvas size when dialog visibility changes.
                observeNow(visible) { vis ->
                    if (vis) {
                        previewRenderer!!.setSize(240, 380)
                        previewRenderer!!.startRendering()
                        previewRenderer!!.refresh()
                    } else {
                        previewRenderer!!.stopRendering()
                    }
                }

                body.appendChild(wrapper)
            } else {
                body.appendChild(addDisposable(NpcDataContent()).element)
            }
        }

        val footerElement = dialogElement.querySelector(".pw-dialog-footer")
        footerElement?.let { footer ->
            footer.innerHTML = ""
            footer.appendChild(addDisposable(Button(
                text = "OK",
                enabled = map(ctrl.enabled, selectedLabel) { e, s -> e && s != null },
                onClick = { save(); onDismiss() },
            )).element)
            footer.appendChild(addDisposable(
                Button(text = "Cancel", onClick = { onDismiss() })
            ).element)
        }

        dialogElement.style.width = if (hasPreview) "560px" else "300px"
        dialogElement.style.maxHeight = "520px"

        observeNow(visible) { vis ->
            if (vis) {
                val targetId = initialLabelId.value
                val entries = labels.value
                (if (targetId != null) entries.find { it.labelId == targetId }
                else entries.firstOrNull())?.let(::loadLabel)
            }
        }
    }

    private fun loadLabel(entry: DataLabelEntry) {
        selectedLabel.value = entry
        val buf = ctrl.readSegmentData(entry.labelId) ?: return
        if (buf.size < NpcVisualConfig.SIZE) return
        val data = NpcVisualConfig.readFrom(buf)
        name.value = data.name
        nameColor.value = data.nameColor.toInt()
        charClass.value = data.charClass.toInt()
        sectionId.value = data.sectionId.toInt() + 1
        extraModel.value = data.extraModel.toInt()
        costume.value = data.costume.toInt() + 1
        skin.value = data.skin.toInt() + 1
        face.value = data.face.toInt() + 1
        head.value = data.head.toInt() + 1
        hair.value = data.hair.toInt() + 1
        hairR.value = data.hairR.toInt()
        hairG.value = data.hairG.toInt()
        hairB.value = data.hairB.toInt()
        proportionX.value = data.proportionX.toDouble()
        proportionY.value = data.proportionY.toDouble()
        v2Flags.value = data.validationFlags.toInt()
        version = data.version.toInt()
        classFlags = data.classFlags.toInt()
        previewRenderer?.refresh()
    }

    private fun save() {
        val entry = selectedLabel.value ?: return
        val buf = ctrl.readSegmentData(entry.labelId) ?: return
        if (buf.size < NpcVisualConfig.SIZE) return
        NpcVisualConfig(
            name = name.value,
            nameColor = nameColor.value.toUInt(),
            extraModel = extraModel.value.toUByte(),
            sectionId = (sectionId.value - 1).coerceAtLeast(0).toUByte(),
            charClass = charClass.value.toUByte(),
            validationFlags = v2Flags.value.toUByte(),
            version = version.toUByte(),
            classFlags = classFlags.toUInt(),
            costume = (costume.value - 1).coerceAtLeast(0).toUShort(),
            skin = (skin.value - 1).coerceAtLeast(0).toUShort(),
            face = (face.value - 1).coerceAtLeast(0).toUShort(),
            head = (head.value - 1).coerceAtLeast(0).toUShort(),
            hair = (hair.value - 1).coerceAtLeast(0).toUShort(),
            hairR = hairR.value.toUShort(),
            hairG = hairG.value.toUShort(),
            hairB = hairB.value.toUShort(),
            proportionX = proportionX.value.toFloat(),
            proportionY = proportionY.value.toFloat(),
        ).writeTo(buf)
        ctrl.writeSegmentData(entry.labelId, buf)
    }

    private fun argbToHex(argb: Int): String {
        val r = (argb shr 16) and 0xFF; val g = (argb shr 8) and 0xFF; val b = argb and 0xFF
        return "#${r.hex2()}${g.hex2()}${b.hex2()}"
    }
    private fun hexToArgb(hex: String) = hex.removePrefix("#").toInt(16) or (0xFF shl 24)
    private fun rgb16ToHex(r: Int, g: Int, b: Int): String {
        fun c(v: Int) = (v * 255 / 65535.0).toInt().coerceIn(0, 255)
        return "#${c(r).hex2()}${c(g).hex2()}${c(b).hex2()}"
    }
    private fun hexToRgb16(hex: String): Triple<Int, Int, Int> {
        val v = hex.removePrefix("#").toInt(16)
        fun c(x: Int) = x * 65535 / 255
        return Triple(c((v shr 16) and 0xFF), c((v shr 8) and 0xFF), c(v and 0xFF))
    }
    private fun Int.hex2() = toString(16).padStart(2, '0')

    private inner class NpcDataContent : Widget() {
        override fun Node.createElement() =
            div {
                className = "pw-npc-editor"

                // ID
                div {
                    className = "pw-npc-field"
                    span { className = "pw-npc-btn-placeholder" }
                    span { className = "pw-npc-field-text"; textContent = "ID:" }
                    addChild(Select(
                        items = labels,
                        itemToString = { "${it.labelId}" },
                        selected = selectedLabel,
                        onSelect = ::loadLabel,
                    ))
                    span { className = "pw-npc-end-placeholder" }
                }

                // Name + Color (color between input and right placeholder)
                div {
                    className = "pw-npc-field"
                    span { className = "pw-npc-btn-placeholder" }
                    span { className = "pw-npc-field-text"; textContent = "Name:" }
                    addChild(TextInput(value = name,
                        onChange = { name.value = it }, maxLength = 16))
                    input {
                        type = "color"
                        className = "pw-npc-color"
                        observeNow(nameColor) { value = argbToHex(it) }
                        onchange = { nameColor.value = hexToArgb(value) }
                    }
                    span { className = "pw-npc-end-placeholder" }
                }

                // Class (always editable)
                navFieldFixed(charClass, CLASS_NAMES.size, false) {
                    CLASS_NAMES.getOrElse(it) { "?" }
                }

                // Section ID
                navFieldFixed(sectionId, 10, true) { "Section ID $it/10" }

                // NPC type: v2Flags==0 → "NPC None", v2Flags!=0 → NPC_NAMES[extra_model]
                div {
                    className = "pw-npc-field"
                    button { className = "pw-npc-btn"; textContent = "\u25C0"
                        onclick = {
                            if (v2Flags.value != 0) {
                                if (extraModel.value > 0) {
                                    extraModel.value--
                                } else {
                                    v2Flags.value = 0
                                }
                            } else {
                                // Wrap: NPC None → Elly (last NPC)
                                v2Flags.value = NPC_MODEL_DEFAULT_FLAG
                                extraModel.value = NPC_NAMES.size - 1
                            }
                            previewRenderer?.refresh()
                        }
                    }
                    span {
                        className = "pw-npc-field-text"
                        val update = {
                            textContent = if (v2Flags.value == 0) "NPC None"
                            else NPC_NAMES.getOrElse(extraModel.value) { "NPC ${extraModel.value}" }
                        }
                        observeNow(v2Flags) { update() }
                        observe(extraModel) { update() }
                    }
                    button { className = "pw-npc-btn"; textContent = "\u25B6"
                        onclick = {
                            if (v2Flags.value == 0) {
                                v2Flags.value = NPC_MODEL_DEFAULT_FLAG
                                extraModel.value = 0
                            } else if (extraModel.value < NPC_NAMES.size - 1) {
                                extraModel.value++
                            } else {
                                v2Flags.value = 0
                                extraModel.value = 0
                            }
                            previewRenderer?.refresh()
                        }
                    }
                }

                // Costume
                val costumeEl = navField("Costume", costume, COSTUME_COUNT)

                // Head
                val headEl = navField("Head", head, HEAD_COUNT)

                // Skin
                val skinEl = navField("Skin", skin, SKIN_COUNT)

                // Face
                val faceEl = navField("Face", face, FACE_COUNT)

                // Hair + color picker (disabled for cast)
                val hairEl = div {
                    className = "pw-npc-field"
                    // Use a wrapper so the color picker overlays without affecting text centering
                    button { className = "pw-npc-btn"; textContent = "\u25C0"
                        onclick = {
                            val max = HAIR_COUNT.getOrElse(charClass.value) { 0 }
                            hair.value = cycleValue(hair.value, min = 1, max = max, backward = true)
                            previewRenderer?.refresh()
                        } }
                    div {
                        className = "pw-npc-field-center"
                        span {
                            className = "pw-npc-field-text"
                            val update = {
                                val max = HAIR_COUNT.getOrElse(charClass.value) { 0 }
                                textContent = "Hair ${hair.value}/$max"
                            }
                            observeNow(hair) { update() }
                            observe(charClass) { update() }
                        }
                        input {
                            type = "color"
                            className = "pw-npc-color"
                            val up = { value = rgb16ToHex(hairR.value, hairG.value, hairB.value) }
                            observeNow(hairR) { up() }; observe(hairG) { up() }; observe(hairB) { up() }
                            onchange = {
                                val (r, g, b) = hexToRgb16(value)
                                hairR.value = r; hairG.value = g; hairB.value = b
                            }
                        }
                    }
                    button { className = "pw-npc-btn"; textContent = "\u25B6"
                        onclick = {
                            val max = HAIR_COUNT.getOrElse(charClass.value) { 0 }
                            hair.value = cycleValue(hair.value, min = 1, max = max, backward = false)
                            previewRenderer?.refresh()
                        }
                    }
                }

                // Disable logic: v2Flags != 0 (NPC model) disables all appearance fields.
                // For NPC None (v2Flags == 0), cast/non-cast rules apply.
                val updateDisabled = {
                    val isNpcModel = v2Flags.value != 0
                    val cls = charClass.value
                    val isCast = cls in CAST_CLASSES

                    setDisabled(costumeEl, isNpcModel || isCast)
                    setDisabled(headEl, isNpcModel || HEAD_COUNT.getOrElse(cls) { 1 } <= 1)
                    setDisabled(skinEl, isNpcModel)
                    setDisabled(faceEl, isNpcModel || isCast)
                    setDisabled(hairEl, isNpcModel || isCast)
                }
                observeNow(charClass) { updateDisabled() }
                observe(v2Flags) { updateDisabled() }

                // Proportions
                propSlider(proportionX)
                propSlider(proportionY)
            }

        private fun Node.navField(
            label: String,
            valCell: MutableCell<Int>,
            maxPerClass: IntArray,
        ): HTMLElement = div {
            className = "pw-npc-field"
            button { className = "pw-npc-btn"; textContent = "\u25C0"
                onclick = {
                    val max = maxPerClass.getOrElse(charClass.value) { 0 }
                    valCell.value = cycleValue(valCell.value, min = 1, max = max, backward = true)
                    previewRenderer?.refresh()
                } }
            span {
                className = "pw-npc-field-text"
                val update = {
                    val max = maxPerClass.getOrElse(charClass.value) { 0 }
                    textContent = "$label ${valCell.value}/$max"
                }
                observeNow(valCell) { update() }
                observe(charClass) { update() }
            }
            button { className = "pw-npc-btn"; textContent = "\u25B6"
                onclick = {
                    val max = maxPerClass.getOrElse(charClass.value) { 0 }
                    valCell.value = cycleValue(valCell.value, min = 1, max = max, backward = false)
                    previewRenderer?.refresh()
                }
            }
        }

        private fun Node.navFieldFixed(
            valCell: MutableCell<Int>,
            maxCount: Int,
            oneBased: Boolean,
            display: (Int) -> String,
        ): HTMLElement = div {
            className = "pw-npc-field"
            val min = if (oneBased) 1 else 0
            val max = if (oneBased) maxCount else maxCount - 1
            button { className = "pw-npc-btn"; textContent = "\u25C0"
                onclick = {
                    valCell.value = cycleValue(valCell.value, min, max, backward = true)
                    previewRenderer?.refresh()
                } }
            span {
                className = "pw-npc-field-text"
                observeNow(valCell) { textContent = display(it) }
            }
            button { className = "pw-npc-btn"; textContent = "\u25B6"
                onclick = {
                    valCell.value = cycleValue(valCell.value, min, max, backward = false)
                    previewRenderer?.refresh()
                } }
        }

        private fun Node.propSlider(valCell: MutableCell<Double>) {
            div {
                className = "pw-npc-prop"
                input {
                    type = "range"; min = "0"; max = "1"; step = "0.01"
                    className = "pw-npc-slider"
                    observeNow(valCell) { valueAsNumber = it }
                    oninput = { valCell.value = valueAsNumber }
                }
                span {
                    className = "pw-npc-prop-val"
                    observeNow(valCell) {
                        textContent = ((it * 100).toInt() / 100.0).toString()
                    }
                }
            }
        }

        private fun setDisabled(el: HTMLElement, disabled: Boolean) {
            el.style.opacity = if (disabled) "0.35" else "1"
            el.style.setProperty("pointer-events", if (disabled) "none" else "auto")
        }
    }

    companion object {
        init {
            // NPC_NAMES is the display list shown in the UI; CharacterClassAssetLoader
            // ships the matching NpcXxxBody.nj/.afs assets. If these get out of sync
            // the forward/back wrap will land on an extra_model index that has no
            // loadable asset, so fail fast at first instantiation instead.
            require(NPC_NAMES.size == CharacterClassAssetLoader.NPC_MODEL_COUNT) {
                "NPC_NAMES (${NPC_NAMES.size}) must match " +
                        "CharacterClassAssetLoader.NPC_MODEL_COUNT (${CharacterClassAssetLoader.NPC_MODEL_COUNT})"
            }

            @Suppress("CssUnusedSymbol", "CssUnresolvedCustomProperty")
            // language=css
            style("""
                .pw-npc-dialog-split {
                    display: flex;
                    gap: 8px;
                }

                .pw-npc-dialog-left {
                    flex: 0 0 260px;
                    overflow-y: auto;
                }

                .pw-npc-dialog-right {
                    width: 240px;
                    height: 380px;
                    background: #181818;
                    border-radius: 4px;
                    overflow: hidden;
                    flex-shrink: 0;
                }

                .pw-npc-editor {
                    display: flex;
                    flex-direction: column;
                    gap: 2px;
                }

                .pw-npc-field {
                    display: flex;
                    align-items: center;
                    height: 26px;
                    gap: 2px;
                }

                .pw-npc-field .pw-select,
                .pw-npc-field .pw-input {
                    flex: 1;
                    min-width: 0;
                }

                .pw-npc-btn {
                    width: 28px;
                    height: 24px;
                    padding: 0;
                    background: var(--pw-control-bg-color);
                    border: 1px solid var(--pw-control-border-color);
                    color: var(--pw-text-color);
                    cursor: pointer;
                    font-size: 10px;
                    flex-shrink: 0;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                }

                .pw-npc-btn:hover {
                    background: var(--pw-control-bg-color-hover);
                }

                .pw-npc-btn-placeholder {
                    width: 28px;
                    flex-shrink: 0;
                }

                .pw-npc-end-placeholder {
                    width: 26px;
                    flex-shrink: 0;
                }

                .pw-npc-field-text {
                    flex: 1;
                    text-align: center;
                    font-size: 13px;
                    white-space: nowrap;
                    user-select: none;
                }

                .pw-npc-field-center {
                    flex: 1;
                    position: relative;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    min-width: 0;
                }

                .pw-npc-field-center .pw-npc-color {
                    position: absolute;
                    right: 0;
                }

                .pw-npc-color {
                    width: 26px;
                    height: 22px;
                    padding: 1px;
                    border: 1px solid var(--pw-control-border-color);
                    border-radius: 2px;
                    background: none;
                    cursor: pointer;
                    flex-shrink: 0;
                }

                .pw-npc-prop {
                    display: flex;
                    align-items: center;
                    gap: 4px;
                    height: 26px;
                    padding: 0 30px 0 30px;
                }

                .pw-npc-slider {
                    flex: 1;
                    height: 14px;
                    cursor: pointer;
                }

                .pw-npc-prop-val {
                    width: 32px;
                    text-align: right;
                    font-size: 12px;
                }
            """.trimIndent())
        }
    }
}
