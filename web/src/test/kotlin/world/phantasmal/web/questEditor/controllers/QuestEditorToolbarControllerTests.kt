package world.phantasmal.web.questEditor.controllers

import org.w3c.files.File
import world.phantasmal.core.Failure
import world.phantasmal.core.Severity
import world.phantasmal.web.test.WebTestSuite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QuestEditorToolbarControllerTests : WebTestSuite() {
    @Test
    fun a_failure_is_exposed_when_openFiles_fails() = asyncTest {
        val ctrl = disposer.add(QuestEditorToolbarController(
            components.questLoader,
            components.areaStore,
            components.questEditorStore
        ))

        assertNull(ctrl.result.value)

        ctrl.openFiles(listOf(File(arrayOf(), "unknown.extension")))

        val result = ctrl.result.value

        assertTrue(result is Failure)
        assertEquals(1, result.problems.size)
        assertEquals(Severity.Error, result.problems.first().severity)
        assertEquals(
            "Please select a .qst file or one .bin and one .dat file.",
            result.problems.first().uiMessage
        )
    }
}
