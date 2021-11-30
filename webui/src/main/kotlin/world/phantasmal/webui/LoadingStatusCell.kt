package world.phantasmal.webui

import kotlinx.coroutines.*
import mu.KotlinLogging
import world.phantasmal.observable.cell.Cell
import world.phantasmal.observable.cell.ImmutableCell
import world.phantasmal.observable.cell.SimpleCell
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
    suspend fun awaitLoad()
}

class ImmutableLoadingStatusCell(status: LoadingStatus) :
    LoadingStatusCell,
    Cell<LoadingStatus> by ImmutableCell(status) {

    override suspend fun awaitLoad() {
        // Nothing to await.
    }
}

class LoadingStatusCellImpl(
    private val cellDelegate: SimpleCell<LoadingStatus>,
    private val dataName: String,
) : LoadingStatusCell, Cell<LoadingStatus> by cellDelegate {

    constructor(dataName: String) : this(SimpleCell(LoadingStatus.Uninitialized), dataName)

    private var job: Job? = null

    fun load(scope: CoroutineScope, loadData: suspend () -> Unit) {
        logger.trace { "Loading $dataName." }

        cellDelegate.value =
            if (value == LoadingStatus.Uninitialized) LoadingStatus.InitialLoad
            else LoadingStatus.Loading

        job = scope.launch {
            var success = false

            try {
                val duration = measureTime {
                    withContext(Dispatchers.Default) {
                        loadData()
                    }
                }

                logger.trace { "Loaded $dataName in ${duration.inWholeMilliseconds}ms." }

                success = true
            } catch (e: Exception) {
                logger.error(e) { "Error while loading $dataName." }
            } finally {
                job = null

                cellDelegate.value = if (success) LoadingStatus.Ok else LoadingStatus.Error
            }
        }
    }

    override suspend fun awaitLoad() {
        job?.join()
    }
}
