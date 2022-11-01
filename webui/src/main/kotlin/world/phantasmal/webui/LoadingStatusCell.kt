package world.phantasmal.webui

import kotlinx.coroutines.*
import mu.KotlinLogging
import world.phantasmal.cell.Cell
import world.phantasmal.cell.MutableCell
import world.phantasmal.cell.mutableCell
import kotlin.time.measureTime

private val logger = KotlinLogging.logger {}

enum class LoadingStatus {
    Uninitialized,
    InitialLoad,
    Loading,
    Ok,
    Error,
}

interface LoadingStatusCell : Cell<LoadingStatus> {
    /** Await the current load, if a load in ongoing. */
    suspend fun await()
}

class LoadingStatusCellImpl private constructor(
    private val scope: CoroutineScope,
    private val dataName: String,
    /** Will be called with [Dispatchers.Main] context. */
    private val loadData: suspend () -> Unit,
    private val cellDelegate: MutableCell<LoadingStatus>,
) : LoadingStatusCell, Cell<LoadingStatus> by cellDelegate {

    constructor(
        scope: CoroutineScope,
        dataName: String,
        loadData: suspend () -> Unit,
    ) : this(scope, dataName, loadData, mutableCell(LoadingStatus.Uninitialized))

    private var currentJob: Job? = null

    fun load() {
        logger.trace { "Loading $dataName." }

        cellDelegate.value =
            if (value == LoadingStatus.Uninitialized) LoadingStatus.InitialLoad
            else LoadingStatus.Loading

        currentJob?.cancel("New load started.")

        currentJob = scope.launch(Dispatchers.Main) {
            var success = false

            try {
                val duration = measureTime {
                    loadData()
                }

                logger.trace { "Loaded $dataName in ${duration.inWholeMilliseconds}ms." }

                success = true
            } catch (e: CancellationException) {
                logger.trace(e) { "Loading $dataName was cancelled." }
            } catch (e: Exception) {
                logger.error(e) { "Error while loading $dataName." }
            }

            // Only reset job and set value when a new job hasn't been started in the meantime.
            if (coroutineContext.job == currentJob) {
                currentJob = null
                cellDelegate.value = if (success) LoadingStatus.Ok else LoadingStatus.Error
            }
        }
    }

    override suspend fun await() {
        currentJob?.let {
            if (!it.isCompleted) {
                it.join()
            }
        }
    }
}
