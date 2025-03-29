package world.phantasmal.web.core.stores

import kotlinx.browser.window
import kotlinx.coroutines.launch
import org.w3c.dom.events.KeyboardEvent
import world.phantasmal.cell.Cell
import world.phantasmal.cell.eq
import world.phantasmal.cell.mutableCell
import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.TrackedDisposable
import world.phantasmal.core.disposable.disposable
import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.core.models.Server
import world.phantasmal.webui.DisposableContainer
import world.phantasmal.webui.dom.disposableListener
import world.phantasmal.webui.stores.Store

/**
 * Represents the current path and parameters without hash (#). E.g. `/viewer/models?model=HUmar`.
 * In production this string is actually appended to the base URL after the hash. The above path and
 * parameters would become `https://www.phantasmal.world/#/viewer/models?model=HUmar`.
 */
interface ApplicationUrl {
    val pathAndParams: String

    fun pushPathAndParams(pathAndParams: String)

    fun replacePathAndParams(pathAndParams: String)

    fun onPopPathAndParams(callback: (String) -> Unit): Disposable
}

interface Param : Disposable {
    val value: String?

    fun set(value: String?)
}

class UiStore(applicationUrl: ApplicationUrl) : Store() {
    private val _server = mutableCell(Server.Ephinea)

    // TODO: Remove this dependency and add it to each component that actually needs it.
    private val navigationStore = addDisposable(NavigationStore(applicationUrl))

    private val globalKeyDownHandlers: MutableMap<String, suspend (e: KeyboardEvent) -> Unit> =
        mutableMapOf()

    private val tools: List<PwToolType> = PwToolType.values().toList()

    /**
     * The tool that is currently visible.
     */
    val currentTool: Cell<PwToolType> get() = navigationStore.currentTool

    /**
     * Map of tools to a boolean cell that says whether they are the current tool or not.
     * At all times, exactly one of these booleans is true.
     */
    val toolToActive: Map<PwToolType, Cell<Boolean>> =
        tools.associateWith { tool -> currentTool eq tool }

    /**
     * Application URL without the tool path prefix.
     * E.g. when the full path is `/viewer/models`, [path] will be `/models`.
     */
    val path: Cell<String> get() = navigationStore.path

    /**
     * The private server we're currently showing data and tools for.
     */
    val server: Cell<Server> get() = _server

    init {
        addDisposables(
            window.disposableListener("keydown", ::dispatchGlobalKeyDown),
        )
    }

    fun setCurrentTool(tool: PwToolType) {
        navigationStore.setCurrentTool(tool)
    }

    /**
     * Updates [path] to [prefix] if the current path doesn't start with [prefix].
     */
    fun setPathPrefix(prefix: String, replace: Boolean) {
        navigationStore.setPathPrefix(prefix, replace)
    }

    fun registerParameter(
        tool: PwToolType,
        path: String,
        parameter: String,
        onChange: (String?) -> Unit,
    ): Param =
        navigationStore.registerParameter(tool, path, parameter, onChange)

    fun onGlobalKeyDown(
        tool: PwToolType,
        binding: String,
        handler: suspend (KeyboardEvent) -> Unit,
    ): Disposable {
        val key = handlerKey(tool, binding)
        require(key !in globalKeyDownHandlers) {
            """Binding "$binding" already exists for tool $tool."""
        }

        globalKeyDownHandlers[key] = handler

        return disposable { globalKeyDownHandlers.remove(key) }
    }

    private fun dispatchGlobalKeyDown(e: KeyboardEvent) {
        val bindingParts = mutableListOf<String>()

        if (e.ctrlKey || e.metaKey) bindingParts.add("Ctrl")  // Map both Ctrl and Cmd to "Ctrl" for compatibility
        if (e.altKey) bindingParts.add("Alt")
        if (e.shiftKey) bindingParts.add("Shift")
        bindingParts.add(e.key.uppercase())

        val binding = bindingParts.joinToString("-")

        val handler = globalKeyDownHandlers[handlerKey(currentTool.value, binding)]

        if (handler != null) {
            e.preventDefault()
            scope.launch { handler(e) }
        }
    }

    private fun handlerKey(tool: PwToolType, binding: String): String {
        return "$tool -> $binding"
    }

    companion object {
        /**
         * The default tool that's loaded if the initial URL has no specific path.
         */
        val DEFAULT_TOOL: PwToolType = PwToolType.Viewer
    }
}

/**
 * Deconstructs the URL into [currentTool], [path], [parameters] and [features], propagates changes
 * to the URL (e.g. when the user navigates backward or forward through the browser history) to the
 * rest of the application and allows the application to update the URL.
 */
class NavigationStore(private val applicationUrl: ApplicationUrl) : DisposableContainer() {

    private val _currentTool = mutableCell(UiStore.DEFAULT_TOOL)

    /**
     * The tool that is currently visible.
     */
    val currentTool: Cell<PwToolType> = _currentTool

    private val _path = mutableCell("")

    /**
     * Application URL without the tool path prefix.
     * E.g. when the full path is `/viewer/models`, [path] will be `/models`.
     */
    val path: Cell<String> = _path

    /**
     * Maps full paths to maps of parameters and their values. In other words we keep track of
     * parameter values per full path.
     */
    private val parameters: MutableMap<String, MutableMap<String, ParamValue>> =
        mutableMapOf()

    /**
     * Enabled alpha features. Alpha features can be turned on by adding a features parameter with
     * the comma-separated feature names as value.
     * E.g. `/viewer?features=f1,f2,f3` to enable features f1, f2 and f3.
     */
    private val features: MutableSet<String> = mutableSetOf()

    init {
        deconstructPathAndParams(applicationUrl.pathAndParams)
        addDisposable(applicationUrl.onPopPathAndParams(::deconstructPathAndParams))
    }

    fun setCurrentTool(tool: PwToolType) {
        if (tool != currentTool.value) {
            updateApplicationUrl(tool, path = "", replace = false)
            setCurrentTool(tool, path = "")
        }
    }

    private fun setCurrentTool(tool: PwToolType, path: String) {
        _path.value = path
        _currentTool.value = tool
    }

    /**
     * Updates [path] to [prefix] if the current path doesn't start with [prefix].
     */
    fun setPathPrefix(prefix: String, replace: Boolean) {
        if (!path.value.startsWith(prefix)) {
            updateApplicationUrl(currentTool.value, prefix, replace)
            _path.value = prefix
        }
    }

    fun registerParameter(
        tool: PwToolType,
        path: String,
        parameter: String,
        onChange: (String?) -> Unit,
    ): Param {
        require(parameter !== FEATURES_PARAM) {
            "$FEATURES_PARAM can't be set because it is a global parameter."
        }

        val pathParams = parameters.getOrPut("/${tool.slug}$path", ::mutableMapOf)
        val paramCtx =
            pathParams.getOrPut(parameter) { ParamValue(value = null, onChange = null) }

        require(paramCtx.onChange == null) {
            "Parameter $parameter is already registered."
        }

        return ParamImpl(paramCtx, tool, path, onChange)
    }

    /**
     * Sets [currentTool], [path], [parameters] and [features].
     */
    private fun deconstructPathAndParams(url: String) {
        val urlSplit = url.split("?")
        val fullPath = urlSplit[0]
        val paramsStr = urlSplit.getOrNull(1)
        val secondSlashIdx = fullPath.indexOf("/", 1)
        val toolStr =
            if (secondSlashIdx == -1) fullPath.substring(1)
            else fullPath.substring(1, secondSlashIdx)

        val tool = SLUG_TO_PW_TOOL[toolStr]
        val path = if (secondSlashIdx == -1) "" else fullPath.substring(secondSlashIdx)

        if (paramsStr != null) {
            val params = parameters.getOrPut(fullPath, ::mutableMapOf)

            for (paramNameAndValue in paramsStr.split("&")) {
                val paramNameAndValueSplit = paramNameAndValue.split("=", limit = 2)
                val paramName = paramNameAndValueSplit[0]
                val value = paramNameAndValueSplit.getOrNull(1)

                if (paramName == FEATURES_PARAM) {
                    if (value != null) {
                        for (feature in value.split(",")) {
                            features.add(feature)
                        }
                    }
                } else {
                    val param = params[paramName]

                    if (param == null) {
                        params[paramName] = ParamValue(value, onChange = null)
                    } else {
                        param.updateAndCallOnChange(value)
                    }
                }
            }
        }

        val actualTool = tool ?: UiStore.DEFAULT_TOOL
        this.setCurrentTool(actualTool, path)

        if (tool == null) {
            updateApplicationUrl(actualTool, path, replace = true)
        }
    }

    // TODO: Use buildString.
    private fun updateApplicationUrl(tool: PwToolType, path: String, replace: Boolean) {
        val fullPath = "/${tool.slug}${path}"
        val params = mutableMapOf<String, String>()

        parameters[fullPath]?.forEach { (k, v) ->
            v.value?.let { params[k] = it }
        }

        if (features.isNotEmpty()) {
            params[FEATURES_PARAM] = features.joinToString(",")
        }

        val paramStr =
            if (params.isEmpty()) ""
            else "?" + params.map { (k, v) -> "$k=$v" }.joinToString("&")

        val url = "${fullPath}${paramStr}"

        if (replace) {
            applicationUrl.replacePathAndParams(url)
        } else {
            applicationUrl.pushPathAndParams(url)
        }
    }

    companion object {
        private const val FEATURES_PARAM = "features"
        private val SLUG_TO_PW_TOOL: Map<String, PwToolType> =
            PwToolType.values().associateBy { it.slug }
    }

    private class ParamValue(
        value: String?,
        var onChange: ((String?) -> Unit)?,
    ) {
        var value: String? = value
            private set

        fun update(value: String?): Boolean {
            val changed = value != this.value
            this.value = value
            return changed
        }

        fun updateAndCallOnChange(value: String?) {
            if (update(value)) {
                onChange?.invoke(value)
            }
        }
    }

    private inner class ParamImpl(
        private val paramValue: ParamValue,
        private val tool: PwToolType,
        private val paramPath: String,
        onChange: (String?) -> Unit,
    ) : TrackedDisposable(), Param {

        override val value: String? get() = paramValue.value

        init {
            require(paramValue.onChange == null)
            paramValue.onChange = onChange
        }

        override fun set(value: String?) {
            // Always update parameter value.
            if (paramValue.update(value)) {
                // Only update URL if current part of the tool is visible.
                if (currentTool.value == tool && path.value == paramPath) {
                    updateApplicationUrl(tool, paramPath, replace = true)
                }
            }
        }

        override fun dispose() {
            paramValue.onChange = null
            super.dispose()
        }
    }
}
