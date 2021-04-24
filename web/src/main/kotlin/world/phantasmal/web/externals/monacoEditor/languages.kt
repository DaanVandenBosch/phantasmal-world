@file:JsModule("monaco-editor")
@file:JsNonModule
@file:JsQualifier("languages")
@file:Suppress("unused")

package world.phantasmal.web.externals.monacoEditor

import kotlin.js.Promise
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

/**
 * Register a completion item provider (use by e.g. suggestions).
 */
external fun registerCompletionItemProvider(
    languageId: String,
    provider: CompletionItemProvider,
): IDisposable

/**
 * Register a signature help provider (used by e.g. parameter hints).
 */
external fun registerSignatureHelpProvider(
    languageId: String,
    provider: SignatureHelpProvider,
): IDisposable

/**
 * Register a definition provider (used by e.g. go to definition).
 */
external fun registerDefinitionProvider(
    languageId: String,
    provider: DefinitionProvider,
): IDisposable

/**
 * Register a hover provider (used by e.g. editor hover).
 */
external fun registerHoverProvider(languageId: String, provider: HoverProvider): IDisposable

external fun registerDocumentSymbolProvider(
    languageId: String,
    provider: DocumentSymbolProvider,
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

external interface CompletionItemLabel {
    /**
     * The function or variable. Rendered leftmost.
     */
    var name: String

    /**
     * The signature without the return type. Render after `name`.
     */
    var signature: String?
        get() = definedExternally
        set(value) = definedExternally

    /**
     * The fully qualified name, like package name or file path. Rendered after `signature`.
     */
    var qualifier: String?
        get() = definedExternally
        set(value) = definedExternally

    /**
     * The return-type of a function or type of a property/variable. Rendered rightmost.
     */
    var type: String?
        get() = definedExternally
        set(value) = definedExternally
}

external interface CompletionItemRanges {
    var insert: IRange
    var replace: IRange
}

external enum class CompletionItemKind {
    Method /* = 0 */,
    Function /* = 1 */,
    Constructor /* = 2 */,
    Field /* = 3 */,
    Variable /* = 4 */,
    Class /* = 5 */,
    Struct /* = 6 */,
    Interface /* = 7 */,
    Module /* = 8 */,
    Property /* = 9 */,
    Event /* = 10 */,
    Operator /* = 11 */,
    Unit /* = 12 */,
    Value /* = 13 */,
    Constant /* = 14 */,
    Enum /* = 15 */,
    EnumMember /* = 16 */,
    Keyword /* = 17 */,
    Text /* = 18 */,
    Color /* = 19 */,
    File /* = 20 */,
    Reference /* = 21 */,
    Customcolor /* = 22 */,
    Folder /* = 23 */,
    TypeParameter /* = 24 */,
    Snippet /* = 25 */,
}

external enum class CompletionItemTag {
    Deprecated /* = 1 */,
}

external enum class CompletionItemInsertTextRule {
    /**
     * Adjust whitespace/indentation of multiline insert texts to
     * match the current line indentation.
     */
    KeepWhitespace /* = 1 */,

    /**
     * `insertText` is a snippet.
     */
    InsertAsSnippet /* = 4 */,
}

external interface Command {
    var id: String
    var title: String
    var tooltip: String
    var arguments: Array<dynamic>
}

/**
 * A completion item represents a text snippet that is
 * proposed to complete text that is being typed.
 */
external interface CompletionItem {
    /**
     * The label of this completion item. By default
     * this is also the text that is inserted when selecting
     * this completion.
     */
    var label: CompletionItemLabel /* string | CompletionItemLabel */

    /**
     * The kind of this completion item. Based on the kind
     * an icon is chosen by the editor.
     */
    var kind: CompletionItemKind

    /**
     * A modifier to the `kind` which affect how the item
     * is rendered, e.g. Deprecated is rendered with a strikeout
     */
    var tags: Array<CompletionItemTag>

    /**
     * A human-readable string with additional information
     * about this item, like type or symbol information.
     */
    var detail: String

    /**
     * A human-readable string that represents a doc-comment.
     */
    var documentation: String /* string | IMarkdownString */

    /**
     * A string that should be used when comparing this item
     * with other items. When `falsy` the [label](#CompletionItem.label)
     * is used.
     */
    var sortText: String

    /**
     * A string that should be used when filtering a set of
     * completion items. When `falsy` the [label](#CompletionItem.label)
     * is used.
     */
    var filterText: String

    /**
     * Select this item when showing. *Note* that only one completion item can be selected and
     * that the editor decides which item that is. The rule is that the *first* item of those
     * that match best is selected.
     */
    var preselect: Boolean

    /**
     * A string or snippet that should be inserted in a document when selecting
     * this completion. When `falsy` the [label](#CompletionItem.label)
     * is used.
     */
    var insertText: String

    /**
     * Addition rules (as bitmask) that should be applied when inserting
     * this completion.
     */
    var insertTextRules: CompletionItemInsertTextRule

    /**
     * A range of text that should be replaced by this completion item.
     *
     * Defaults to a range from the start of the [current word](#TextDocument.getWordRangeAtPosition) to the
     * current position.
     *
     * *Note:* The range must be a [single line](#Range.isSingleLine) and it must
     * [contain](#Range.contains) the position at which completion has been [requested](#CompletionItemProvider.provideCompletionItems).
     */
    var range: CompletionItemRanges

    /**
     * An optional set of characters that when pressed while this completion is active will accept it first and
     * then type that character. *Note* that all commit characters should have `length=1` and that superfluous
     * characters will be ignored.
     */
    var commitCharacters: Array<String>

    /**
     * An optional array of additional text edits that are applied when
     * selecting this completion. Edits must not overlap with the main edit
     * nor with themselves.
     */
    var additionalTextEdits: Array<ISingleEditOperation>

    /**
     * A command that should be run upon acceptance of this item.
     */
    var command: Command
}

external interface CompletionList {
    var suggestions: Array<CompletionItem>
    var incomplete: Boolean

    fun dispose()
}

/**
 * How a suggest provider was triggered.
 */
external enum class CompletionTriggerKind {
    Invoke /* = 0 */,
    TriggerCharacter /* = 1 */,
    TriggerForIncompleteCompletions /* = 2 */,
}

/**
 * Contains additional information about the context in which
 * [completion provider](#CompletionItemProvider.provideCompletionItems) is triggered.
 */
external interface CompletionContext {
    /**
     * How the completion was triggered.
     */
    var triggerKind: CompletionTriggerKind

    /**
     * Character that triggered the completion item provider.
     *
     * `undefined` if provider was not triggered by a character.
     */
    var triggerCharacter: String
}

external interface CompletionItemProvider {
    var triggerCharacters: Array<String>?
        get() = definedExternally
        set(value) = definedExternally

    /**
     * Provide completion items for the given position and document.
     */
    fun provideCompletionItems(
        model: ITextModel,
        position: Position,
        context: CompletionContext,
        token: CancellationToken,
    ): Promise<CompletionList?> /* type ProviderResult<T> = T | undefined | null | Thenable<T | undefined | null> */
}

/**
 * Represents a parameter of a callable-signature. A parameter can
 * have a label and a doc-comment.
 */
external interface ParameterInformation {
    /**
     * The label of this signature. Will be shown in
     * the UI.
     */
    var label: Array<Int> /* string | [number, number] */

    /**
     * The human-readable doc-comment of this signature. Will be shown
     * in the UI but can be omitted.
     *
     * This property is not nullable in TS. Do not assign null to it as null will be interpreted as
     * IMarkdownString.
     */
    var documentation: String? /* string | IMarkdownString */
        get() = definedExternally
        set(value) = definedExternally
}

/**
 * Represents the signature of something callable. A signature
 * can have a label, like a function-name, a doc-comment, and
 * a set of parameters.
 */
external interface SignatureInformation {
    /**
     * The label of this signature. Will be shown in
     * the UI.
     */
    var label: String

    /**
     * The human-readable doc-comment of this signature. Will be shown
     * in the UI but can be omitted.
     *
     * This property is not nullable in TS. Do not assign null to it as null will be interpreted as
     * IMarkdownString.
     */
    var documentation: String? /* string | IMarkdownString */
        get() = definedExternally
        set(value) = definedExternally

    /**
     * The parameters of this signature.
     */
    var parameters: Array<ParameterInformation>
}

/**
 * Signature help represents the signature of something
 * callable. There can be multiple signatures but only one
 * active and only one active parameter.
 */
external interface SignatureHelp {
    /**
     * One or more signatures.
     */
    var signatures: Array<SignatureInformation>

    /**
     * The active signature.
     */
    var activeSignature: Int

    /**
     * The active parameter of the active signature.
     */
    var activeParameter: Int
}

external enum class SignatureHelpTriggerKind {
    Invoke /* = 1 */,
    TriggerCharacter /* = 2 */,
    ContentChange /* = 3 */,
}

external interface SignatureHelpContext {
    val triggerKind: SignatureHelpTriggerKind
    val triggerCharacter: String?
        get() = definedExternally
    val isRetrigger: Boolean
    val activeSignatureHelp: SignatureHelp?
        get() = definedExternally
}

external interface SignatureHelpResult : IDisposable {
    var value: SignatureHelp
}

/**
 * The signature help provider interface defines the contract between extensions and
 * the [parameter hints](https://code.visualstudio.com/docs/editor/intellisense)-feature.
 */
external interface SignatureHelpProvider {
    val signatureHelpTriggerCharacters: Array<String>?
        get() = definedExternally
    val signatureHelpRetriggerCharacters: Array<String>?
        get() = definedExternally

    /**
     * Provide help for the signature at the given position and document.
     */
    fun provideSignatureHelp(
        model: ITextModel,
        position: Position,
        token: CancellationToken,
        context: SignatureHelpContext,
    ): Promise<SignatureHelpResult?> /* type ProviderResult<T> = T | undefined | null | Thenable<T | undefined | null> */
}

/**
 * A hover represents additional information for a symbol or word. Hovers are
 * rendered in a tooltip-like widget.
 */
external interface Hover {
    /**
     * The contents of this hover.
     */
    var contents: Array<IMarkdownString>

    /**
     * The range to which this hover applies. When missing, the
     * editor will use the range at the current position or the
     * current position itself.
     */
    var range: IRange
}

external interface HoverProvider {
    /**
     * Provide a hover for the given position and document. Multiple hovers at the same
     * position will be merged by the editor. A hover can have a range which defaults
     * to the word range at the position when omitted.
     */
    fun provideHover(
        model: ITextModel,
        position: Position,
        token: CancellationToken,
    ): Promise<Hover?> /* type ProviderResult<T> = T | undefined | null | Thenable<T | undefined | null> */
}

external interface LocationLink {
    /**
     * A range to select where this link originates from.
     */
    var originSelectionRange: IRange?

    /**
     * The target uri this link points to.
     */
    var uri: Uri

    /**
     * The full range this link points to.
     */
    var range: IRange

    /**
     * A range to select this link points to. Must be contained
     * in `LocationLink.range`.
     */
    var targetSelectionRange: IRange?
}

/**
 * The definition provider interface defines the contract between extensions and
 * the [go to definition](https://code.visualstudio.com/docs/editor/editingevolved#_go-to-definition)
 * and peek definition features.
 */
external interface DefinitionProvider {
    /**
     * Provide the definition of the symbol at the given position and document.
     */
    fun provideDefinition(
        model: ITextModel,
        position: Position,
        token: CancellationToken,
    ): Promise<Array<LocationLink>?>
}

external enum class SymbolKind {
    File /* = 0 */,
    Module /* = 1 */,
    Namespace /* = 2 */,
    Package /* = 3 */,
    Class /* = 4 */,
    Method /* = 5 */,
    Property /* = 6 */,
    Field /* = 7 */,
    Constructor /* = 8 */,
    Enum /* = 9 */,
    Interface /* = 10 */,
    Function /* = 11 */,
    Variable /* = 12 */,
    Constant /* = 13 */,
    String /* = 14 */,
    Number /* = 15 */,
    Boolean /* = 16 */,
    Array /* = 17 */,
    Object /* = 18 */,
    Key /* = 19 */,
    Null /* = 20 */,
    EnumMember /* = 21 */,
    Struct /* = 22 */,
    Event /* = 23 */,
    Operator /* = 24 */,
    TypeParameter /* = 25 */
}

external enum class SymbolTag {
    Deprecated /* = 1 */
}

external interface DocumentSymbol {
    var name: String
    var detail: String
    var kind: SymbolKind
    var tags: Array<SymbolTag>
    var containerName: String?
    var range: IRange
    var selectionRange: IRange
    var children: Array<DocumentSymbol>?
}

external interface DocumentSymbolProvider {
    val displayName: String?

    fun provideDocumentSymbols(
        model: ITextModel,
        token: CancellationToken,
    ): Promise<Array<DocumentSymbol>>
}
