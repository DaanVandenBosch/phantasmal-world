package world.phantasmal.web.questEditor.models

import world.phantasmal.lib.fileFormats.quest.ObjectType
import world.phantasmal.lib.fileFormats.quest.QuestObject
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.mutableVal

class QuestObjectModel(obj: QuestObject) : QuestEntityModel<ObjectType, QuestObject>(obj) {
    private val _model = mutableVal(obj.model)

    val model: Val<Int?> = _model

    fun setModel(model: Int) {
        _model.value = model

        // TODO: Propagate to props.
    }
}
