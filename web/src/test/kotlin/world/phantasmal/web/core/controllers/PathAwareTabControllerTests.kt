package world.phantasmal.web.core.controllers

import world.phantasmal.testUtils.TestSuite
import world.phantasmal.web.core.stores.PwTool
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.test.TestApplicationUrl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class PathAwareTabControllerTests : TestSuite() {
    @Test
    fun activeTab_is_initialized_correctly() {
        setup { ctrl, appUrl ->
            assertEquals("/b", ctrl.activeTab.value?.path)
            assertFalse(appUrl.canGoBack)
            assertFalse(appUrl.canGoForward)
        }
    }

    @Test
    fun applicationUrl_changes_when_activeTab_changes() {
        setup { ctrl, appUrl ->
            ctrl.setActiveTab(ctrl.tabs[2])

            assertEquals("/${PwTool.HuntOptimizer.slug}/c", appUrl.url.value)
            assertEquals(1, appUrl.historyEntries)
            assertFalse(appUrl.canGoForward)
        }
    }

    @Test
    fun activeTab_changes_when_applicationUrl_changes() {
        setup { ctrl, applicationUrl ->
            applicationUrl.pushUrl("/${PwTool.HuntOptimizer.slug}/c")

            assertEquals("/c", ctrl.activeTab.value?.path)
        }
    }

    @Test
    fun applicationUrl_changes_when_switch_to_tool_with_tabs() {
        val appUrl = TestApplicationUrl("/")
        val uiStore = UiStore(scope, appUrl)

        PathAwareTabController(scope, uiStore, PwTool.HuntOptimizer, listOf(
            PathAwareTab("A", "/a"),
            PathAwareTab("B", "/b"),
            PathAwareTab("C", "/c"),
        ))

        assertFalse(appUrl.canGoBack)
        assertFalse(appUrl.canGoForward)
        assertEquals("/${uiStore.defaultTool.slug}", appUrl.url.value)

        uiStore.setCurrentTool(PwTool.HuntOptimizer)

        assertEquals(1, appUrl.historyEntries)
        assertFalse(appUrl.canGoForward)
        assertEquals("/${PwTool.HuntOptimizer.slug}", appUrl.url.value)

        appUrl.back()

        assertEquals("/${uiStore.defaultTool.slug}", appUrl.url.value)
    }

    private fun setup(
        block: (PathAwareTabController<PathAwareTab>, applicationUrl: TestApplicationUrl) -> Unit,
    ) {
        val applicationUrl = TestApplicationUrl("/${PwTool.HuntOptimizer.slug}/b")
        val uiStore = UiStore(scope, applicationUrl)
        uiStore.setCurrentTool(PwTool.HuntOptimizer)

        val ctrl = PathAwareTabController(scope, uiStore, PwTool.HuntOptimizer, listOf(
            PathAwareTab("A", "/a"),
            PathAwareTab("B", "/b"),
            PathAwareTab("C", "/c"),
        ))

        block(ctrl, applicationUrl)
    }
}
