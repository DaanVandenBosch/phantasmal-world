package world.phantasmal.web.questEditor.widgets

import kotlinx.browser.document
import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.symbolchat.SymbolChatColliTable
import world.phantasmal.web.questEditor.asm.SymbolChatTriggerInfo
import world.phantasmal.web.questEditor.asm.SymbolChatTriggerStage
import world.phantasmal.web.questEditor.loading.SymbolChatColliRepository

/**
 * Resolves a trigger's per-slot spec info into renderable strips, applying
 * newserv's "dlabel wins" rule: if the slot carries a dlabel, use the
 * 60-byte body at that data label (stripping the 4-byte HEX header);
 * otherwise fall back to the built-in preset indexed by [SymbolChatTriggerStage.scId].
 * Slots that resolve to neither (sentinel scId + no dlabel, or missing
 * segment) are skipped.
 *
 * Callers provide their own [readSegmentData] (typically
 * `DataEditorController::readSegmentData`) so this helper stays free of
 * controller dependencies.
 */
fun resolveTriggerStages(
    trigger: SymbolChatTriggerInfo,
    readSegmentData: (Int) -> Buffer?,
    colliRepo: SymbolChatColliRepository,
): List<SymbolChatStageRenderer.Stage> {
    val out = mutableListOf<SymbolChatStageRenderer.Stage>()
    for (stage in trigger.stages) {
        val buf = resolveTriggerStageBuffer(stage, readSegmentData, colliRepo) ?: continue
        out.add(SymbolChatStageRenderer.Stage(slot = stage.slot, buf = buf))
    }
    return out
}

private fun resolveTriggerStageBuffer(
    stage: SymbolChatTriggerStage,
    readSegmentData: (Int) -> Buffer?,
    colliRepo: SymbolChatColliRepository,
): Buffer? {
    val dlabelBuf = stage.dlabel?.let { label ->
        readSegmentData(label)?.let { raw ->
            if (raw.size >= SymbolChatColliTable.SYMBOL_CHAT_SIZE + 4) {
                raw.slice(4, SymbolChatColliTable.SYMBOL_CHAT_SIZE)
            } else null
        }
    }
    return dlabelBuf ?: stage.scId?.let { colliRepo.entry(it) }
}

/**
 * Paints the "N stages side by side with S1/S2/S3 badges" layout used by
 * both the 3D billboard above `SymbolChatObject` entities and the inline
 * preview below `set_symbol_chat_collision` calls in the ASM editor.
 *
 * The two callers share the visual language — three 144×80 SC strips in a
 * row, each with an orange slot badge — so they share this renderer.
 * Callers differ in WHERE the canvas is mounted (3D texture vs. Monaco
 * view zone) and WHEN it's repainted (entity selection / prop change vs.
 * quest reassembly).
 */
object SymbolChatStageRenderer {

    /**
     * A single resolvable stage: a spec slot number (1..3) and the 60-byte
     * `SymbolChatT` body ready to render. Callers are expected to have
     * already resolved the "dlabel wins over built-in SC ID" rule and the
     * "skip sentinel" rule before building this list.
     */
    data class Stage(val slot: Int, val buf: Buffer)

    /**
     * Resizes [canvas] to `CANVAS_WIDTH * max(stages.size, 1)` × `CANVAS_HEIGHT`
     * and paints each stage as a 144×80 strip with a top-left slot badge.
     * If [stages] is empty, paints a single-strip gray "nothing at any
     * stage" placeholder so the caller can distinguish "deliberately
     * silent throughout" from "missing configuration".
     *
     * Painting may be deferred until `SymbolChatRenderer`'s atlas loads;
     * [onPainted] is invoked (synchronously or later) once the canvas
     * reflects the final state. Callers use this to mark textures dirty
     * or trigger reflow.
     */
    fun paintStages(
        canvas: HTMLCanvasElement,
        stages: List<Stage>,
        onPainted: () -> Unit = {},
    ) {
        val stripW = SymbolChatRenderer.CANVAS_WIDTH
        val stripH = SymbolChatRenderer.CANVAS_HEIGHT
        val columns = stages.size.coerceAtLeast(1)
        canvas.width = stripW * columns
        canvas.height = stripH

        if (stages.isEmpty()) {
            drawPlaceholder(canvas, "nothing at any stage")
            onPainted()
            return
        }

        SymbolChatRenderer.ensureLoaded {
            val ctx = canvas.getContext("2d").asDynamic()
            ctx.clearRect(0, 0, canvas.width, canvas.height)

            // SymbolChatRenderer.renderBuffer resizes the canvas it paints
            // onto, so we can't share the main canvas with it. Render each
            // stage onto a temp canvas and drawImage the result at the
            // slot's x-offset on the main canvas.
            for ((idx, stage) in stages.withIndex()) {
                val temp = document.createElement("canvas") as HTMLCanvasElement
                SymbolChatRenderer.renderBuffer(temp, stage.buf)
                ctx.drawImage(temp, idx * stripW, 0)
                drawBadge(canvas, idx * stripW + 2.0, 2.0, stage.slot)
            }

            onPainted()
        }
    }

    /** Orange "S1" / "S2" / "S3" badge at the top-left of a stage strip. */
    private fun drawBadge(canvas: HTMLCanvasElement, x: Double, y: Double, slot: Int) {
        val ctx = canvas.getContext("2d").asDynamic()
        val w = 28.0
        val h = 16.0
        ctx.fillStyle = "rgba(255, 170, 0, 0.85)"
        ctx.fillRect(x, y, w, h)
        ctx.fillStyle = "#000"
        ctx.font = "bold 11px sans-serif"
        ctx.textAlign = "center"
        ctx.textBaseline = "middle"
        ctx.fillText("S$slot", x + w / 2, y + h / 2 + 1)
    }

    private fun drawPlaceholder(canvas: HTMLCanvasElement, title: String) {
        val ctx = canvas.getContext("2d").asDynamic()
        ctx.fillStyle = "#2a2a2a"
        ctx.fillRect(0, 0, canvas.width, canvas.height)
        ctx.strokeStyle = "#666"
        ctx.lineWidth = 1
        ctx.strokeRect(0.5, 0.5, canvas.width - 1.0, canvas.height - 1.0)
        ctx.fillStyle = "#ccc"
        ctx.font = "bold 13px sans-serif"
        ctx.textAlign = "center"
        ctx.textBaseline = "middle"
        ctx.fillText(title, canvas.width / 2.0, canvas.height / 2.0)
    }
}
