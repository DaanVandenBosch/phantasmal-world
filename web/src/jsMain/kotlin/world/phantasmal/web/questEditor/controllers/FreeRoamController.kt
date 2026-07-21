package world.phantasmal.web.questEditor.controllers

import world.phantasmal.cell.*
import world.phantasmal.cell.list.ListCell
import world.phantasmal.cell.list.mutableListCell
import world.phantasmal.core.externals.browser.FileSystemDirectoryHandle
import world.phantasmal.psolib.Episode
import world.phantasmal.psolib.fileFormats.quest.getFloorFileInfo
import world.phantasmal.web.questEditor.loading.FreeRoamAreaInfo
import world.phantasmal.webui.controllers.Controller

class FreeRoamController(
) : Controller() {

    private val _freeRoamState = mutableCell<FreeRoamState?>(null)
    private val _freeRoamV1 = mutableCell<Int?>(null)
    private val _freeRoamV2 = mutableCell<Int?>(null)
    private val _freeRoamOffline = mutableCell(false)
    private val _freeRoamUltimate = mutableCell(false)
    private val _freeRoamV1Options = mutableListCell<Int>()
    private val _freeRoamV2Options = mutableListCell<Int>()

    val freeRoamV1Options: ListCell<Int> = _freeRoamV1Options
    val freeRoamV2Options: ListCell<Int> = _freeRoamV2Options
    val showFreeRoamV1: Cell<Boolean> = _freeRoamState.map { it != null && it.v1Values.size > 1 }
    val showFreeRoamV2: Cell<Boolean> = _freeRoamState.map { it != null && it.v2Values.size > 1 }
    val showFreeRoamOnOff: Cell<Boolean> = _freeRoamState.map { it?.hasOnOff == true }
    val showFreeRoamUltimate: Cell<Boolean> = _freeRoamState.map { it?.hasUltimate == true }
    val freeRoamV1: Cell<Int?> = _freeRoamV1
    val freeRoamV2: Cell<Int?> = _freeRoamV2
    val freeRoamOffline: Cell<Boolean> = _freeRoamOffline
    val freeRoamUltimate: Cell<Boolean> = _freeRoamUltimate

    fun setupFreeRoamState(
        episode: Episode,
        floorRange: IntRange,
        gameDirHandle: FileSystemDirectoryHandle,
        binPrefix: String?,
        isCity: Boolean,
        initialOffline: Boolean = false,
        initialUltimate: Boolean = false,
        tokenBase: String? = null,
    ) {
        val (v1Values, v2Values) = computeSelectableVariants(episode, floorRange)
        _freeRoamState.value = FreeRoamState(
            episode, floorRange, gameDirHandle,
            v1Values, v2Values,
            binPrefix, isCity,
            hasOnOff = isCity,
            hasUltimate = episode == Episode.I || episode == Episode.II,
            tokenBase = tokenBase,
        )
        _freeRoamV1.value = v1Values.firstOrNull()
        _freeRoamV2.value = v2Values.firstOrNull()
        _freeRoamV1Options.value = v1Values
        _freeRoamV2Options.value = v2Values
        _freeRoamOffline.value = initialOffline
        _freeRoamUltimate.value = initialUltimate
    }

    fun clearFreeRoamState() {
        _freeRoamState.value = null
        _freeRoamV1.value = null
        _freeRoamV2.value = null
        _freeRoamV1Options.clear()
        _freeRoamV2Options.clear()
    }

    suspend fun setFreeRoamV1(v1: Int, onReload: suspend (FreeRoamReloadParams) -> Unit) {
        _freeRoamV1.value = v1
        reloadFreeRoam(onReload)
    }

    suspend fun setFreeRoamV2(v2: Int, onReload: suspend (FreeRoamReloadParams) -> Unit) {
        _freeRoamV2.value = v2
        reloadFreeRoam(onReload)
    }

    suspend fun setFreeRoamOffline(offline: Boolean, onReload: suspend (FreeRoamReloadParams) -> Unit) {
        _freeRoamOffline.value = offline
        reloadFreeRoam(onReload)
    }

    suspend fun setFreeRoamUltimate(ultimate: Boolean, onReload: suspend (FreeRoamReloadParams) -> Unit) {
        _freeRoamUltimate.value = ultimate
        reloadFreeRoam(onReload)
    }

    private suspend fun reloadFreeRoam(onReload: suspend (FreeRoamReloadParams) -> Unit) {
        val state = _freeRoamState.value ?: return
        val v1 = _freeRoamV1.value ?: 0
        val v2 = _freeRoamV2.value ?: 0

        val info = FreeRoamAreaInfo(
            episode = state.episode,
            floorRange = state.floorRange,
            binPrefix = state.binPrefix,
            tokenBase = state.tokenBase,
            isCity = state.isCity,
            offline = _freeRoamOffline.value,
            ultimate = _freeRoamUltimate.value,
        )

        val params = FreeRoamReloadParams(
            gameDirHandle = state.gameDirHandle,
            info = info,
            v1 = v1,
            v2 = v2,
        )

        onReload(params)
    }

    /**
     * Compute selectable v1/v2 values for an area group.
     * Only includes values from floors that have more than one option.
     */
    private fun computeSelectableVariants(
        episode: Episode,
        floorRange: IntRange,
    ): Pair<List<Int>, List<Int>> {
        val allV1 = mutableSetOf<Int>()
        val allV2 = mutableSetOf<Int>()
        for (floor in floorRange) {
            val info = getFloorFileInfo(episode, floor) ?: continue
            if (info.v1Values.size > 1) allV1.addAll(info.v1Values)
            if (info.v2Values.size > 1) allV2.addAll(info.v2Values)
        }
        return Pair(allV1.sorted(), allV2.sorted())
    }

    private class FreeRoamState(
        val episode: Episode,
        val floorRange: IntRange,
        val gameDirHandle: FileSystemDirectoryHandle,
        val v1Values: List<Int>,
        val v2Values: List<Int>,
        val binPrefix: String?,
        val isCity: Boolean,
        val hasOnOff: Boolean,
        val hasUltimate: Boolean,
        val tokenBase: String? = null,
    )
}

/**
 * Parameters passed back to the toolbar controller when a free roam reload is needed.
 */
class FreeRoamReloadParams(
    val gameDirHandle: FileSystemDirectoryHandle,
    val info: FreeRoamAreaInfo,
    val v1: Int,
    val v2: Int,
)
