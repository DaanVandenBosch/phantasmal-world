package world.phantasmal.web.core.store

import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.test.TestApplicationUrl
import world.phantasmal.web.test.WebTestSuite
import kotlin.test.Test
import kotlin.test.assertEquals

class UiStoreTests : WebTestSuite {
    @Test
    fun applicationUrl_is_initialized_correctly() = test {
        val applicationUrl = TestApplicationUrl("/")
        val uiStore = disposer.add(UiStore(applicationUrl))

        assertEquals(PwToolType.Viewer, uiStore.currentTool.value)
        assertEquals("/${PwToolType.Viewer.slug}", applicationUrl.pathAndParams)
    }

    @Test
    fun applicationUrl_changes_when_tool_changes() = test {
        val applicationUrl = TestApplicationUrl("/")
        val uiStore = disposer.add(UiStore(applicationUrl))

        PwToolType.values().forEach { tool ->
            uiStore.setCurrentTool(tool)

            assertEquals(tool, uiStore.currentTool.value)
            assertEquals("/${tool.slug}", applicationUrl.pathAndParams)
        }
    }

    @Test
    fun applicationUrl_changes_when_path_changes() = test {
        val applicationUrl = TestApplicationUrl("/")
        val uiStore = disposer.add(UiStore(applicationUrl))

        assertEquals(PwToolType.Viewer, uiStore.currentTool.value)
        assertEquals("/${PwToolType.Viewer.slug}", applicationUrl.pathAndParams)

        listOf("/models", "/textures", "/animations").forEach { prefix ->
            uiStore.setPathPrefix(prefix, replace = false)

            assertEquals("/${PwToolType.Viewer.slug}${prefix}", applicationUrl.pathAndParams)
        }
    }

    @Test
    fun currentTool_and_path_change_when_applicationUrl_changes() = test {
        val applicationUrl = TestApplicationUrl("/")
        val uiStore = disposer.add(UiStore(applicationUrl))

        PwToolType.values().forEach { tool ->
            listOf("/a", "/b", "/c").forEach { path ->
                applicationUrl.navigate("/${tool.slug}$path")

                assertEquals(tool, uiStore.currentTool.value)
                assertEquals(path, uiStore.path.value)
            }
        }
    }

    @Test
    fun browser_navigation_stack_is_manipulated_correctly() = test {
        val appUrl = TestApplicationUrl("/")
        val uiStore = disposer.add(UiStore(appUrl))

        assertEquals("/${UiStore.DEFAULT_TOOL.slug}", appUrl.pathAndParams)

        uiStore.setCurrentTool(PwToolType.HuntOptimizer)

        assertEquals("/${PwToolType.HuntOptimizer.slug}", appUrl.pathAndParams)

        uiStore.setPathPrefix("/prefix", replace = true)

        assertEquals("/${PwToolType.HuntOptimizer.slug}/prefix", appUrl.pathAndParams)

        appUrl.back()

        assertEquals("/${UiStore.DEFAULT_TOOL.slug}", appUrl.pathAndParams)

        appUrl.forward()

        assertEquals("/${PwToolType.HuntOptimizer.slug}/prefix", appUrl.pathAndParams)
    }
}
