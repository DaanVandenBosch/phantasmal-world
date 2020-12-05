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

external object KeyCode {
    /**
     * Placed first to cover the 0 value of the enum.
     */
    val Unknown: Int /* = 0 */
    val Backspace: Int /* = 1 */
    val Tab: Int /* = 2 */
    val Enter: Int /* = 3 */
    val Shift: Int /* = 4 */
    val Ctrl: Int /* = 5 */
    val Alt: Int /* = 6 */
    val PauseBreak: Int /* = 7 */
    val CapsLock: Int /* = 8 */
    val Escape: Int /* = 9 */
    val Space: Int /* = 10 */
    val PageUp: Int /* = 11 */
    val PageDown: Int /* = 12 */
    val End: Int /* = 13 */
    val Home: Int /* = 14 */
    val LeftArrow: Int /* = 15 */
    val UpArrow: Int /* = 16 */
    val RightArrow: Int /* = 17 */
    val DownArrow: Int /* = 18 */
    val Insert: Int /* = 19 */
    val Delete: Int /* = 20 */
    val KEY_0: Int /* = 21 */
    val KEY_1: Int /* = 22 */
    val KEY_2: Int /* = 23 */
    val KEY_3: Int /* = 24 */
    val KEY_4: Int /* = 25 */
    val KEY_5: Int /* = 26 */
    val KEY_6: Int /* = 27 */
    val KEY_7: Int /* = 28 */
    val KEY_8: Int /* = 29 */
    val KEY_9: Int /* = 30 */
    val KEY_A: Int /* = 31 */
    val KEY_B: Int /* = 32 */
    val KEY_C: Int /* = 33 */
    val KEY_D: Int /* = 34 */
    val KEY_E: Int /* = 35 */
    val KEY_F: Int /* = 36 */
    val KEY_G: Int /* = 37 */
    val KEY_H: Int /* = 38 */
    val KEY_I: Int /* = 39 */
    val KEY_J: Int /* = 40 */
    val KEY_K: Int /* = 41 */
    val KEY_L: Int /* = 42 */
    val KEY_M: Int /* = 43 */
    val KEY_N: Int /* = 44 */
    val KEY_O: Int /* = 45 */
    val KEY_P: Int /* = 46 */
    val KEY_Q: Int /* = 47 */
    val KEY_R: Int /* = 48 */
    val KEY_S: Int /* = 49 */
    val KEY_T: Int /* = 50 */
    val KEY_U: Int /* = 51 */
    val KEY_V: Int /* = 52 */
    val KEY_W: Int /* = 53 */
    val KEY_X: Int /* = 54 */
    val KEY_Y: Int /* = 55 */
    val KEY_Z: Int /* = 56 */
    val Meta: Int /* = 57 */
    val ContextMenu: Int /* = 58 */
    val F1: Int /* = 59 */
    val F2: Int /* = 60 */
    val F3: Int /* = 61 */
    val F4: Int /* = 62 */
    val F5: Int /* = 63 */
    val F6: Int /* = 64 */
    val F7: Int /* = 65 */
    val F8: Int /* = 66 */
    val F9: Int /* = 67 */
    val F10: Int /* = 68 */
    val F11: Int /* = 69 */
    val F12: Int /* = 70 */
    val F13: Int /* = 71 */
    val F14: Int /* = 72 */
    val F15: Int /* = 73 */
    val F16: Int /* = 74 */
    val F17: Int /* = 75 */
    val F18: Int /* = 76 */
    val F19: Int /* = 77 */
    val NumLock: Int /* = 78 */
    val ScrollLock: Int /* = 79 */

    /**
     * Used for miscellaneous characters; it can vary by keyboard.
     * For the US standard keyboard, the ';:' key
     */
    val US_SEMICOLON: Int /* = 80 */

    /**
     * For any country/region, the '+' key
     * For the US standard keyboard, the '=+' key
     */
    val US_EQUAL: Int /* = 81 */

    /**
     * For any country/region, the ',' key
     * For the US standard keyboard, the ',<' key
     */
    val US_COMMA: Int /* = 82 */

    /**
     * For any country/region, the '-' key
     * For the US standard keyboard, the '-_' key
     */
    val US_MINUS: Int /* = 83 */

    /**
     * For any country/region, the '.' key
     * For the US standard keyboard, the '.>' key
     */
    val US_DOT: Int /* = 84 */

    /**
     * Used for miscellaneous characters; it can vary by keyboard.
     * For the US standard keyboard, the '/?' key
     */
    val US_SLASH: Int /* = 85 */

    /**
     * Used for miscellaneous characters; it can vary by keyboard.
     * For the US standard keyboard, the '`~' key
     */
    val US_BACKTICK: Int /* = 86 */

    /**
     * Used for miscellaneous characters; it can vary by keyboard.
     * For the US standard keyboard, the '[{' key
     */
    val US_OPEN_SQUARE_BRACKET: Int /* = 87 */

    /**
     * Used for miscellaneous characters; it can vary by keyboard.
     * For the US standard keyboard, the '\|' key
     */
    val US_BACKSLASH: Int /* = 88 */

    /**
     * Used for miscellaneous characters; it can vary by keyboard.
     * For the US standard keyboard, the ']}' key
     */
    val US_CLOSE_SQUARE_BRACKET: Int /* = 89 */

    /**
     * Used for miscellaneous characters; it can vary by keyboard.
     * For the US standard keyboard, the ''"' key
     */
    val US_QUOTE: Int /* = 90 */

    /**
     * Used for miscellaneous characters; it can vary by keyboard.
     */
    val OEM_8: Int /* = 91 */

    /**
     * Either the angle bracket key or the backslash key on the RT 102-key keyboard.
     */
    val OEM_102: Int /* = 92 */
    val NUMPAD_0: Int /* = 93 */
    val NUMPAD_1: Int /* = 94 */
    val NUMPAD_2: Int /* = 95 */
    val NUMPAD_3: Int /* = 96 */
    val NUMPAD_4: Int /* = 97 */
    val NUMPAD_5: Int /* = 98 */
    val NUMPAD_6: Int /* = 99 */
    val NUMPAD_7: Int /* = 100 */
    val NUMPAD_8: Int /* = 101 */
    val NUMPAD_9: Int /* = 102 */
    val NUMPAD_MULTIPLY: Int /* = 103 */
    val NUMPAD_ADD: Int /* = 104 */
    val NUMPAD_SEPARATOR: Int /* = 105 */
    val NUMPAD_SUBTRACT: Int /* = 106 */
    val NUMPAD_DECIMAL: Int /* = 107 */
    val NUMPAD_DIVIDE: Int /* = 108 */

    /**
     * Cover all key codes when IME is processing input.
     */
    val KEY_IN_COMPOSITION: Int /* = 109 */
    val ABNT_C1: Int /* = 110 */
    val ABNT_C2: Int /* = 111 */

    /**
     * Placed last to cover the length of the enum.
     * Please do not depend on this value!
     */
    val MAX_VALUE: Int /* = 112 */
}

external object KeyMod {
    val CtrlCmd: Int
    val Shift: Int
    val Alt: Int
    val WinCtrl: Int

    fun chord(firstPart: Int, secondPart: Int): Int
}
