package world.phantasmal.web.questEditor.models

import world.phantasmal.lib.fileFormats.quest.ObjectType
import world.phantasmal.lib.fileFormats.quest.QuestObject
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.mutableVal

class QuestObjectModel(obj: QuestObject) : QuestEntityModel<ObjectType, QuestObject>(obj) {
    private val _model = mutableVal(obj.model)

    val model: Val<Int?> = _model

    fun setModel(model: Int, propagateToProps: Boolean = true) {
        _model.value = model

        if (propagateToProps) {
            val props = when (type) {
                ObjectType.Probe ->
                    properties.value.filter { it.offset == 40 }

                ObjectType.Saw,
                ObjectType.LaserDetect,
                ->
                    properties.value.filter { it.offset == 48 }

                ObjectType.Sonic,
                ObjectType.LittleCryotube,
                ObjectType.Cactus,
                ObjectType.BigBrownRock,
                ObjectType.BigBlackRocks,
                ObjectType.BeeHive,
                ->
                    properties.value.filter { it.offset == 52 }

                ObjectType.ForestConsole ->
                    properties.value.filter { it.offset == 56 }

                ObjectType.PrincipalWarp,
                ObjectType.LaserFence,
                ObjectType.LaserSquareFence,
                ObjectType.LaserFenceEx,
                ObjectType.LaserSquareFenceEx,
                ->
                    properties.value.filter { it.offset == 60 }

                else -> return
            }

            for (prop in props) {
                prop.setValue(model, propagateToEntity = false)
            }
        }
    }
}
