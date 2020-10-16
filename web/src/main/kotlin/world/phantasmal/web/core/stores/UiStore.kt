package world.phantasmal.web.core.stores

import kotlinx.browser.window
import org.w3c.dom.events.KeyboardEvent
import world.phantasmal.core.disposable.Scope
import world.phantasmal.observable.value.MutableVal
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.mutableVal
import world.phantasmal.web.core.models.Server
import world.phantasmal.webui.dom.disposableListener
import world.phantasmal.webui.stores.Store

/**
 * Phantasmal World consists of several tools.
 */
enum class PwTool(val uiName: String, val slug: String) {
    Viewer("Viewer", "viewer"),
    QuestEditor("Quest Editor", "quest_editor"),
    HuntOptimizer("Hunt Optimizer", "hunt_optimizer"),
}

interface ApplicationUrl {
    val url: Val<String>

    fun pushUrl(url: String)

    fun replaceUrl(url: String)
}

class UiStore(scope: Scope, private val applicationUrl: ApplicationUrl) : Store(scope) {
    private val _currentTool: MutableVal<PwTool>

    private val _path = mutableVal("")
    private val _server = mutableVal(Server.Ephinea)

    /**
     * Maps full paths to maps of parameters and their values. In other words we keep track of
     * parameter values per [applicationUrl].
     */
    private val parameters: MutableMap<String, Map<String, String>> = mutableMapOf()
    private val globalKeydownHandlers: MutableMap<String, (e: KeyboardEvent) -> Unit> =
        mutableMapOf()

    /**
     * Enabled alpha features. Alpha features can be turned on by adding a features parameter with
     * the comma-separated feature names as value.
     * E.g. `/viewer?features=f1,f2,f3`
     */
    private val features: MutableSet<String> = mutableSetOf()

    val tools: List<PwTool> = PwTool.values().toList()

    /**
     * The default tool that is loaded.
     */
    val defaultTool: PwTool = PwTool.Viewer

    /**
     * The tool that is current visible.
     */
    val currentTool: Val<PwTool>

    /**
     * Map of tools to a boolean Val that says whether they are the current tool or not.
     */
    val toolToActive: Map<PwTool, Val<Boolean>>

    /**
     * Application URL without the tool path prefix.
     */
    val path: Val<String> = _path

    /**
     * The private server we're currently showing data and tools for.
     */
    val server: Val<Server> = _server

    init {
        _currentTool = mutableVal(defaultTool)
        currentTool = _currentTool

        toolToActive = tools
            .map { tool ->
                tool to currentTool.transform { it == tool }
            }
            .toMap()

        disposableListener(scope, window, "keydown", ::dispatchGlobalKeydown)
        applicationUrl.url.observe(scope, callNow = true) { setDataFromUrl(it.value) }
    }

    fun setCurrentTool(tool: PwTool) {
        if (tool != currentTool.value) {
            updateApplicationUrl(tool, path = "", replace = false)
            setCurrentTool(tool, path = "")
        }
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

    /**
     * Sets [currentTool], [path], [parameters] and [features].
     */
    private fun setDataFromUrl(url: String) {
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
            val params = mutableMapOf<String, String>()

            for (p in paramsStr.split("&")) {
                val (param, value) = p.split("=", limit = 2)

                if (param == "features") {
                    for (feature in value.split(",")) {
                        features.add(feature)
                    }
                } else {
                    params[param] = value
                }
            }

            parameters[fullPath] = params
        }

        val actualTool = tool ?: defaultTool
        this.setCurrentTool(actualTool, path)

        if (tool == null) {
            updateApplicationUrl(actualTool, path, replace = true)
        }
    }

    private fun setCurrentTool(tool: PwTool, path: String) {
        _path.value = path
        _currentTool.value = tool
    }

    private fun updateApplicationUrl(tool: PwTool, path: String, replace: Boolean) {
        val fullPath = "/${tool.slug}${path}"
        val params: MutableMap<String, String> =
            parameters[fullPath]?.let { HashMap(it) } ?: mutableMapOf()

        if (features.isNotEmpty()) {
            params["features"] = features.joinToString(",")
        }

        val paramStr =
            if (params.isEmpty()) ""
            else "?" + params.map { (k, v) -> "$k=$v" }.joinToString("&")

        val url = "${fullPath}${paramStr}"

        if (replace) {
            applicationUrl.replaceUrl(url)
        } else {
            applicationUrl.pushUrl(url)
        }
    }

    private fun dispatchGlobalKeydown(e: KeyboardEvent) {
        val bindingParts = mutableListOf<String>()

        if (e.ctrlKey) bindingParts.add("Ctrl")
        if (e.shiftKey) bindingParts.add("Shift")
        if (e.altKey) bindingParts.add("Alt")
        bindingParts.add(e.key.toUpperCase())

        val binding = bindingParts.joinToString("-")

        val handler = globalKeydownHandlers[handlerKey(currentTool.value, binding)]

        if (handler != null) {
            e.preventDefault()
            handler(e)
        }
    }

    private fun handlerKey(tool: PwTool, binding: String): String {
        return "$tool -> $binding"
    }

    companion object {
        private val SLUG_TO_PW_TOOL: Map<String, PwTool> =
            PwTool.values().map { it.slug to it }.toMap()
    }
}
