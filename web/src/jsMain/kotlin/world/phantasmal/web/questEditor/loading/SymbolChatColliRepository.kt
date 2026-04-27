package world.phantasmal.web.questEditor.loading

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mu.KotlinLogging
import world.phantasmal.cell.Cell
import world.phantasmal.cell.mutableCell
import world.phantasmal.core.disposable.DisposableSupervisedScope
import world.phantasmal.psolib.Endianness
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.symbolchat.SymbolChatColliTable
import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.webui.DisposableContainer

private val logger = KotlinLogging.logger {}

/**
 * Loads `symbolchatcolli.bin` (the PRS-decompressed payload of PSO BB's
 * `symbolchatcolli.prs`) once on construction and exposes it as a
 * [SymbolChatColliTable].
 *
 * Used by the Symbol Chat Object inspector to render a preview of the
 * built-in symbol chat referenced by `SC ID 1`.
 */
class SymbolChatColliRepository(
    private val assetLoader: AssetLoader,
) : DisposableContainer() {

    private var loaded: SymbolChatColliTable? = null

    private val _available = mutableCell(false)
    val available: Cell<Boolean> = _available

    private val scope = addDisposable(DisposableSupervisedScope(this::class, Dispatchers.Main))

    init {
        scope.launch { load() }
    }

    /** Returns the 60-byte SymbolChatT slice for [id], or null if not loaded / out of range. */
    fun entry(id: Int): Buffer? = loaded?.entry(id)

    private suspend fun load() {
        try {
            val ab = assetLoader.loadArrayBuffer("/symbol_chat/symbolchatcolli.bin")
            val buf = Buffer.fromArrayBuffer(ab, Endianness.Little)
            if (buf.size >= SymbolChatColliTable.FILE_SIZE) {
                loaded = SymbolChatColliTable(buf)
                _available.value = true
            } else {
                logger.warn { "symbolchatcolli.bin too small: ${buf.size}" }
            }
        } catch (e: Throwable) {
            logger.warn { "symbolchatcolli.bin not loaded: ${e.message}" }
        }
    }
}
