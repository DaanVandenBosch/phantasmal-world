package world.phantasmal.web.questEditor

import world.phantasmal.web.externals.babylon.Engine
import world.phantasmal.web.test.WebTestSuite
import kotlin.test.Test

class QuestEditorTests : WebTestSuite() {
    @Test
    fun initialization_and_shutdown_should_succeed_without_throwing() = test {
        val questEditor = disposer.add(
            QuestEditor(components.assetLoader, components.uiStore, createEngine = { Engine(it) })
        )
        disposer.add(questEditor.initialize(scope))
    }
}
