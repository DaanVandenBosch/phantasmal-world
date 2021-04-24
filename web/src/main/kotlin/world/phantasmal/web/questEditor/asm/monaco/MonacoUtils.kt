package world.phantasmal.web.questEditor.asm.monaco

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.disposable
import world.phantasmal.web.externals.monacoEditor.IDisposable
import world.phantasmal.web.externals.monacoEditor.IRange
import world.phantasmal.web.shared.messages.AsmRange
import world.phantasmal.webui.obj

fun IDisposable.toDisposable(): Disposable = disposable { dispose() }

fun AsmRange.toIRange(): IRange =
    obj {
        startLineNumber = startLineNo
        startColumn = startCol
        endLineNumber = endLineNo
        endColumn = endCol
    }
