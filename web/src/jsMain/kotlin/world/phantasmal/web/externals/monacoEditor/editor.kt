@file:JsModule("monaco-editor")
@file:JsNonModule
@file:JsQualifier("editor")
@file:Suppress("unused", "PropertyName")

package world.phantasmal.web.externals.monacoEditor

import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.Range
import kotlin.js.Promise

external fun create(
    domElement: HTMLElement,
    options: IStandaloneEditorConstructionOptions = definedExternally,
): IStandaloneCodeEditor

external fun createModel(
    value: String,
    language: String = definedExternally,
    uri: Uri = definedExternally,
): ITextModel

external fun defineTheme(themeName: String, themeData: IStandaloneThemeData)

/**
 * Set the markers for a model.
 */
external fun setModelMarkers(model: ITextModel, owner: String, markers: Array<IMarkerData>)

external interface IStandaloneThemeData {
    var base: String /* 'vs' | 'vs-dark' | 'hc-black' */
    var inherit: Boolean
    var rules: Array<ITokenThemeRule>
    var encodedTokensColors: Array<String>?
    var colors: IColors
}

external interface IColors

external interface ITokenThemeRule {
    var token: String
    var foreground: String?
    var background: String?
    var fontStyle: String?
}

sealed external class ScrollType {
    object Smooth : ScrollType /* = 0 */
    object Immediate : ScrollType /* = 1 */
}

external interface IDimension {
    var width: Number
    var height: Number
}

external interface IEditor {
    fun onDidDispose(listener: () -> Unit): IDisposable
    fun dispose()
    fun getId(): String
    fun getEditorType(): String
    fun updateOptions(newOptions: IEditorOptions)
    fun layout(dimension: IDimension = definedExternally)
    fun focus()
    fun hasTextFocus(): Boolean
    fun saveViewState(): dynamic /* ICodeEditorViewState? | IDiffEditorViewState? */
    fun getVisibleColumnFromPosition(position: IPosition): Number
    fun getPosition(): Position?
    fun setPosition(position: IPosition)
    fun revealLine(lineNumber: Number, scrollType: ScrollType = definedExternally)
    fun revealLineInCenter(lineNumber: Number, scrollType: ScrollType = definedExternally)
    fun revealLineInCenterIfOutsideViewport(
        lineNumber: Number,
        scrollType: ScrollType = definedExternally,
    )

    fun revealLineNearTop(lineNumber: Number, scrollType: ScrollType = definedExternally)
    fun revealPosition(position: IPosition, scrollType: ScrollType = definedExternally)
    fun revealPositionInCenter(position: IPosition, scrollType: ScrollType = definedExternally)
    fun revealPositionInCenterIfOutsideViewport(
        position: IPosition,
        scrollType: ScrollType = definedExternally,
    )

    fun revealPositionNearTop(position: IPosition, scrollType: ScrollType = definedExternally)
    fun getSelection(): Selection?
    fun getSelections(): Array<Selection>?
    fun setSelection(selection: IRange)
    fun setSelection(selection: Range)
    fun setSelection(selection: ISelection)
    fun setSelection(selection: Selection)
    fun setSelections(selections: Any)
    fun revealLines(
        startLineNumber: Number,
        endLineNumber: Number,
        scrollType: ScrollType = definedExternally,
    )

    fun revealLinesInCenter(
        lineNumber: Number,
        endLineNumber: Number,
        scrollType: ScrollType = definedExternally,
    )

    fun revealLinesInCenterIfOutsideViewport(
        lineNumber: Number,
        endLineNumber: Number,
        scrollType: ScrollType = definedExternally,
    )

    fun revealLinesNearTop(
        lineNumber: Number,
        endLineNumber: Number,
        scrollType: ScrollType = definedExternally,
    )

    fun revealRange(range: IRange, scrollType: ScrollType = definedExternally)
    fun revealRangeInCenter(range: IRange, scrollType: ScrollType = definedExternally)
    fun revealRangeAtTop(range: IRange, scrollType: ScrollType = definedExternally)
    fun revealRangeInCenterIfOutsideViewport(
        range: IRange,
        scrollType: ScrollType = definedExternally,
    )

    fun revealRangeNearTop(range: IRange, scrollType: ScrollType = definedExternally)
    fun revealRangeNearTopIfOutsideViewport(
        range: IRange,
        scrollType: ScrollType = definedExternally,
    )

    fun trigger(source: String?, handlerId: String, payload: dynamic)
    fun getModel(): dynamic /* ITextModel? | IDiffEditorModel? */
    fun setModel(model: ITextModel?)
}

sealed external class MouseTargetType {
    object UNKNOWN : MouseTargetType /* = 0 */
    object TEXTAREA : MouseTargetType /* = 1 */
    object GUTTER_GLYPH_MARGIN : MouseTargetType /* = 2 */
    object GUTTER_LINE_NUMBERS : MouseTargetType /* = 3 */
    object GUTTER_LINE_DECORATIONS : MouseTargetType /* = 4 */
    object GUTTER_VIEW_ZONE : MouseTargetType /* = 5 */
    object CONTENT_TEXT : MouseTargetType /* = 6 */
    object CONTENT_EMPTY : MouseTargetType /* = 7 */
    object CONTENT_VIEW_ZONE : MouseTargetType /* = 8 */
    object CONTENT_WIDGET : MouseTargetType /* = 9 */
    object OVERVIEW_RULER : MouseTargetType /* = 10 */
    object SCROLLBAR : MouseTargetType /* = 11 */
    object OVERLAY_WIDGET : MouseTargetType /* = 12 */
    object OUTSIDE_EDITOR : MouseTargetType /* = 13 */
}

external interface IMouseTarget {
    val element: Element?
    val type: MouseTargetType
    val position: Position?
    val mouseColumn: Int
    val range: Range?
    val detail: dynamic
}

external interface IEditorMouseEvent {
    val event: IMouseEvent
    val target: IMouseTarget
}

external interface ICodeEditor : IEditor {
    fun onDidChangeModelContent(listener: (e: IModelContentChangedEvent) -> Unit): IDisposable
    fun onDidChangeModelLanguage(listener: (e: IModelLanguageChangedEvent) -> Unit): IDisposable
    fun onDidChangeModelLanguageConfiguration(listener: (e: IModelLanguageConfigurationChangedEvent) -> Unit): IDisposable
    fun onDidChangeModelOptions(listener: (e: IModelOptionsChangedEvent) -> Unit): IDisposable
    fun onDidChangeCursorPosition(listener: (e: ICursorPositionChangedEvent) -> Unit): IDisposable
    fun onDidChangeCursorSelection(listener: (e: ICursorSelectionChangedEvent) -> Unit): IDisposable
    fun onDidChangeModel(listener: () -> Unit): IDisposable
    fun onDidChangeModelDecorations(listener: (e: IModelDecorationsChangedEvent) -> Unit): IDisposable
    fun onDidFocusEditorText(listener: () -> Unit): IDisposable
    fun onDidBlurEditorText(listener: () -> Unit): IDisposable
    fun onDidFocusEditorWidget(listener: () -> Unit): IDisposable
    fun onDidBlurEditorWidget(listener: () -> Unit): IDisposable
    fun onDidCompositionStart(listener: () -> Unit): IDisposable
    fun onDidCompositionEnd(listener: () -> Unit): IDisposable
    fun onDidAttemptReadOnlyEdit(listener: () -> Unit): IDisposable
    fun onMouseUp(listener: (e: IEditorMouseEvent) -> Unit): IDisposable
    fun hasWidgetFocus(): Boolean
    override fun getModel(): ITextModel?
    override fun setModel(model: ITextModel?)
    fun getRawOptions(): IEditorOptions
    fun setValue(newValue: String)
    fun getContentWidth(): Number
    fun getScrollWidth(): Number
    fun getScrollLeft(): Number
    fun getContentHeight(): Number
    fun getScrollHeight(): Number
    fun getScrollTop(): Number
    fun pushUndoStop(): Boolean
    fun executeEdits(
        source: String?,
        edits: Array<IIdentifiedSingleEditOperation>,
        endCursorState: ICursorStateComputer = definedExternally,
    ): Boolean

    fun executeEdits(
        source: String?,
        edits: Array<IIdentifiedSingleEditOperation>,
        endCursorState: Array<Selection> = definedExternally,
    ): Boolean

    fun getLineDecorations(lineNumber: Number): Array<IModelDecoration>?
    fun deltaDecorations(
        oldDecorations: Array<String>,
        newDecorations: Array<IModelDeltaDecoration>,
    ): Array<String>

    fun getVisibleRanges(): Array<Range>
    fun getTopForLineNumber(lineNumber: Number): Number
    fun getTopForPosition(lineNumber: Number, column: Number): Number
    fun getContainerDomNode(): HTMLElement
    fun getDomNode(): HTMLElement?
    fun getOffsetForColumn(lineNumber: Number, column: Number): Number
    fun render(forceRedraw: Boolean = definedExternally)
    fun applyFontInfo(target: HTMLElement)
    fun getSupportedActions(): Array<IEditorAction>
    fun getAction(id: String): IEditorAction
    fun addAction(descriptor: IActionDescriptor): IDisposable
}

external interface IActionDescriptor {
    var id: String
    var label: String
    var keybindings: Array<Int>

    fun run(editor: ICodeEditor, vararg args: dynamic): dynamic
}

external interface IEditorAction {
    val id: String
    val label: String
    val alias: String

    fun isSupported(): Boolean
    fun run(): Promise<Unit>
}

external interface IStandaloneCodeEditor : ICodeEditor {
    override fun updateOptions(newOptions: IEditorOptions /* IEditorOptions & IGlobalEditorOptions */)
}

external interface IGlobalEditorOptions {
    var tabSize: Number?
    var insertSpaces: Boolean?
    var detectIndentation: Boolean?
    var trimAutoWhitespace: Boolean?
    var largeFileOptimizations: Boolean?
    var wordBasedSuggestions: Boolean?
    var stablePeek: Boolean?
    var maxTokenizationLineLength: Number?
    var theme: String?
}

external interface IEditorScrollbarOptions {
    var arrowSize: Number?
    var vertical: String? /* 'auto' | 'visible' | 'hidden' */
    var horizontal: String? /* 'auto' | 'visible' | 'hidden' */
    var useShadows: Boolean?
    var verticalHasArrows: Boolean?
    var horizontalHasArrows: Boolean?
    var handleMouseWheel: Boolean?
    var alwaysConsumeMouseWheel: Boolean?
    var horizontalScrollbarSize: Number?
    var verticalScrollbarSize: Number?
    var verticalSliderSize: Number?
    var horizontalSliderSize: Number?
}

external interface IEditorMinimapOptions {
    var enabled: Boolean?
    var side: String? /* 'right' | 'left' */
    var size: String? /* 'proportional' | 'fill' | 'fit' */
    var showSlider: String? /* 'always' | 'mouseover' */
    var renderCharacters: Boolean?
    var maxColumn: Number?
    var scale: Number?
}

external interface IEditorFindOptions {
    var cursorMoveOnType: Boolean?
    var seedSearchStringFromSelection: Boolean?
    var autoFindInSelection: String? /* 'never' | 'always' | 'multiline' */
    var addExtraSpaceOnTop: Boolean?
    var loop: Boolean?
}

external interface IEditorOptions {
    var inDiffEditor: Boolean?
    var ariaLabel: String?
    var tabIndex: Number?
    var rulers: Array<dynamic /* Number | IRulerOption */>?
    var wordSeparators: String?
    var selectionClipboard: Boolean?
    var lineNumbers: dynamic /* String | String | String | String | ((lineNumber: Number) -> String)? */
    var cursorSurroundingLines: Number?
    var cursorSurroundingLinesStyle: String? /* 'default' | 'all' */
    var renderFinalNewline: Boolean?
    var unusualLineTerminators: String? /* 'off' | 'prompt' | 'auto' */
    var selectOnLineNumbers: Boolean?
    var lineNumbersMinChars: Number?
    var glyphMargin: Boolean?
    var lineDecorationsWidth: dynamic /* Number? | String? */
    var revealHorizontalRightPadding: Number?
    var roundedSelection: Boolean?
    var extraEditorClassName: String?
    var readOnly: Boolean?
    var renameOnType: Boolean?
    var renderValidationDecorations: String? /* 'editable' | 'on' | 'off' */
    var scrollbar: IEditorScrollbarOptions?
    var minimap: IEditorMinimapOptions?
    var find: IEditorFindOptions?
    var fixedOverflowWidgets: Boolean?
    var overviewRulerLanes: Number?
    var overviewRulerBorder: Boolean?
    var cursorBlinking: String? /* 'blink' | 'smooth' | 'phase' | 'expand' | 'solid' */
    var mouseWheelZoom: Boolean?
    var mouseStyle: String? /* 'text' | 'default' | 'copy' */
    var cursorSmoothCaretAnimation: Boolean?
    var cursorStyle: String? /* 'line' | 'block' | 'underline' | 'line-thin' | 'block-outline' | 'underline-thin' */
    var cursorWidth: Number?
    var fontLigatures: dynamic /* Boolean? | String? */
    var disableLayerHinting: Boolean?
    var disableMonospaceOptimizations: Boolean?
    var hideCursorInOverviewRuler: Boolean?
    var scrollBeyondLastLine: Boolean?
    var scrollBeyondLastColumn: Number?
    var smoothScrolling: Boolean?
    var automaticLayout: Boolean?
    var wordWrap: String? /* 'off' | 'on' | 'wordWrapColumn' | 'bounded' */
    var wordWrapColumn: Number?
    var wordWrapMinified: Boolean?
    var wrappingIndent: String? /* 'none' | 'same' | 'indent' | 'deepIndent' */
    var wrappingStrategy: String? /* 'simple' | 'advanced' */
    var wordWrapBreakBeforeCharacters: String?
    var wordWrapBreakAfterCharacters: String?
    var stopRenderingLineAfter: Number?
    var links: Boolean?
    var colorDecorators: Boolean?
    var contextmenu: Boolean?
    var mouseWheelScrollSensitivity: Number?
    var fastScrollSensitivity: Number?
    var scrollPredominantAxis: Boolean?
    var columnSelection: Boolean?
    var multiCursorModifier: String? /* 'ctrlCmd' | 'alt' */
    var multiCursorMergeOverlapping: Boolean?
    var multiCursorPaste: String? /* 'spread' | 'full' */
    var accessibilitySupport: String? /* 'auto' | 'off' | 'on' */
    var accessibilityPageSize: Number?
    var quickSuggestions: dynamic /* Boolean? | IQuickSuggestionsOptions? */
    var quickSuggestionsDelay: Number?
    var autoClosingBrackets: String? /* 'always' | 'languageDefined' | 'beforeWhitespace' | 'never' */
    var autoClosingQuotes: String? /* 'always' | 'languageDefined' | 'beforeWhitespace' | 'never' */
    var autoClosingOvertype: String? /* 'always' | 'auto' | 'never' */
    var autoSurround: String? /* 'languageDefined' | 'quotes' | 'brackets' | 'never' */
    var autoIndent: String? /* 'none' | 'keep' | 'brackets' | 'advanced' | 'full' */
    var formatOnType: Boolean?
    var formatOnPaste: Boolean?
    var dragAndDrop: Boolean?
    var suggestOnTriggerCharacters: Boolean?
    var acceptSuggestionOnEnter: String? /* 'on' | 'smart' | 'off' */
    var acceptSuggestionOnCommitCharacter: Boolean?
    var snippetSuggestions: String? /* 'top' | 'bottom' | 'inline' | 'none' */
    var emptySelectionClipboard: Boolean?
    var copyWithSyntaxHighlighting: Boolean?
    var suggestSelection: String? /* 'first' | 'recentlyUsed' | 'recentlyUsedByPrefix' */
    var suggestFontSize: Number?
    var suggestLineHeight: Number?
    var tabCompletion: String? /* 'on' | 'off' | 'onlySnippets' */
    var selectionHighlight: Boolean?
    var occurrencesHighlight: Boolean?
    var codeLens: Boolean?
    var codeActionsOnSaveTimeout: Number?
    var folding: Boolean?
    var foldingStrategy: String? /* 'auto' | 'indentation' */
    var foldingHighlight: Boolean?
    var showFoldingControls: String? /* 'always' | 'mouseover' */
    var unfoldOnClickAfterEndOfLine: Boolean?
    var matchBrackets: String? /* 'never' | 'near' | 'always' */
    var renderWhitespace: String? /* 'none' | 'boundary' | 'selection' | 'trailing' | 'all' */
    var renderControlCharacters: Boolean?
    var renderIndentGuides: Boolean?
    var highlightActiveIndentGuide: Boolean?
    var renderLineHighlight: String? /* 'none' | 'gutter' | 'line' | 'all' */
    var renderLineHighlightOnlyWhenFocus: Boolean?
    var useTabStops: Boolean?
    var fontFamily: String?
    var fontWeight: String?
    var fontSize: Number?
    var lineHeight: Number?
    var letterSpacing: Number?
    var showUnused: Boolean?
    var peekWidgetDefaultFocus: String? /* 'tree' | 'editor' */
    var definitionLinkOpensInPeek: Boolean?
    var showDeprecated: Boolean?
}

external interface IEditorConstructionOptions : IEditorOptions {
    var overflowWidgetsDomNode: HTMLElement?
}

external interface IStandaloneEditorConstructionOptions : IEditorConstructionOptions,
    IGlobalEditorOptions {
    var model: ITextModel?
    var value: String?
    var language: String?
    override var theme: String?
    var accessibilityHelpUrl: String?
}

external interface IMarker {
    var owner: String
    var resource: Uri
    var severity: MarkerSeverity
    var code: dynamic /* String? | `T$5`? */
    var message: String
    var source: String?
    var startLineNumber: Number
    var startColumn: Number
    var endLineNumber: Number
    var endColumn: Number
    var relatedInformation: Array<IRelatedInformation>?
    var tags: Array<MarkerTag>?
}

external interface IMarkerData {
    var code: dynamic /* String? | `T$5`? */
    var severity: MarkerSeverity
    var message: String
    var source: String?
    var startLineNumber: Number
    var startColumn: Number
    var endLineNumber: Number
    var endColumn: Number
    var relatedInformation: Array<IRelatedInformation>?
    var tags: Array<MarkerTag>?
}

external interface IRelatedInformation {
    var resource: Uri
    var message: String
    var startLineNumber: Number
    var startColumn: Number
    var endLineNumber: Number
    var endColumn: Number
}

external interface IColorizerOptions {
    var tabSize: Number?
}

external interface IColorizerElementOptions : IColorizerOptions {
    var theme: String?
    var mimeType: String?
}

sealed external class ScrollbarVisibility {
    object Auto : ScrollbarVisibility /* = 1 */
    object Hidden : ScrollbarVisibility /* = 2 */
    object Visible : ScrollbarVisibility /* = 3 */
}

external interface ThemeColor {
    var id: String
}

sealed external class OverviewRulerLane {
    object Left : OverviewRulerLane /* = 1 */
    object Center : OverviewRulerLane /* = 2 */
    object Right : OverviewRulerLane /* = 4 */
    object Full : OverviewRulerLane /* = 7 */
}

sealed external class MinimapPosition {
    object Inline : MinimapPosition /* = 1 */
    object Gutter : MinimapPosition /* = 2 */
}

external interface IDecorationOptions {
    var color: dynamic /* String? | ThemeColor? */
    var darkColor: dynamic /* String? | ThemeColor? */
}

external interface IModelDecorationOverviewRulerOptions : IDecorationOptions {
    var position: OverviewRulerLane
}

external interface IModelDecorationMinimapOptions : IDecorationOptions {
    var position: MinimapPosition
}

external interface IModelDecorationOptions {
    var stickiness: TrackedRangeStickiness?
    var className: String?
    var glyphMarginHoverMessage: dynamic /* IMarkdownString? | Array<IMarkdownString>? */
    var hoverMessage: dynamic /* IMarkdownString? | Array<IMarkdownString>? */
    var isWholeLine: Boolean?
    var zIndex: Number?
    var overviewRuler: IModelDecorationOverviewRulerOptions?
    var minimap: IModelDecorationMinimapOptions?
    var glyphMarginClassName: String?
    var linesDecorationsClassName: String?
    var firstLineDecorationClassName: String?
    var marginClassName: String?
    var inlineClassName: String?
    var inlineClassNameAffectsLetterSpacing: Boolean?
    var beforeContentClassName: String?
    var afterContentClassName: String
}

external interface IModelDeltaDecoration {
    var range: IRange
    var options: IModelDecorationOptions
}

external interface IModelDecoration {
    var id: String
    var ownerId: Number
    var range: Range
    var options: IModelDecorationOptions
}

external interface IWordAtPosition {
    var word: String
    var startColumn: Number
    var endColumn: Number
}

sealed external class EndOfLinePreference {
    object TextDefined : EndOfLinePreference /* = 0 */
    object LF : EndOfLinePreference /* = 1 */
    object CRLF : EndOfLinePreference /* = 2 */
}

sealed external class DefaultEndOfLine {
    object LF : DefaultEndOfLine /* = 1 */
    object CRLF : DefaultEndOfLine /* = 2 */
}

sealed external class EndOfLineSequence {
    object LF : EndOfLineSequence /* = 0 */
    object CRLF : EndOfLineSequence /* = 1 */
}

external interface ISingleEditOperation {
    var range: IRange
    var text: String?
    var forceMoveMarkers: Boolean?
}

external interface IIdentifiedSingleEditOperation {
    var range: IRange
    var text: String?
    var forceMoveMarkers: Boolean?
}

external interface IValidEditOperation {
    var range: Range
    var text: String
}

external interface ICursorStateComputer

open external class TextModelResolvedOptions {
    open var _textModelResolvedOptionsBrand: Unit
    open var tabSize: Number
    open var indentSize: Number
    open var insertSpaces: Boolean
    open var defaultEOL: DefaultEndOfLine
    open var trimAutoWhitespace: Boolean
}

external interface ITextModelUpdateOptions {
    var tabSize: Number?
    var indentSize: Number?
    var insertSpaces: Boolean?
    var trimAutoWhitespace: Boolean?
}

open external class FindMatch {
    open var _findMatchBrand: Unit
    open var range: Range
    open var matches: Array<String>?
}

sealed external class TrackedRangeStickiness {
    object AlwaysGrowsWhenTypingAtEdges : TrackedRangeStickiness /* = 0 */
    object NeverGrowsWhenTypingAtEdges : TrackedRangeStickiness /* = 1 */
    object GrowsOnlyWhenTypingBefore : TrackedRangeStickiness /* = 2 */
    object GrowsOnlyWhenTypingAfter : TrackedRangeStickiness /* = 3 */
}

external interface ITextModel {
    var uri: Uri
    var id: String
    fun getOptions(): TextModelResolvedOptions
    fun getVersionId(): Int
    fun getAlternativeVersionId(): Int
    fun setValue(newValue: String)
    fun getValue(
        eol: EndOfLinePreference = definedExternally,
        preserveBOM: Boolean = definedExternally,
    ): String

    fun getValueLength(
        eol: EndOfLinePreference = definedExternally,
        preserveBOM: Boolean = definedExternally,
    ): Number

    fun getValueInRange(range: IRange, eol: EndOfLinePreference = definedExternally): String
    fun getValueLengthInRange(range: IRange): Number
    fun getCharacterCountInRange(range: IRange): Number
    fun getLineCount(): Number
    fun getLineContent(lineNumber: Number): String
    fun getLineLength(lineNumber: Number): Number
    fun getLinesContent(): Array<String>
    fun getEOL(): String
    fun getLineMinColumn(lineNumber: Number): Number
    fun getLineMaxColumn(lineNumber: Number): Number
    fun getLineFirstNonWhitespaceColumn(lineNumber: Number): Number
    fun getLineLastNonWhitespaceColumn(lineNumber: Number): Number
    fun validatePosition(position: IPosition): Position
    fun modifyPosition(position: IPosition, offset: Number): Position
    fun validateRange(range: IRange): Range
    fun getOffsetAt(position: IPosition): Number
    fun getPositionAt(offset: Number): Position
    fun getFullModelRange(): Range
    fun isDisposed(): Boolean
    fun findMatches(
        searchString: String,
        searchOnlyEditableRange: Boolean,
        isRegex: Boolean,
        matchCase: Boolean,
        wordSeparators: String?,
        captureMatches: Boolean,
        limitResultCount: Number = definedExternally,
    ): Array<FindMatch>

    fun findMatches(
        searchString: String,
        searchScope: IRange,
        isRegex: Boolean,
        matchCase: Boolean,
        wordSeparators: String?,
        captureMatches: Boolean,
        limitResultCount: Number = definedExternally,
    ): Array<FindMatch>

    fun findMatches(
        searchString: String,
        searchScope: Array<IRange>,
        isRegex: Boolean,
        matchCase: Boolean,
        wordSeparators: String?,
        captureMatches: Boolean,
        limitResultCount: Number = definedExternally,
    ): Array<FindMatch>

    fun findNextMatch(
        searchString: String,
        searchStart: IPosition,
        isRegex: Boolean,
        matchCase: Boolean,
        wordSeparators: String?,
        captureMatches: Boolean,
    ): FindMatch?

    fun findPreviousMatch(
        searchString: String,
        searchStart: IPosition,
        isRegex: Boolean,
        matchCase: Boolean,
        wordSeparators: String?,
        captureMatches: Boolean,
    ): FindMatch?

    fun getModeId(): String
    fun getWordAtPosition(position: IPosition): IWordAtPosition?
    fun getWordUntilPosition(position: IPosition): IWordAtPosition
    fun deltaDecorations(
        oldDecorations: Array<String>,
        newDecorations: Array<IModelDeltaDecoration>,
        ownerId: Number = definedExternally,
    ): Array<String>

    fun getDecorationOptions(id: String): IModelDecorationOptions?
    fun getDecorationRange(id: String): Range?
    fun getLineDecorations(
        lineNumber: Number,
        ownerId: Number = definedExternally,
        filterOutValidation: Boolean = definedExternally,
    ): Array<IModelDecoration>

    fun getLinesDecorations(
        startLineNumber: Number,
        endLineNumber: Number,
        ownerId: Number = definedExternally,
        filterOutValidation: Boolean = definedExternally,
    ): Array<IModelDecoration>

    fun getDecorationsInRange(
        range: IRange,
        ownerId: Number = definedExternally,
        filterOutValidation: Boolean = definedExternally,
    ): Array<IModelDecoration>

    fun getAllDecorations(
        ownerId: Number = definedExternally,
        filterOutValidation: Boolean = definedExternally,
    ): Array<IModelDecoration>

    fun getOverviewRulerDecorations(
        ownerId: Number = definedExternally,
        filterOutValidation: Boolean = definedExternally,
    ): Array<IModelDecoration>

    fun normalizeIndentation(str: String): String
    fun updateOptions(newOpts: ITextModelUpdateOptions)
    fun detectIndentation(defaultInsertSpaces: Boolean, defaultTabSize: Number)
    fun pushStackElement()
    fun pushEditOperations(
        beforeCursorState: Array<Selection>?,
        editOperations: Array<IIdentifiedSingleEditOperation>,
        cursorStateComputer: ICursorStateComputer,
    ): Array<Selection>?

    fun pushEOL(eol: EndOfLineSequence)
    fun applyEdits(operations: Array<IIdentifiedSingleEditOperation>)
    fun applyEdits(
        operations: Array<IIdentifiedSingleEditOperation>,
        computeUndoEdits: Boolean,
    ): dynamic /* Unit | Array */

    fun setEOL(eol: EndOfLineSequence)
    fun onDidChangeContent(listener: (e: IModelContentChangedEvent) -> Unit): IDisposable
    fun onDidChangeDecorations(listener: (e: IModelDecorationsChangedEvent) -> Unit): IDisposable
    fun onDidChangeOptions(listener: (e: IModelOptionsChangedEvent) -> Unit): IDisposable
    fun onDidChangeLanguage(listener: (e: IModelLanguageChangedEvent) -> Unit): IDisposable
    fun onDidChangeLanguageConfiguration(listener: (e: IModelLanguageConfigurationChangedEvent) -> Unit): IDisposable
    fun onWillDispose(listener: () -> Unit): IDisposable
    fun dispose()
}

external object EditorType {
    var ICodeEditor: String
    var IDiffEditor: String
}

external interface IModelLanguageChangedEvent {
    var oldLanguage: String
    var newLanguage: String
}

external interface IModelLanguageConfigurationChangedEvent

external interface IModelContentChange {
    var range: IRange
    var rangeOffset: Number
    var rangeLength: Number
    var text: String
}

external interface IModelContentChangedEvent {
    var changes: Array<IModelContentChange>
    var eol: String
    var versionId: Number
    var isUndoing: Boolean
    var isRedoing: Boolean
    var isFlush: Boolean
}

external interface IModelDecorationsChangedEvent {
    var affectsMinimap: Boolean
    var affectsOverviewRuler: Boolean
}

external interface IModelOptionsChangedEvent {
    var tabSize: Boolean
    var indentSize: Boolean
    var insertSpaces: Boolean
    var trimAutoWhitespace: Boolean
}

sealed external class CursorChangeReason {
    object NotSet : CursorChangeReason /* = 0 */
    object ContentFlush : CursorChangeReason /* = 1 */
    object RecoverFromMarkers : CursorChangeReason /* = 2 */
    object Explicit : CursorChangeReason /* = 3 */
    object Paste : CursorChangeReason /* = 4 */
    object Undo : CursorChangeReason /* = 5 */
    object Redo : CursorChangeReason /* = 6 */
}

external interface ICursorPositionChangedEvent {
    var position: Position
    var secondaryPositions: Array<Position>
    var reason: CursorChangeReason
    var source: String
}

external interface ICursorSelectionChangedEvent {
    var selection: Selection
    var secondarySelections: Array<Selection>
    var modelVersionId: Number
    var oldSelections: Array<Selection>?
    var oldModelVersionId: Number
    var source: String
    var reason: CursorChangeReason
}

sealed external class AccessibilitySupport {
    object Unknown : AccessibilitySupport /* = 0 */
    object Disabled : AccessibilitySupport /* = 1 */
    object Enabled : AccessibilitySupport /* = 2 */
}

sealed external class EditorAutoIndentStrategy {
    object None : EditorAutoIndentStrategy /* = 0 */
    object Keep : EditorAutoIndentStrategy /* = 1 */
    object Brackets : EditorAutoIndentStrategy /* = 2 */
    object Advanced : EditorAutoIndentStrategy /* = 3 */
    object Full : EditorAutoIndentStrategy /* = 4 */
}
