@file:JsModule("monaco-editor")
@file:JsNonModule
@file:Suppress("CovariantEquals", "unused")

package world.phantasmal.web.externals.monacoEditor

external interface IDisposable {
    fun dispose()
}

external enum class MarkerTag {
    Unnecessary /* = 1 */,
    Deprecated /* = 2 */
}

external enum class MarkerSeverity {
    Hint /* = 1 */,
    Info /* = 2 */,
    Warning /* = 4 */,
    Error /* = 8 */
}

external interface IRange {
    var startLineNumber: Number
    var startColumn: Number
    var endLineNumber: Number
    var endColumn: Number
}

open external class Range(
    startLineNumber: Number,
    startColumn: Number,
    endLineNumber: Number,
    endColumn: Number,
) {
    open var startLineNumber: Number
    open var startColumn: Number
    open var endLineNumber: Number
    open var endColumn: Number
    open fun isEmpty(): Boolean
    open fun containsPosition(position: IPosition): Boolean
    open fun containsRange(range: IRange): Boolean
    open fun strictContainsRange(range: IRange): Boolean
    open fun plusRange(range: IRange): Range
    open fun intersectRanges(range: IRange): Range?
    open fun equalsRange(other: IRange?): Boolean
    open fun getEndPosition(): Position
    open fun getStartPosition(): Position
    override fun toString(): String
    open fun setEndPosition(endLineNumber: Number, endColumn: Number): Range
    open fun setStartPosition(startLineNumber: Number, startColumn: Number): Range
    open fun collapseToStart(): Range

    companion object {
        fun isEmpty(range: IRange): Boolean
        fun containsPosition(range: IRange, position: IPosition): Boolean
        fun containsRange(range: IRange, otherRange: IRange): Boolean
        fun strictContainsRange(range: IRange, otherRange: IRange): Boolean
        fun plusRange(a: IRange, b: IRange): Range
        fun intersectRanges(a: IRange, b: IRange): Range?
        fun equalsRange(a: IRange?, b: IRange?): Boolean
        fun getEndPosition(range: IRange): Position
        fun getStartPosition(range: IRange): Position
        fun collapseToStart(range: IRange): Range
        fun fromPositions(start: IPosition, end: IPosition = definedExternally): Range
        fun lift(range: Nothing?): Nothing?
        fun lift(range: IRange): Range
        fun isIRange(obj: Any): Boolean
        fun areIntersectingOrTouching(a: IRange, b: IRange): Boolean
        fun areIntersecting(a: IRange, b: IRange): Boolean
        fun compareRangesUsingStarts(a: IRange?, b: IRange?): Number
        fun compareRangesUsingEnds(a: IRange, b: IRange): Number
        fun spansMultipleLines(range: IRange): Boolean
    }
}

external interface ISelection {
    var selectionStartLineNumber: Number
    var selectionStartColumn: Number
    var positionLineNumber: Number
    var positionColumn: Number
}

open external class Selection(
    selectionStartLineNumber: Number,
    selectionStartColumn: Number,
    positionLineNumber: Number,
    positionColumn: Number,
) : Range {
    open var selectionStartLineNumber: Number
    open var selectionStartColumn: Number
    open var positionLineNumber: Number
    open var positionColumn: Number
    override fun toString(): String
    open fun equalsSelection(other: ISelection): Boolean
    open fun getDirection(): SelectionDirection
    override fun setEndPosition(endLineNumber: Number, endColumn: Number): Selection
    open fun getPosition(): Position
    override fun setStartPosition(startLineNumber: Number, startColumn: Number): Selection

    companion object {
        fun selectionsEqual(a: ISelection, b: ISelection): Boolean
        fun fromPositions(start: IPosition, end: IPosition = definedExternally): Selection
        fun liftSelection(sel: ISelection): Selection
        fun selectionsArrEqual(a: Array<ISelection>, b: Array<ISelection>): Boolean
        fun isISelection(obj: Any): Boolean
        fun createWithDirection(
            startLineNumber: Number,
            startColumn: Number,
            endLineNumber: Number,
            endColumn: Number,
            direction: SelectionDirection,
        ): Selection
    }
}

external enum class SelectionDirection {
    LTR /* = 0 */,
    RTL /* = 1 */
}

external interface IPosition {
    var lineNumber: Number
    var column: Number
}

open external class Position(lineNumber: Number, column: Number) {
    open var lineNumber: Number
    open var column: Number
    open fun with(
        newLineNumber: Number = definedExternally,
        newColumn: Number = definedExternally,
    ): Position

    open fun delta(
        deltaLineNumber: Number = definedExternally,
        deltaColumn: Number = definedExternally,
    ): Position

    open fun equals(other: IPosition): Boolean
    open fun isBefore(other: IPosition): Boolean
    open fun isBeforeOrEqual(other: IPosition): Boolean
    open fun clone(): Position
    override fun toString(): String

    companion object {
        fun equals(a: IPosition?, b: IPosition?): Boolean
        fun isBefore(a: IPosition, b: IPosition): Boolean
        fun isBeforeOrEqual(a: IPosition, b: IPosition): Boolean
        fun compare(a: IPosition, b: IPosition): Number
        fun lift(pos: IPosition): Position
        fun isIPosition(obj: Any): Boolean
    }
}

external interface UriComponents {
    var scheme: String
    var authority: String
    var path: String
    var query: String
    var fragment: String
}

open external class Uri : UriComponents {
    override var scheme: String
    override var authority: String
    override var path: String
    override var query: String
    override var fragment: String
    open fun toString(skipEncoding: Boolean = definedExternally): String
    open fun toJSON(): UriComponents

    companion object {
        fun isUri(thing: Any): Boolean
        fun parse(value: String, _strict: Boolean = definedExternally): Uri
        fun file(path: String): Uri
        fun joinPath(uri: Uri, vararg pathFragment: String): Uri
        fun revive(data: UriComponents): Uri
        fun revive(data: Uri): Uri
        fun revive(data: UriComponents? = definedExternally): Uri?
        fun revive(data: Uri? = definedExternally): Uri?
    }
}
