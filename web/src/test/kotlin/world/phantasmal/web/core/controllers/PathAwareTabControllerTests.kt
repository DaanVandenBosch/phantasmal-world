package world.phantasmal.web.core.controllers

import world.phantasmal.testUtils.TestContext
import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.test.TestApplicationUrl
import world.phantasmal.web.test.WebTestSuite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class PathAwareTabControllerTests : WebTestSuite {
    @Test
    fun activeTab_is_initialized_correctly() = test {
        setup { ctrl, appUrl ->
            assertEquals("/b", ctrl.activeTab.value?.path)
            assertFalse(appUrl.canGoBack)
            assertFalse(appUrl.canGoForward)
        }
    }

    @Test
    fun applicationUrl_changes_when_activeTab_changes() = test {
        setup { ctrl, appUrl ->
            ctrl.setActiveTab(ctrl.tabs[2])

            assertEquals("/${PwToolType.HuntOptimizer.slug}/c", appUrl.url.value)
            assertEquals(1, appUrl.historyEntries)
            assertFalse(appUrl.canGoForward)
        }
    }

    @Test
    fun activeTab_changes_when_applicationUrl_changes() = test {
        setup { ctrl, applicationUrl ->
            applicationUrl.pushUrl("/${PwToolType.HuntOptimizer.slug}/c")

            assertEquals("/c", ctrl.activeTab.value?.path)
        }
    }

    @Test
    fun applicationUrl_changes_when_switch_to_tool_with_tabs() = test {
        val appUrl = TestApplicationUrl("/")
        val uiStore = disposer.add(UiStore(appUrl))

        disposer.add(
            PathAwareTabContainerController(uiStore, PwToolType.HuntOptimizer, listOf(
                PathAwareTab("A", "/a"),
                PathAwareTab("B", "/b"),
                PathAwareTab("C", "/c"),
            ))
        )

        assertFalse(appUrl.canGoBack)
        assertFalse(appUrl.canGoForward)
        assertEquals("/${uiStore.defaultTool.slug}", appUrl.url.value)

        uiStore.setCurrentTool(PwToolType.HuntOptimizer)

        assertEquals(1, appUrl.historyEntries)
        assertFalse(appUrl.canGoForward)
        assertEquals("/${PwToolType.HuntOptimizer.slug}", appUrl.url.value)

        appUrl.back()

        assertEquals("/${uiStore.defaultTool.slug}", appUrl.url.value)
    }

    private fun TestContext.setup(
        block: (PathAwareTabContainerController<PathAwareTab>, applicationUrl: TestApplicationUrl) -> Unit,
    ) {
        val applicationUrl = TestApplicationUrl("/${PwToolType.HuntOptimizer.slug}/b")
        val uiStore = disposer.add(UiStore(applicationUrl))
        uiStore.setCurrentTool(PwToolType.HuntOptimizer)

        val ctrl = disposer.add(
            PathAwareTabContainerController(uiStore, PwToolType.HuntOptimizer, listOf(
                PathAwareTab("A", "/a"),
                PathAwareTab("B", "/b"),
                PathAwareTab("C", "/c"),
            ))
        )

        block(ctrl, applicationUrl)
    }
}
