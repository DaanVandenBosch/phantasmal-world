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
    private val _contextMenuRequest: MutableCell<ContextMenuRequest?> = mutableCell(null)

    val mouseWorldPosition: Cell<Vector3?> = _mouseWorldPosition
    val targetCameraPosition: Cell<Vector3?> = _targetCameraPosition
    val contextMenuRequest: Cell<ContextMenuRequest?> = _contextMenuRequest

    fun setMouseWorldPosition(position: Vector3?) {
        _mouseWorldPosition.value = position
    }

    fun setTargetCameraPosition(position: Vector3?) {
        _targetCameraPosition.value = position
    }

    fun requestContextMenu(clientX: Int, clientY: Int) {
        _contextMenuRequest.value = ContextMenuRequest(clientX, clientY)
    }

    fun dismissContextMenu() {
        _contextMenuRequest.value = null
    }
}
