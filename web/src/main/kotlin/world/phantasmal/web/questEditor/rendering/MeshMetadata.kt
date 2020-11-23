package world.phantasmal.web.questEditor.rendering

import world.phantasmal.web.questEditor.models.QuestEntityModel

class EntityMetadata(val entity: QuestEntityModel<*, *>)

interface CollisionUserData {
    var collisionMesh: Boolean
}
