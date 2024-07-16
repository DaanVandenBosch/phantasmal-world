@file:Suppress("FunctionName")

package world.phantasmal.web.externals.javascriptLpSolver

external interface IModel {
    var optimize: String

    /**
     * "max" or "min".
     */
    var opType: String
    var constraints: dynamic
    var variables: dynamic
}

@JsModule("javascript-lp-solver")
@JsNonModule
external object Solver {
    fun Solve(model: IModel): dynamic
}
