package world.phantasmal.lib.asm

import world.phantasmal.lib.test.LibTestSuite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class OpcodeTests : LibTestSuite {
    // We do these checks in a unit test instead of in the Opcode constructor to avoid the runtime
    // overhead. This is static data that is built up once and only needs to be verified once.
    @Test
    fun all_opcodes_are_consistent() = test {
        for (code in (0x00..0xFF).asSequence() + (0xF800..0xF8FF) + (0xF900..0xF9FF)) {
            val opcode = codeToOpcode(code)

            assertEquals(code, opcode.code)
            assertTrue(opcode.mnemonic.isNotBlank())
            assertTrue(opcode.doc == null || opcode.doc!!.isNotBlank())
            // If an opcodes pushes something onto the stack, it needs at least one immediate
            // argument. If an opcode pops the stack, it needs at least one stack argument.
            assertTrue(opcode.stack == null || opcode.params.isNotEmpty())

            // Varargs.
            val varargCount = opcode.params.count { it.varargs }
            val hasVarargs = varargCount >= 1
            // Only the last parameter can be variadic.
            assertTrue(varargCount <= 1)
            assertTrue(!hasVarargs || opcode.params.lastOrNull()?.varargs == true)
            assertEquals(hasVarargs, opcode.varargs)

            for (param in opcode.params) {
                val type = param.type

                // Any should only be used with register parameters.
                assertNotEquals(AnyType.Instance, type)

                // Register references.
                if (type is RegType) {
                    val registers = type.registers

                    if (registers == null) {
                        assertTrue(param.read || param.write)
                    } else {
                        assertTrue(registers.isNotEmpty())

                        for (register in registers) {
                            assertTrue(register.read || register.write)
                        }
                    }
                }
            }
        }
    }
}
