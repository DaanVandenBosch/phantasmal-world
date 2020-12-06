package world.phantasmal.web.questEditor.asm

import world.phantasmal.core.asArray
import world.phantasmal.core.jsArrayOf
import world.phantasmal.lib.asm.*
import world.phantasmal.web.externals.monacoEditor.*
import world.phantasmal.webui.obj

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
    ): SignatureHelpResult? =
        getSignatureHelp(model, position)?.let { signatureHelp ->
            object : SignatureHelpResult {
                override var value: SignatureHelp = signatureHelp

                override fun dispose() {
                    // Nothing to dispose.
                }
            }
        }

    fun getSignatureHelp(model: ITextModel, position: Position): SignatureHelp? {
        // Hacky way of providing parameter hints.
        // We just tokenize the current line and look for the first identifier and check whether
        // it's a valid opcode.
        var signatureInfo: SignatureInformation? = null
        var activeParam = -1
        val line = model.getLineContent(position.lineNumber)

        val tokens = tokenizeLine(line)

        tokens.find { it is Token.Ident }?.let { ident ->
            ident as Token.Ident

            mnemonicToOpcode(ident.value)?.let { opcode ->
                signatureInfo = getSignatureInformation(opcode)

                for (tkn in tokens) {
                    if (tkn.col + tkn.len > position.column) {
                        break
                    } else if (tkn is Token.Ident && activeParam == -1) {
                        activeParam = 0
                    } else if (tkn is Token.ArgSeparator) {
                        activeParam++
                    }
                }
            }
        }

        return signatureInfo?.let { sigInfo ->
            obj<SignatureHelp> {
                signatures = arrayOf(sigInfo)
                activeSignature = 0
                activeParameter = activeParam
            }
        }
    }

    private fun getSignatureInformation(opcode: Opcode): SignatureInformation {
        var signature = opcode.mnemonic + " "
        val params = jsArrayOf<ParameterInformation>()
        var first = true

        for (param in opcode.params) {
            if (first) {
                first = false
            } else {
                signature += ", "
            }

            val paramTypeStr = when (param.type) {
                ByteType -> "Byte"
                ShortType -> "Short"
                IntType -> "Int"
                FloatType -> "Float"
                ILabelType -> "&Function"
                DLabelType -> "&Data"
                SLabelType -> "&String"
                ILabelVarType -> "...&Function"
                StringType -> "String"
                RegRefType, is RegTupRefType -> "Register"
                RegRefVarType -> "...Register"
                PointerType -> "Pointer"
                else -> "Any"
            }

            params.push(
                obj {
                    label = arrayOf(signature.length, signature.length + paramTypeStr.length)
                    param.doc?.let { documentation = it }
                }
            )

            signature += paramTypeStr
        }

        return obj {
            label = signature
            opcode.doc?.let { documentation = it }
            parameters = params.asArray()
        }
    }
}
