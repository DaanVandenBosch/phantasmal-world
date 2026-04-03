package world.phantasmal.web.questEditor.asm.monaco

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.promise
import world.phantasmal.web.externals.monacoEditor.*
import world.phantasmal.web.questEditor.asm.AsmAnalyser
import kotlin.js.Promise

/**
 * Creates a plain JS object implementing [DocumentSymbolProvider] to avoid Monaco's objectHash
 * stack overflow (monaco-editor#2586). Kotlin class instances have deep prototype chains that
 * cause infinite recursion when Monaco tries to hash the provider object.
 */
fun createDocumentSymbolProvider(scope: CoroutineScope, asmAnalyser: AsmAnalyser): DocumentSymbolProvider {

    val provider: dynamic = js("({})")
    provider.displayName = null

    provider.provideDocumentSymbols = { model: ITextModel, token: CancellationToken ->
        scope.promise {
            val labels = asmAnalyser.getLabels()

            val result: dynamic = js("[]")
            for (label in labels) {
                val r = label.range
                val sym: dynamic = js("({})")
                sym.name = "${label.name}"
                sym.detail = ""
                sym.kind = 11 // SymbolKind.Function
                sym.tags = js("[]")
                val range: dynamic = js("({})")
                range.startLineNumber = r.startLineNo
                range.startColumn = r.startCol
                range.endLineNumber = r.endLineNo
                range.endColumn = r.endCol
                sym.range = range
                val selRange: dynamic = js("({})")
                selRange.startLineNumber = r.startLineNo
                selRange.startColumn = r.startCol
                selRange.endLineNumber = r.endLineNo
                selRange.endColumn = r.endCol
                sym.selectionRange = selRange
                sym.children = null
                result.push(sym)
            }

            @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
            result as Array<DocumentSymbol>
        }
    }

    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    return provider as DocumentSymbolProvider
}
