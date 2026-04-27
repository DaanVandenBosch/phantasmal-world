package world.phantasmal.psolib.symbolchat

import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.test.LibTestSuite
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [SymbolChatColliTable] — parsing the bundled `symbolchatcolli.bin`.
 *
 * Run with: ./gradlew :psolib:jvmTest --tests "*.SymbolChatColliTableTest"
 */
class SymbolChatColliTableTest : LibTestSuite {

    private fun loadTable(): SymbolChatColliTable {
        val projectRoot = File(System.getProperty("user.dir")).let {
            if (it.name == "psolib") it.parentFile else it
        }
        val file = File(projectRoot, "web/src/jsMain/resources/assets/symbol_chat/symbolchatcolli.bin")
        assertTrue(file.exists(), "symbolchatcolli.bin not found: ${file.absolutePath}")
        val buf = Buffer.fromByteArray(file.readBytes())
        assertTrue(buf.size >= SymbolChatColliTable.FILE_SIZE,
            "File too small: ${buf.size} < ${SymbolChatColliTable.FILE_SIZE}")
        return SymbolChatColliTable(buf)
    }

    @Test
    fun file_size_is_exactly_2496_bytes() = testAsync {
        val projectRoot = File(System.getProperty("user.dir")).let {
            if (it.name == "psolib") it.parentFile else it
        }
        val file = File(projectRoot, "web/src/jsMain/resources/assets/symbol_chat/symbolchatcolli.bin")
        assertTrue(file.exists())
        val bytes = file.readBytes()
        assertEquals(SymbolChatColliTable.FILE_SIZE, bytes.size,
            "Expected exactly ${SymbolChatColliTable.FILE_SIZE} bytes, got ${bytes.size}")
    }

    @Test
    fun all_24_entries_are_non_null() = testAsync {
        val table = loadTable()
        for (id in 0 until 24) {
            assertNotNull(table.entry(id), "Entry $id should not be null")
        }
    }

    @Test
    fun each_entry_is_60_bytes() = testAsync {
        val table = loadTable()
        for (id in 0 until 24) {
            val entry = table.entry(id)!!
            assertEquals(SymbolChatColliTable.SYMBOL_CHAT_SIZE, entry.size,
                "Entry $id should be ${SymbolChatColliTable.SYMBOL_CHAT_SIZE} bytes, got ${entry.size}")
        }
    }

    @Test
    fun out_of_range_ids_return_null() = testAsync {
        val table = loadTable()
        assertNull(table.entry(-1), "id=-1 should return null")
        assertNull(table.entry(24), "id=24 should return null")
        assertNull(table.entry(30), "id=30 should return null")
        assertNull(table.entry(100), "id=100 should return null")
    }

    @Test
    fun face_field_has_valid_shape_and_color() = testAsync {
        val table = loadTable()
        for (id in 0 until 24) {
            val buf = table.entry(id)!!
            val face = buf.getInt(0)
            val shape = face and 3
            val color = (face shr 2) and 7
            assertTrue(shape in 0..3, "Entry $id: face shape $shape out of range 0..3")
            assertTrue(color in 0..7, "Entry $id: face color $color out of range 0..7")
        }
    }
}
