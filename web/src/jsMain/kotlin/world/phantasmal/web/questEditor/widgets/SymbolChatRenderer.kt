package world.phantasmal.web.questEditor.widgets

import kotlinx.browser.document
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.Image
import world.phantasmal.psolib.buffer.Buffer

/**
 * Loads the 4 sega-part atlases and renders a symbol-chat preview to an
 * HTMLCanvas.
 *
 * The atlases live at `/assets/symbol_chat/sega_{0..3}.png`. Each is a
 * 256×256 RGBA image:
 *
 *   sega_0..2 — corner icon sets (icons 0..63 / 64..127 / 128..191)
 *   sega_3    — face expressions (top), part shapes (middle), background
 *               corner tile (bottom-right)
 *
 * Loading is module-level and lazy: the first dialog open kicks off PNG
 * fetches and subsequent dialogs reuse the cached pixel data.
 */
object SymbolChatRenderer {
    /** 256×256 packed ARGB pixels per atlas, indexed `[y * 256 + x]`. */
    private val segaPics = arrayOfNulls<IntArray>(4)
    private var loaded = false
    private val pendingCallbacks = mutableListOf<() -> Unit>()
    private var loadStarted = false

    /** Sega palette — the 9 selectable colours used by face / corner / part.
     *  Values taken from qedit source (FSymbolChat.pas:83 `segaColors`). */
    private val SEGA_COLORS = intArrayOf(
        0xCFCFCF, 0x3C50F3, 0x2FA3FF, 0x04FDDF, 0x79FD79,
        0xFAAC87, 0xF68BD5, 0x787878, 0xFFFFFF,
    )

    /** Face index -> tile slot in segaPics[3]. */
    private val FACE_MAP = intArrayOf(2, 0, 1, 3)

    const val CANVAS_WIDTH = 144
    const val CANVAS_HEIGHT = 80

    /** Total picker icons (192 corner icons + slot 255 = "none"). */
    const val ICON_COUNT = 192
    /** Total picker part shapes (0..67 + slot 255 = "none"). */
    const val PART_COUNT = 68

    /**
     * Rectangle a part occupies in canvas pixels. Returned by [partRect] for
     * hit-testing — the dialog uses this to map mouse clicks to part indices.
     * `null` size means the part is empty (`partId == 0xFF`).
     */
    data class PartRect(val left: Int, val top: Int, val width: Int, val height: Int) {
        operator fun contains(p: Pair<Int, Int>): Boolean {
            val (x, y) = p
            return x in left until (left + width) && y in top until (top + height)
        }
    }

    /**
     * Returns the canvas rect for part `c` given its [partId], [posX], [posY],
     * or null when the slot is empty. Uses the same dimension lookup as [render].
     */
    fun partRect(partId: Int, posX: Int, posY: Int): PartRect? {
        if (partId == 0xFF) return null
        val w: Int; val h: Int
        when {
            partId < 48 -> { w = 16; h = 16 }
            partId < 60 -> { w = 40; h = 20 }
            else        -> { w = 32; h = 32 }
        }
        val left = posX + 40 - (w / 2)
        val top = (posY * 0.92).toInt()
        return PartRect(left, top, w, h)
    }

    /**
     * Triggers asset loading if needed and invokes [onReady] when all 4
     * atlases are decoded. If already loaded, runs the callback synchronously.
     */
    fun ensureLoaded(onReady: () -> Unit) {
        if (loaded) {
            onReady()
            return
        }
        pendingCallbacks.add(onReady)
        if (loadStarted) return
        loadStarted = true

        var done = 0
        for (i in 0..3) {
            val img = Image()
            img.crossOrigin = "anonymous"
            img.onload = { _ ->
                segaPics[i] = imageToArgbArray(img)
                done++
                if (done == 4) {
                    loaded = true
                    val cbs = pendingCallbacks.toList()
                    pendingCallbacks.clear()
                    cbs.forEach { it() }
                }
                null
            }
            img.onerror = { _, _, _, _, _ ->
                // Leave segaPics[i] = null; render() will skip layers it
                // can't draw. We still flip `loaded` so the dialog stops
                // waiting forever.
                done++
                if (done == 4) {
                    loaded = true
                    val cbs = pendingCallbacks.toList()
                    pendingCallbacks.clear()
                    cbs.forEach { it() }
                }
                null
            }
            img.src = "/assets/symbol_chat/sega_$i.png"
        }
    }

    private fun imageToArgbArray(img: HTMLImageElement): IntArray {
        val canvas = document.createElement("canvas") as HTMLCanvasElement
        canvas.width = 256
        canvas.height = 256
        val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
        ctx.drawImage(img, 0.0, 0.0)
        val data = ctx.getImageData(0.0, 0.0, 256.0, 256.0).data
        val out = IntArray(256 * 256)
        val raw = data.asDynamic()
        for (p in out.indices) {
            val o = p * 4
            val r = (raw[o] as Int) and 0xFF
            val g = (raw[o + 1] as Int) and 0xFF
            val b = (raw[o + 2] as Int) and 0xFF
            val a = (raw[o + 3] as Int) and 0xFF
            out[p] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
        return out
    }

    /**
     * Convenience: decodes a 60-byte little-endian SymbolChatT [buf] and
     * renders it. The buffer must be at least 60 bytes; both the dlabel
     * (custom) path and the symbolchatcolli (built-in) path produce LE data.
     */
    fun renderBuffer(canvas: HTMLCanvasElement, buf: Buffer) {
        require(buf.size >= 60) { "SymbolChatT buffer too small: ${buf.size}" }
        val face = buf.getInt(0)
        val cornerIcons = IntArray(4)
        val cornerParams = IntArray(4)
        for (k in 0..3) {
            cornerIcons[k] = buf.getUByte(4 + k * 2).toInt()
            cornerParams[k] = buf.getUByte(4 + k * 2 + 1).toInt()
        }
        val partIds = IntArray(12)
        val partXs = IntArray(12)
        val partYs = IntArray(12)
        val partMirrors = IntArray(12)
        for (k in 0..11) {
            val o = 12 + k * 4
            partIds[k] = buf.getUByte(o).toInt()
            partXs[k] = buf.getUByte(o + 1).toInt()
            partYs[k] = buf.getUByte(o + 2).toInt()
            partMirrors[k] = buf.getUByte(o + 3).toInt()
        }
        render(canvas, face, cornerIcons, cornerParams, partIds, partXs, partYs, partMirrors)
    }

    /**
     * Draws the current symbol-chat into [canvas]. Caller is responsible for
     * sizing the canvas (use [CANVAS_WIDTH] × [CANVAS_HEIGHT]).
     */
    fun render(
        canvas: HTMLCanvasElement,
        face: Int,
        cornerIcons: IntArray, // length 4
        cornerParams: IntArray, // length 4
        partIds: IntArray, // length 12
        partXs: IntArray,
        partYs: IntArray,
        partMirrors: IntArray,
        /** Index of the part to highlight with a yellow outline, or -1 for none. */
        highlightPart: Int = -1,
    ) {
        canvas.width = CANVAS_WIDTH
        canvas.height = CANVAS_HEIGHT
        val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
        val image = ctx.createImageData(CANVAS_WIDTH.toDouble(), CANVAS_HEIGHT.toDouble())
        val dst = IntArray(CANVAS_WIDTH * CANVAS_HEIGHT) { 0xFFFFFFFF.toInt() } // opaque white

        // ---- face -----------------------------------------------------------
        run {
            val faceIdx = FACE_MAP[face and 3]
            val sx = (faceIdx and 1) * 64
            val sy = (faceIdx / 2) * 64 + 128
            val color = (face shr 2) and 7
            drawSega(dst, 3, sx, sy, sx + 64, sy + 64, dstL = 40, dstT = 8, color = color, wMirror = 0, hMirror = 0)
        }

        // ---- corners --------------------------------------------------------
        // The four corner positions are hard-coded:
        //   c0 → frame at (4,6),   icon at (8,6)
        //   c1 → frame at (99,6),  icon at (104,6)
        //   c2 → frame at (4,46),  icon at (8,48)
        //   c3 → frame at (99,46), icon at (104,48)
        // Frame source: segaPics[3] ([181|128], [168|128], +42, +40), drawn at
        // each corner's frame position. Icon source: 32×32 from segaPics[icon/64]
        // at ((icon&7)*32, ((icon&63)/8)*32), drawn at the icon position.
        val cornerLayouts = arrayOf(
            intArrayOf(/*frameSx*/181, /*frameSy*/168, /*frameDstL*/4,  /*frameDstT*/6,  /*iconDstL*/8,   /*iconDstT*/6),
            intArrayOf(128, 168, 99, 6,  104, 6),
            intArrayOf(181, 128, 4, 46,  8,   48),
            intArrayOf(128, 128, 99, 46, 104, 48),
        )
        for (k in 0..3) {
            val icon = cornerIcons[k]
            if (icon == 0xFF) continue
            val L = cornerLayouts[k]
            val frameSx = L[0]; val frameSy = L[1]
            val frameDstL = L[2]; val frameDstT = L[3]
            val iconDstL = L[4]; val iconDstT = L[5]

            // Frame background tile is only drawn for icons < 19.
            if (icon < 19) {
                drawSega(dst, 3,
                    frameSx, frameSy, frameSx + 42, frameSy + 40,
                    dstL = frameDstL, dstT = frameDstT, color = 8, wMirror = 0, hMirror = 0)
            }

            // Icon itself.
            val atlasIdx = icon / 64
            val iconLocal = icon and 63
            val ix = (iconLocal and 7) * 32
            val iy = (iconLocal / 8) * 32
            val param = cornerParams[k]
            drawSega(dst, atlasIdx,
                ix, iy, ix + 32, iy + 32,
                dstL = iconDstL, dstT = iconDstT,
                color = param and 7,
                wMirror = (param shr 3) and 1,
                hMirror = (param shr 4) and 1)
        }

        // ---- parts ----------------------------------------------------------
        for (c in 0 until 12) {
            val partId = partIds[c]
            if (partId == 0xFF) continue

            val sx: Int; val sy: Int; val w: Int; val h: Int
            when {
                partId < 48 -> {
                    sx = (partId % 8) * 16
                    sy = (partId / 8) * 16
                    w = 16; h = 16
                }
                partId < 60 -> {
                    val pl = partId - 48
                    sx = (pl % 3) * 40 + 128
                    sy = (pl / 3) * 20
                    w = 40; h = 20
                }
                else -> {
                    sx = (partId - 60) * 32
                    sy = 96
                    w = 32; h = 32
                }
            }

            val mirror = partMirrors[c]
            val dstL = partXs[c] + 40 - (w / 2)
            val dstT = (partYs[c] * 0.92).toInt()
            drawSega(dst, 3,
                sx, sy, sx + w, sy + h,
                dstL = dstL, dstT = dstT,
                color = 8, // parts always render in white
                wMirror = mirror and 1,
                hMirror = (mirror shr 1) and 1)
        }

        // ---- selection highlight -------------------------------------------
        if (highlightPart in 0 until 12) {
            val rect = partRect(partIds[highlightPart], partXs[highlightPart], partYs[highlightPart])
            if (rect != null) drawRectOutline(dst, rect, 0xFFFF00) // yellow
        }

        // ---- copy IntArray RGB into ImageData -------------------------------
        for (p in dst.indices) {
            val px = dst[p]
            val o = p * 4
            data_set(image.data, o,     ((px shr 16) and 0xFF))
            data_set(image.data, o + 1, ((px shr 8) and 0xFF))
            data_set(image.data, o + 2, (px and 0xFF))
            data_set(image.data, o + 3, 255)
        }
        ctx.putImageData(image, 0.0, 0.0)
    }

    /** ImageData.data is Uint8ClampedArray; index assignment goes through `asDynamic`. */
    private fun data_set(arr: org.khronos.webgl.Uint8ClampedArray, idx: Int, value: Int) {
        arr.asDynamic()[idx] = value
    }

    /**
     * Draws a single 32×32 corner icon onto [canvas]. Used by the icon
     * picker popup. Pass `0xFF` to render an empty / "none" cell. Returns
     * silently if atlases haven't loaded yet — caller should rerender after
     * [ensureLoaded] fires.
     */
    fun drawIconTile(canvas: HTMLCanvasElement, iconId: Int) {
        canvas.width = 32
        canvas.height = 32
        val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
        ctx.fillStyle = "#181818"
        ctx.fillRect(0.0, 0.0, 32.0, 32.0)
        if (iconId == 0xFF) {
            ctx.strokeStyle = "#888"
            ctx.beginPath()
            ctx.moveTo(4.0, 4.0); ctx.lineTo(28.0, 28.0)
            ctx.moveTo(28.0, 4.0); ctx.lineTo(4.0, 28.0)
            ctx.stroke()
            return
        }
        if (!loaded) return
        val atlasIdx = iconId / 64
        val src = segaPics[atlasIdx] ?: return
        val local = iconId and 63
        val sx = (local and 7) * 32
        val sy = (local / 8) * 32
        blitToCanvas(ctx, src, sx, sy, 32, 32, 0, 0)
    }

    /**
     * Draws a single part shape onto [canvas] at 32×32. Centred for
     * 16×16 parts; scaled-down for 40×20 parts. Pass `0xFF` for empty.
     */
    fun drawPartTile(canvas: HTMLCanvasElement, partId: Int) {
        canvas.width = 32
        canvas.height = 32
        val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
        ctx.fillStyle = "#181818"
        ctx.fillRect(0.0, 0.0, 32.0, 32.0)
        if (partId == 0xFF) {
            ctx.strokeStyle = "#888"
            ctx.beginPath()
            ctx.moveTo(4.0, 4.0); ctx.lineTo(28.0, 28.0)
            ctx.moveTo(28.0, 4.0); ctx.lineTo(4.0, 28.0)
            ctx.stroke()
            return
        }
        if (!loaded) return
        val src = segaPics[3] ?: return
        val sx: Int; val sy: Int; val w: Int; val h: Int
        when {
            partId < 48 -> {
                sx = (partId % 8) * 16
                sy = (partId / 8) * 16
                w = 16; h = 16
            }
            partId < 60 -> {
                val pl = partId - 48
                sx = (pl % 3) * 40 + 128
                sy = (pl / 3) * 20
                w = 40; h = 20
            }
            else -> {
                sx = (partId - 60) * 32
                sy = 96
                w = 32; h = 32
            }
        }
        // Centre the source rect in the 32×32 cell. For 40×20 parts the
        // 40-wide source would overflow — clip to the cell.
        val dstX = ((32 - w) / 2).coerceAtLeast(0)
        val dstY = ((32 - h) / 2).coerceAtLeast(0)
        blitToCanvas(ctx, src, sx, sy, w, h, dstX, dstY)
    }

    private fun blitToCanvas(
        ctx: CanvasRenderingContext2D,
        src: IntArray,
        sx: Int, sy: Int,
        w: Int, h: Int,
        dstX: Int, dstY: Int,
    ) {
        // Clip to canvas bounds.
        val cw = ctx.canvas.width
        val ch = ctx.canvas.height
        val drawW = (w).coerceAtMost(cw - dstX)
        val drawH = (h).coerceAtMost(ch - dstY)
        if (drawW <= 0 || drawH <= 0) return

        val image = ctx.createImageData(drawW.toDouble(), drawH.toDouble())
        val data = image.data.asDynamic()
        // Background colour for the picker tile (dark gray #181818)
        val bgR = 24; val bgG = 24; val bgB = 24
        for (y in 0 until drawH) {
            for (x in 0 until drawW) {
                val srcPx = src[(sy + y) * 256 + (sx + x)]
                val a = (srcPx ushr 24) and 0xFF
                val o = (y * drawW + x) * 4
                if (a == 0) {
                    data[o] = bgR; data[o + 1] = bgG; data[o + 2] = bgB; data[o + 3] = 255
                } else if (a == 255) {
                    data[o] = (srcPx shr 16) and 0xFF
                    data[o + 1] = (srcPx shr 8) and 0xFF
                    data[o + 2] = srcPx and 0xFF
                    data[o + 3] = 255
                } else {
                    val sR = (srcPx shr 16) and 0xFF
                    val sG = (srcPx shr 8) and 0xFF
                    val sB = srcPx and 0xFF
                    data[o]     = (sR * a + bgR * (255 - a)) / 255
                    data[o + 1] = (sG * a + bgG * (255 - a)) / 255
                    data[o + 2] = (sB * a + bgB * (255 - a)) / 255
                    data[o + 3] = 255
                }
            }
        }
        ctx.putImageData(image, dstX.toDouble(), dstY.toDouble())
    }

    private fun drawRectOutline(dst: IntArray, rect: PartRect, rgb: Int) {
        val argb = (0xFF shl 24) or (rgb and 0xFFFFFF)
        val l = rect.left.coerceIn(0, CANVAS_WIDTH - 1)
        val r = (rect.left + rect.width - 1).coerceIn(0, CANVAS_WIDTH - 1)
        val t = rect.top.coerceIn(0, CANVAS_HEIGHT - 1)
        val b = (rect.top + rect.height - 1).coerceIn(0, CANVAS_HEIGHT - 1)
        for (x in l..r) {
            dst[t * CANVAS_WIDTH + x] = argb
            dst[b * CANVAS_WIDTH + x] = argb
        }
        for (y in t..b) {
            dst[y * CANVAS_WIDTH + l] = argb
            dst[y * CANVAS_WIDTH + r] = argb
        }
    }

    /**
     * Per-pixel blit with alpha blending.
     * - Fully-transparent source pixels (a == 0) are skipped.
     * - Source pixels whose RGB exceeds 0xC0C0C0 are recoloured with
     *   `SEGA_COLORS[color]` before blending (this maps the white "fill" of
     *   faces/frames to the chosen sega palette colour while leaving dark
     *   outlines and expressions intact).
     * - Semi-transparent pixels (anti-aliased edges) are blended over the
     *   current destination, producing smooth edges on the white canvas.
     */
    private fun drawSega(
        dst: IntArray,
        srcAtlas: Int,
        srcL: Int, srcT: Int, srcR: Int, srcB: Int,
        dstL: Int, dstT: Int,
        color: Int,
        wMirror: Int,
        hMirror: Int,
    ) {
        val src = segaPics[srcAtlas] ?: return
        val w = srcR - srcL
        val h = srcB - srcT
        val recolor = SEGA_COLORS[color.coerceIn(0, SEGA_COLORS.lastIndex)]

        for (y in 0 until h) {
            val sy = srcT + y
            if (sy < 0 || sy >= 256) continue
            val py = if (hMirror != 0) dstT + (h - 1 - y) else dstT + y
            if (py < 0 || py >= CANVAS_HEIGHT) continue

            for (x in 0 until w) {
                val sx = srcL + x
                if (sx < 0 || sx >= 256) continue
                val srcPx = src[sy * 256 + sx]
                // Match qedit's alpha handling: only draw pixels whose top bit
                // is set (alpha >= 128), with no blending. This produces the
                // same hard-edged look as the original tool.
                if (srcPx.toUInt() <= 0x7FFFFFFFu) continue

                val rgb = srcPx and 0xFFFFFF
                val coloredRgb = if (rgb > 0xC0C0C0) recolor else rgb

                val px = if (wMirror != 0) dstL + (w - 1 - x) else dstL + x
                if (px < 0 || px >= CANVAS_WIDTH) continue

                dst[py * CANVAS_WIDTH + px] = (0xFF shl 24) or coloredRgb
            }
        }
    }
}
