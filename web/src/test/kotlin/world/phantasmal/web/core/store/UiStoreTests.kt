package world.phantasmal.web.core.store

import world.phantasmal.testUtils.TestSuite
import world.phantasmal.web.core.stores.PwTool
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.test.TestApplicationUrl
import kotlin.test.Test
import kotlin.test.assertEquals

class UiStoreTests : TestSuite() {
    @Test
    fun applicationUrl_is_initialized_correctly() {
        val applicationUrl = TestApplicationUrl("/")
        val uiStore = UiStore(scope, applicationUrl)

        assertEquals(PwTool.Viewer, uiStore.currentTool.value)
        assertEquals("/${PwTool.Viewer.slug}", applicationUrl.url.value)
    }

    @Test
    fun applicationUrl_changes_when_tool_changes() {
        val applicationUrl = TestApplicationUrl("/")
        val uiStore = UiStore(scope, applicationUrl)

        PwTool.values().forEach { tool ->
            uiStore.setCurrentTool(tool)

            assertEquals(tool, uiStore.currentTool.value)
            assertEquals("/${tool.slug}", applicationUrl.url.value)
        }
    }

    @Test
    fun applicationUrl_changes_when_path_changes() {
        val applicationUrl = TestApplicationUrl("/")
        val uiStore = UiStore(scope, applicationUrl)

        assertEquals(PwTool.Viewer, uiStore.currentTool.value)
        assertEquals("/${PwTool.Viewer.slug}", applicationUrl.url.value)

        listOf("/models", "/textures", "/animations").forEach { prefix ->
            uiStore.setPathPrefix(prefix, replace = false)

            assertEquals("/${PwTool.Viewer.slug}${prefix}", applicationUrl.url.value)
        }
    }

    @Test
    fun currentTool_and_path_change_when_applicationUrl_changes() {
        val applicationUrl = TestApplicationUrl("/")
        val uiStore = UiStore(scope, applicationUrl)

        PwTool.values().forEach { tool ->
            listOf("/a", "/b", "/c").forEach { path ->
                applicationUrl.url.value = "/${tool.slug}$path"

                assertEquals(tool, uiStore.currentTool.value)
                assertEquals(path, uiStore.path.value)
            }
        }
    }

    @Test
    fun browser_navigation_stack_is_manipulated_correctly() {
        val appUrl = TestApplicationUrl("/")
        val uiStore = UiStore(scope, appUrl)

        assertEquals("/${uiStore.defaultTool.slug}", appUrl.url.value)

        uiStore.setCurrentTool(PwTool.HuntOptimizer)

        assertEquals("/${PwTool.HuntOptimizer.slug}", appUrl.url.value)

        uiStore.setPathPrefix("/prefix", replace = true)

        assertEquals("/${PwTool.HuntOptimizer.slug}/prefix", appUrl.url.value)

        appUrl.back()

        assertEquals("/${uiStore.defaultTool.slug}", appUrl.url.value)

        appUrl.forward()

        assertEquals("/${PwTool.HuntOptimizer.slug}/prefix", appUrl.url.value)
    }
}
