package world.phantasmal.web.questEditor.rendering

import world.phantasmal.psolib.fileFormats.quest.ObjectType
import world.phantasmal.web.externals.three.Vector3
import world.phantasmal.web.questEditor.models.QuestModel
import world.phantasmal.web.questEditor.models.QuestObjectModel

/**
 * Calculates room centers based on ObjRoomID objects and section geometry.
 */
class RoomCenterCalculator {
    
    /**
     * Calculates room centers by analyzing actual room content, not boundary markers.
     * ObjRoomID objects are boundary markers and should not be used as room centers.
     * Returns a map of section ID to geometric center position.
     */
    fun calculateRoomCenters(quest: QuestModel): Map<Int, Vector3> {
        val roomCenters = mutableMapOf<Int, Vector3>()
        
        // Group all meaningful objects by their section ID (not area ID)
        // Exclude PlayerSet (player spawn points) and ObjRoomID (boundary markers)
        val objectsBySection = mutableMapOf<Int, MutableList<QuestObjectModel>>()
        
        for (obj in quest.objects.value) {
            // Skip objects that don't define room content
            if (obj.type == ObjectType.PlayerSet || obj.type == ObjectType.ObjRoomID) continue
            
            val sectionId = obj.sectionId.value
            if (sectionId >= 0) {
                if (!objectsBySection.containsKey(sectionId)) {
                    objectsBySection[sectionId] = mutableListOf()
                }
                objectsBySection[sectionId]!!.add(obj)
            }
        }
        
        // Calculate geometric center for each section based on actual room objects
        for ((sectionId, objects) in objectsBySection) {
            if (objects.isNotEmpty()) {
                val center = calculateCenterFromObjects(objects)
                roomCenters[sectionId] = center
            }
        }
        
        return roomCenters
    }
    
    /**
     * Fallback method to calculate room centers by area when section data is not available.
     */
    private fun calculateFallbackRoomCenters(quest: QuestModel): Map<Int, Vector3> {
        val roomCenters = mutableMapOf<Int, Vector3>()
        val objectsByArea = mutableMapOf<Int, MutableList<QuestObjectModel>>()
        
        for (obj in quest.objects.value) {
            // Skip PlayerSet and ObjRoomID objects - they don't define room content
            if (obj.type == ObjectType.PlayerSet || obj.type == ObjectType.ObjRoomID) continue
            
            val areaId = obj.areaId
            if (areaId >= 0) {
                if (!objectsByArea.containsKey(areaId)) {
                    objectsByArea[areaId] = mutableListOf()
                }
                objectsByArea[areaId]!!.add(obj)
            }
        }
        
        // Calculate center for each area and map to section IDs
        for ((areaId, objects) in objectsByArea) {
            if (objects.isNotEmpty()) {
                val center = calculateCenterFromObjects(objects)
                // Use area ID as room ID for fallback
                roomCenters[areaId] = center
            }
        }
        
        return roomCenters
    }
    
    /**
     * Extracts section ID from an object's area ID or section information.
     */
    private fun extractSectionId(obj: QuestObjectModel): Int {
        // For now, use area ID as section ID
        // In PSO, sections are typically numbered within areas
        return obj.areaId
    }
    
    /**
     * Extracts the "Next section" room ID from ObjRoomID object.
     */
    private fun extractNextSectionId(roomIdObj: QuestObjectModel): Int {
        val nextSectionProp = roomIdObj.properties.value.find { it.name == "Next section" }
        if (nextSectionProp != null) {
            val value = nextSectionProp.value.value as? Float
            if (value != null && value >= 0) {
                return value.toInt()
            }
        }
        return -1
    }
    
    /**
     * Extracts the "Previous section" room ID from ObjRoomID object.
     */
    private fun extractPreviousSectionId(roomIdObj: QuestObjectModel): Int {
        val prevSectionProp = roomIdObj.properties.value.find { it.name == "Previous section" }
        if (prevSectionProp != null) {
            val value = prevSectionProp.value.value as? Float
            if (value != null && value >= 0) {
                return value.toInt()
            }
        }
        return -1
    }
    
    /**
     * Extracts the actual radius from SCL_TAMA property (multiply by 10 for real radius).
     */
    private fun extractRadiusFromObject(roomIdObj: QuestObjectModel): Float {
        val sclTamaProp = roomIdObj.properties.value.find { it.name == "SCL_TAMA" }
        if (sclTamaProp != null) {
            val value = sclTamaProp.value.value as? Float
            if (value != null && value >= 0) {
                // SCL_TAMA is 1/10th of the actual radius
                return value * 10.0f
            }
        }
        return 0.0f
    }
    
    /**
     * Calculates the geometric center from a list of objects.
     */
    private fun calculateCenterFromObjects(objects: List<QuestObjectModel>): Vector3 {
        if (objects.isEmpty()) {
            return Vector3(0.0, 0.0, 0.0)
        }
        
        var totalX = 0.0
        var totalY = 0.0  
        var totalZ = 0.0
        var count = 0
        
        // Calculate average position of all objects in the section
        for (obj in objects) {
            val pos = obj.worldPosition.value
            totalX += pos.x
            totalY += pos.y
            totalZ += pos.z
            count++
        }
        
        return Vector3(
            totalX / count,
            totalY / count,
            totalZ / count
        )
    }
    
    /**
     * Calculates a single room center for a specific section ID.
     */
    fun calculateRoomCenter(quest: QuestModel, sectionId: Int): Vector3? {
        val centers = calculateRoomCenters(quest)
        return centers[sectionId]
    }
}