package world.phantasmal.web.core.store

import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.test.TestApplicationUrl
import world.phantasmal.web.test.WebTestSuite
import kotlin.test.Test
import kotlin.test.assertEquals

class UiStoreTests : WebTestSuite() {
    @Test
    fun applicationUrl_is_initialized_correctly() = test {
        val applicationUrl = TestApplicationUrl("/")
        val uiStore = disposer.add(UiStore(scope, applicationUrl))

        assertEquals(PwToolType.Viewer, uiStore.currentTool.value)
        assertEquals("/${PwToolType.Viewer.slug}", applicationUrl.url.value)
    }

    @Test
    fun applicationUrl_changes_when_tool_changes() = test {
        val applicationUrl = TestApplicationUrl("/")
        val uiStore = disposer.add(UiStore(scope, applicationUrl))

        PwToolType.values().forEach { tool ->
            uiStore.setCurrentTool(tool)

            assertEquals(tool, uiStore.currentTool.value)
            assertEquals("/${tool.slug}", applicationUrl.url.value)
        }
    }

    @Test
    fun applicationUrl_changes_when_path_changes() = test {
        val applicationUrl = TestApplicationUrl("/")
        val uiStore = disposer.add(UiStore(scope, applicationUrl))

        assertEquals(PwToolType.Viewer, uiStore.currentTool.value)
        assertEquals("/${PwToolType.Viewer.slug}", applicationUrl.url.value)

        listOf("/models", "/textures", "/animations").forEach { prefix ->
            uiStore.setPathPrefix(prefix, replace = false)

            assertEquals("/${PwToolType.Viewer.slug}${prefix}", applicationUrl.url.value)
        }
    }

    @Test
    fun currentTool_and_path_change_when_applicationUrl_changes() = test {
        val applicationUrl = TestApplicationUrl("/")
        val uiStore = disposer.add(UiStore(scope, applicationUrl))

        PwToolType.values().forEach { tool ->
            listOf("/a", "/b", "/c").forEach { path ->
                applicationUrl.url.value = "/${tool.slug}$path"

                assertEquals(tool, uiStore.currentTool.value)
                assertEquals(path, uiStore.path.value)
            }
        }
    }

    @Test
    fun browser_navigation_stack_is_manipulated_correctly() = test {
        val appUrl = TestApplicationUrl("/")
        val uiStore = disposer.add(UiStore(scope, appUrl))

        assertEquals("/${uiStore.defaultTool.slug}", appUrl.url.value)

        uiStore.setCurrentTool(PwToolType.HuntOptimizer)

        assertEquals("/${PwToolType.HuntOptimizer.slug}", appUrl.url.value)

        uiStore.setPathPrefix("/prefix", replace = true)

        assertEquals("/${PwToolType.HuntOptimizer.slug}/prefix", appUrl.url.value)

        appUrl.back()

        assertEquals("/${uiStore.defaultTool.slug}", appUrl.url.value)

        appUrl.forward()

        assertEquals("/${PwToolType.HuntOptimizer.slug}/prefix", appUrl.url.value)
    }
}
