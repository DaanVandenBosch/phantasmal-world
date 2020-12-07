package world.phantasmal.web.questEditor.asm

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import world.phantasmal.web.externals.monacoEditor.*
import world.phantasmal.webui.obj
import kotlin.js.Promise
import world.phantasmal.web.externals.monacoEditor.SignatureHelp as MonacoSigHelp

object AsmSignatureHelpProvider : SignatureHelpProvider {
    override val signatureHelpTriggerCharacters: Array<String> =
        arrayOf(" ", ",")

    override val signatureHelpRetriggerCharacters: Array<String> =
        arrayOf(", ")

    override fun provideSignatureHelp(
        model: ITextModel,
        position: Position,
        token: CancellationToken,
        context: SignatureHelpContext,
    ): Promise<SignatureHelpResult?> =
        GlobalScope.promise {
            AsmAnalyser.getSignatureHelp(position.lineNumber, position.column)
                ?.let { sigHelp ->
                    val monacoSigHelp = obj<MonacoSigHelp> {
                        signatures = arrayOf(
                            obj {
                                label = sigHelp.signature.label
                                sigHelp.signature.documentation?.let { documentation = it }
                                parameters = sigHelp.signature.parameters.map { param ->
                                    obj<ParameterInformation> {
                                        label = arrayOf(param.labelStart, param.labelEnd)
                                        param.documentation?.let { documentation = it }
                                    }
                                }.toTypedArray()
                            }
                        )
                        activeSignature = 0
                        activeParameter = sigHelp.activeParameter
                    }

                    object : SignatureHelpResult {
                        override var value = monacoSigHelp

                        override fun dispose() {
                            // Nothing to dispose.
                        }
                    }
                }
        }
}
