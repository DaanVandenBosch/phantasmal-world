package world.phantasmal.web.questEditor.asm

import world.phantasmal.core.asArray
import world.phantasmal.core.jsArrayOf
import world.phantasmal.web.externals.monacoEditor.*
import world.phantasmal.webui.obj

object AsmHoverProvider : HoverProvider {
    override fun provideHover(
        model: ITextModel,
        position: Position,
        token: CancellationToken,
    ): Hover? {
        val help = AsmSignatureHelpProvider.getSignatureHelp(model, position)
            ?: return null

        val sig = help.signatures[help.activeSignature]
        val param = sig.parameters.getOrNull(help.activeParameter)

        val contents = jsArrayOf<IMarkdownString>()

        // Instruction signature. Parameter highlighted if possible.
        contents.push(
            obj {
                value =
                    if (param == null) {
                        sig.label
                    } else {
                        // TODO: Figure out how to underline the active parameter in addition to
                        //  bolding it to make it match the look of the signature help.
                        sig.label.substring(0, param.label[0]) +
                                "__" +
                                sig.label.substring(param.label[0], param.label[1]) +
                                "__" +
                                sig.label.substring(param.label[1])
                    }
            }
        )

        // Put the parameter doc and the instruction doc in the same string to match the look of the
        // signature help.
        var doc = ""

        // Parameter doc.
        if (param?.documentation != null) {
            doc += param.documentation

            // TODO: Figure out how add an empty line here to make it match the look of the
            //  signature help.
            doc += "\n\n"
        }

        // Instruction doc.
        sig.documentation?.let { doc += it }

        contents.push(obj { value = doc })

        return obj<Hover> { this.contents = contents.asArray() }
    }
}
