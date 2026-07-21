package world.phantasmal.web.questEditor.stores

import world.phantasmal.cell.Cell
import world.phantasmal.cell.MutableCell
import world.phantasmal.cell.mutableCell
import world.phantasmal.web.externals.three.Vector3
import world.phantasmal.webui.stores.Store

data class ContextMenuRequest(val clientX: Int, val clientY: Int)

class ViewportStore : Store() {
    private val _mouseWorldPosition: MutableCell<Vector3?> = mutableCell(null)
    private val _targetCameraPosition: MutableCell<Vector3?> = mutableCell(null)
    private val _gotoIndicatorPosition: MutableCell<Vector3?> = mutableCell(null)
    private val _contextMenuRequest: MutableCell<ContextMenuRequest?> = mutableCell(null)

    val mouseWorldPosition: Cell<Vector3?> = _mouseWorldPosition
    val targetCameraPosition: Cell<Vector3?> = _targetCameraPosition

    /**
     * Last "goto position" target. Unlike [targetCameraPosition] (which is consumed once and
     * cleared so navigations can re-fire), this persists so a 3D indicator can mark where the
     * camera was last sent. Cleared explicitly via [setGotoIndicatorPosition]`(null)`.
     */
    val gotoIndicatorPosition: Cell<Vector3?> = _gotoIndicatorPosition
    val contextMenuRequest: Cell<ContextMenuRequest?> = _contextMenuRequest

    fun setMouseWorldPosition(position: Vector3?) {
        _mouseWorldPosition.value = position
    }

    fun setTargetCameraPosition(position: Vector3?) {
        _targetCameraPosition.value = position
    }

    fun setGotoIndicatorPosition(position: Vector3?) {
        _gotoIndicatorPosition.value = position
    }

    fun requestContextMenu(clientX: Int, clientY: Int) {
        _contextMenuRequest.value = ContextMenuRequest(clientX, clientY)
    }

    fun dismissContextMenu() {
        _contextMenuRequest.value = null
    }
}
