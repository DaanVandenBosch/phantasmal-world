package world.phantasmal.web.assemblyWorker.test

import world.phantasmal.testUtils.assertDeepEquals
import world.phantasmal.web.shared.messages.Parameter
import world.phantasmal.web.shared.messages.Response
import world.phantasmal.web.shared.messages.Signature
import world.phantasmal.web.shared.messages.SignatureHelp
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

fun assertDeepEquals(
    expected: Response.GetSignatureHelp,
    actual: Response.GetSignatureHelp,
    message: String? = null,
) {
    assertEquals(expected.id, actual.id, message)

    if (expected.result == null) {
        assertNull(actual.result, message)
    } else {
        assertNotNull(actual.result, message)
        assertDeepEquals(expected.result!!, actual.result!!, message)
    }
}

fun assertDeepEquals(expected: SignatureHelp, actual: SignatureHelp, message: String? = null) {
    assertDeepEquals(expected.signature, actual.signature, message)
    assertEquals(expected.activeParameter, actual.activeParameter, message)
}

fun assertDeepEquals(expected: Signature, actual: Signature, message: String? = null) {
    assertEquals(expected.label, actual.label, message)
    assertEquals(expected.documentation, actual.documentation, message)
    assertDeepEquals(expected.parameters, actual.parameters, ::assertDeepEquals, message)
}

fun assertDeepEquals(expected: Parameter, actual: Parameter, message: String? = null) {
    assertEquals(expected.labelStart, actual.labelStart, message)
    assertEquals(expected.labelEnd, actual.labelEnd, message)
    assertEquals(expected.documentation, actual.documentation, message)
}
