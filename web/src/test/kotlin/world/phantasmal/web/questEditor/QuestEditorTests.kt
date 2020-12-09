package world.phantasmal.web.questEditor

import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.test.TestApplicationUrl
import world.phantasmal.web.test.WebTestSuite
import kotlin.test.Test

class QuestEditorTests : WebTestSuite() {
    @Test
    fun initialization_and_shutdown_should_succeed_without_throwing() = test {
        components.applicationUrl = TestApplicationUrl("/${PwToolType.QuestEditor}")

        val questEditor = disposer.add(
            QuestEditor(components.assetLoader, components.uiStore, components.createThreeRenderer)
        )
        disposer.add(questEditor.initialize())
    }
}
