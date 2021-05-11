package world.phantasmal.web.questEditor.asm.monaco

import world.phantasmal.web.externals.monacoEditor.*
import world.phantasmal.webui.obj
import kotlin.js.RegExp

object AsmLanguageConfiguration : LanguageConfiguration {
    override var indentationRules: IndentationRule? =
        obj<IndentationRule> {
            increaseIndentPattern = RegExp("""^\s*\d+:""")
            decreaseIndentPattern = RegExp("""^\s*(\d+|\.)""")
        }

    override var autoClosingPairs: Array<IAutoClosingPairConditional>? =
        arrayOf(obj { open = "\""; close = "\"" })

    override var surroundingPairs: Array<IAutoClosingPair>? =
        arrayOf(obj { open = "\""; close = "\"" })

    override var comments: CommentRule? =
        obj<CommentRule> { lineComment = "//" }
}
