package world.phantasmal.web.questEditor.asm.monaco

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.disposable
import world.phantasmal.web.externals.monacoEditor.IDisposable

fun IDisposable.toDisposable(): Disposable = disposable { dispose() }
