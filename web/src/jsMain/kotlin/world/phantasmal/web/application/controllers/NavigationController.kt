package world.phantasmal.web.application.controllers

import kotlinx.browser.window
import world.phantasmal.cell.Cell
import world.phantasmal.cell.mutableCell
import world.phantasmal.web.core.Clock
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
        // Swatch Internet Time: 1000 beats per day from Biel Mean Time (UTC+01:00).
        // Compute without any date-time library to keep the bundle small.
        val utcSeconds = floor(clock.nowMillis() / 1000.0)
        val bielSeconds = ((utcSeconds + BIEL_OFFSET_SECONDS) % SECONDS_PER_DAY + SECONDS_PER_DAY) % SECONDS_PER_DAY
        _internetTime.value = "@" + floor(bielSeconds / SECONDS_PER_BEAT).toInt()
    }

    companion object {
        private const val BIEL_OFFSET_SECONDS = 3600.0 // UTC+01:00
        private const val SECONDS_PER_DAY = 86400.0
        private const val SECONDS_PER_BEAT = 86.4 // 86400 / 1000
    }
}
