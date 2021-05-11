package world.phantasmal.lib.fileFormats

import world.phantasmal.lib.test.LibTestSuite
import world.phantasmal.lib.test.readFile
import world.phantasmal.testUtils.assertCloseTo
import kotlin.test.Test
import kotlin.test.assertEquals

class AreaCollisionGeometryTests : LibTestSuite {
    @Test
    fun parse_forest_1() = testAsync {
        val obj = parseAreaCollisionGeometry(readFile("/map_forest01c.rel"))

        assertEquals(69, obj.meshes.size)
        assertEquals(11, obj.meshes[0].vertices.size)
        assertCloseTo(-589.5195f, obj.meshes[0].vertices[0].x)
        assertCloseTo(16.7166f, obj.meshes[0].vertices[0].y)
        assertCloseTo(-218.6852f, obj.meshes[0].vertices[0].z)
        assertEquals(12, obj.meshes[0].triangles.size)
        assertEquals(0b100000001, obj.meshes[0].triangles[0].flags)
        assertEquals(5, obj.meshes[0].triangles[0].index1)
        assertEquals(0, obj.meshes[0].triangles[0].index2)
        assertEquals(7, obj.meshes[0].triangles[0].index3)
        assertCloseTo(0.0137f, obj.meshes[0].triangles[0].normal.x)
        assertCloseTo(0.9994f, obj.meshes[0].triangles[0].normal.y)
        assertCloseTo(-0.0307f, obj.meshes[0].triangles[0].normal.z)
    }
}
