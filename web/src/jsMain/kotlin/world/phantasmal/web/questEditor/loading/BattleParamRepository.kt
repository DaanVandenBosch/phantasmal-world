package world.phantasmal.web.questEditor.loading

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mu.KotlinLogging
import world.phantasmal.core.disposable.DisposableSupervisedScope
import world.phantasmal.cell.Cell
import world.phantasmal.cell.mutableCell
import world.phantasmal.psolib.Endianness
import world.phantasmal.psolib.battleparam.BattleParamSet
import world.phantasmal.psolib.battleparam.BattleParamTable
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.webui.DisposableContainer

private val logger = KotlinLogging.logger {}

/**
 * Lazily loads the 8 standard PSO `BattleParamEntry*.dat` files from
 * `/assets/battle_param/` and caches the parsed [BattleParamTable]s.
 *
 * If a file is missing (e.g. assets not yet provisioned), the corresponding
 * cell stays at `null`. The Load Template UI uses [available] to enable/disable
 * itself based on whether at least one set has loaded.
 */
class BattleParamRepository(
    private val assetLoader: AssetLoader,
) : DisposableContainer() {

    private val tablesByOrdinal: Array<BattleParamTable?> = arrayOfNulls(BattleParamSet.entries.size)

    private val _available = mutableCell(false)
    val available: Cell<Boolean> = _available

    private val scope = addDisposable(DisposableSupervisedScope(this::class, Dispatchers.Main))

    init {
        scope.launch { loadAll() }
    }

    /** Returns the parsed table for [set], or null if it hasn't been loaded (or is missing). */
    fun get(set: BattleParamSet): BattleParamTable? = tablesByOrdinal[set.ordinal]

    private suspend fun loadAll() {
        var any = false
        for (set in BattleParamSet.entries) {
            try {
                val ab = assetLoader.loadArrayBuffer("/battle_param/${set.fileName}")
                val buf = Buffer.fromArrayBuffer(ab, Endianness.Little)
                if (buf.size >= BattleParamTable.FILE_SIZE) {
                    tablesByOrdinal[set.ordinal] = BattleParamTable(set, buf)
                    any = true
                } else {
                    logger.warn { "BattleParam ${set.fileName} too small: ${buf.size}" }
                }
            } catch (e: Throwable) {
                // Asset missing — silently leave this set unavailable.
                logger.warn { "BattleParam ${set.fileName} not loaded: ${e.message}" }
            }
        }
        if (any) _available.value = true
    }
}
