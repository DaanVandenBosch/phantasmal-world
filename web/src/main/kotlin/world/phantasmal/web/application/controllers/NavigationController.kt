package world.phantasmal.web.application.controllers

import kotlinx.browser.window
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.mutableVal
import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.webui.controllers.Controller
import kotlin.math.floor

class NavigationController(private val uiStore: UiStore, private val clock: Clock) : Controller() {
    private val _internetTime = mutableVal("@")
    private var internetTimeInterval: Int

    val tools: Map<PwToolType, Val<Boolean>> = uiStore.toolToActive
    val internetTime: Val<String> = _internetTime

    init {
        internetTimeInterval = window.setInterval(::updateInternetTime, 1000)
        updateInternetTime()
    }

    override fun internalDispose() {
        window.clearInterval(internetTimeInterval)
        super.internalDispose()
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
