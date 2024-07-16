package world.phantasmal.web.application.controllers

import kotlinx.browser.window
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import world.phantasmal.cell.Cell
import world.phantasmal.cell.mutableCell
import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.webui.controllers.Controller
import kotlin.math.floor

class NavigationController(private val uiStore: UiStore, private val clock: Clock) : Controller() {
    private val _internetTime = mutableCell("@")
    private var internetTimeInterval: Int

    val tools: Map<PwToolType, Cell<Boolean>> = uiStore.toolToActive
    val internetTime: Cell<String> = _internetTime

    init {
        internetTimeInterval = window.setInterval(::updateInternetTime, 1000)
        updateInternetTime()
    }

    override fun dispose() {
        window.clearInterval(internetTimeInterval)
        super.dispose()
    }

    fun setCurrentTool(tool: PwToolType) {
        uiStore.setCurrentTool(tool)
    }

    private fun updateInternetTime() {
        val now = clock.now().toLocalDateTime(INTERNET_TIME_TZ)
        _internetTime.value = "@" + floor((now.second + 60 * (now.minute + 60 * now.hour)) / 86.4)
    }

    companion object {
        /**
         * Internet time is calculated from UTC+01:00.
         */
        private val INTERNET_TIME_TZ = TimeZone.of("UTC+01:00")
    }
}
