@file:JsModule("monaco-editor")
@file:JsNonModule
@file:JsQualifier("languages")

package world.phantasmal.web.externals.monacoEditor

import kotlin.js.RegExp

external fun register(language: ILanguageExtensionPoint)

external fun setLanguageConfiguration(
    languageId: String,
    configuration: LanguageConfiguration,
): IDisposable

external fun setMonarchTokensProvider(
    languageId: String,
    languageDef: IMonarchLanguage,
): IDisposable

external interface CommentRule {
    var lineComment: String?
        get() = definedExternally
        set(value) = definedExternally
    var blockComment: dynamic /* JsTuple<String, String> */
        get() = definedExternally
        set(value) = definedExternally
}

external interface LanguageConfiguration {
    var comments: CommentRule?
        get() = definedExternally
        set(value) = definedExternally
    var brackets: Array<dynamic /* JsTuple<String, String> */>?
        get() = definedExternally
        set(value) = definedExternally
    var wordPattern: RegExp?
        get() = definedExternally
        set(value) = definedExternally
    var indentationRules: IndentationRule?
        get() = definedExternally
        set(value) = definedExternally
    var onEnterRules: Array<OnEnterRule>?
        get() = definedExternally
        set(value) = definedExternally
    var autoClosingPairs: Array<IAutoClosingPairConditional>?
        get() = definedExternally
        set(value) = definedExternally
    var surroundingPairs: Array<IAutoClosingPair>?
        get() = definedExternally
        set(value) = definedExternally
    var autoCloseBefore: String?
        get() = definedExternally
        set(value) = definedExternally
    var folding: FoldingRules?
        get() = definedExternally
        set(value) = definedExternally
}

external interface IndentationRule {
    var decreaseIndentPattern: RegExp
    var increaseIndentPattern: RegExp
    var indentNextLinePattern: RegExp?
        get() = definedExternally
        set(value) = definedExternally
    var unIndentedLinePattern: RegExp?
        get() = definedExternally
        set(value) = definedExternally
}

external interface FoldingMarkers {
    var start: RegExp
    var end: RegExp
}

external interface FoldingRules {
    var offSide: Boolean?
        get() = definedExternally
        set(value) = definedExternally
    var markers: FoldingMarkers?
        get() = definedExternally
        set(value) = definedExternally
}

external interface OnEnterRule {
    var beforeText: RegExp
    var afterText: RegExp?
        get() = definedExternally
        set(value) = definedExternally
    var oneLineAboveText: RegExp?
        get() = definedExternally
        set(value) = definedExternally
    var action: EnterAction
}

external interface IDocComment {
    var open: String
    var close: String?
        get() = definedExternally
        set(value) = definedExternally
}

external interface IAutoClosingPair {
    var open: String
    var close: String
}

external interface IAutoClosingPairConditional : IAutoClosingPair {
    var notIn: Array<String>?
        get() = definedExternally
        set(value) = definedExternally
}

external enum class IndentAction {
    None /* = 0 */,
    Indent /* = 1 */,
    IndentOutdent /* = 2 */,
    Outdent /* = 3 */
}

external interface EnterAction {
    var indentAction: IndentAction
    var appendText: String?
        get() = definedExternally
        set(value) = definedExternally
    var removeText: Number?
        get() = definedExternally
        set(value) = definedExternally
}

external interface ILanguageExtensionPoint {
    var id: String
    var extensions: Array<String>?
        get() = definedExternally
        set(value) = definedExternally
    var filenames: Array<String>?
        get() = definedExternally
        set(value) = definedExternally
    var filenamePatterns: Array<String>?
        get() = definedExternally
        set(value) = definedExternally
    var firstLine: String?
        get() = definedExternally
        set(value) = definedExternally
    var aliases: Array<String>?
        get() = definedExternally
        set(value) = definedExternally
    var mimetypes: Array<String>?
        get() = definedExternally
        set(value) = definedExternally
    var configuration: Uri?
        get() = definedExternally
        set(value) = definedExternally
}

external interface IMonarchLanguageTokenizer

external interface IMonarchLanguage {
    var tokenizer: IMonarchLanguageTokenizer
    var ignoreCase: Boolean?
        get() = definedExternally
        set(value) = definedExternally
    var unicode: Boolean?
        get() = definedExternally
        set(value) = definedExternally
    var defaultToken: String?
        get() = definedExternally
        set(value) = definedExternally
    var brackets: Array<IMonarchLanguageBracket>?
        get() = definedExternally
        set(value) = definedExternally
    var start: String?
        get() = definedExternally
        set(value) = definedExternally
    var tokenPostfix: String?
        get() = definedExternally
        set(value) = definedExternally
}

external interface IExpandedMonarchLanguageRule {
    var regex: RegExp
    var action: IExpandedMonarchLanguageAction
    var include: String
}

external interface IExpandedMonarchLanguageAction {
    var group: Array<dynamic /* IShortMonarchLanguageAction | IExpandedMonarchLanguageAction | Array<IShortMonarchLanguageAction> | Array<IExpandedMonarchLanguageAction> */>?
        get() = definedExternally
        set(value) = definedExternally
    var cases: Any?
        get() = definedExternally
        set(value) = definedExternally
    var token: String?
        get() = definedExternally
        set(value) = definedExternally
    var next: String?
        get() = definedExternally
        set(value) = definedExternally
    var switchTo: String?
        get() = definedExternally
        set(value) = definedExternally
    var goBack: Number?
        get() = definedExternally
        set(value) = definedExternally
    var bracket: String?
        get() = definedExternally
        set(value) = definedExternally
    var nextEmbedded: String?
        get() = definedExternally
        set(value) = definedExternally
    var log: String?
        get() = definedExternally
        set(value) = definedExternally
}

external interface IMonarchLanguageBracket {
    var open: String
    var close: String
    var token: String
}
