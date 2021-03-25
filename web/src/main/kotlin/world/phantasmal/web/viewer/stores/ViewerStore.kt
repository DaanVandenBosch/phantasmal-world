package world.phantasmal.web.viewer.stores

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import world.phantasmal.core.enumValueOfOrNull
import world.phantasmal.lib.fileFormats.ninja.NinjaObject
import world.phantasmal.lib.fileFormats.ninja.NjMotion
import world.phantasmal.lib.fileFormats.ninja.XvrTexture
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.list.ListVal
import world.phantasmal.observable.value.list.mutableListVal
import world.phantasmal.observable.value.mutableVal
import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.shared.dto.SectionId
import world.phantasmal.web.viewer.ViewerUrls
import world.phantasmal.web.viewer.loading.CharacterClassAssetLoader
import world.phantasmal.web.viewer.models.CharacterClass
import world.phantasmal.webui.stores.Store

private val logger = KotlinLogging.logger {}

class ViewerStore(
    private val assetLoader: CharacterClassAssetLoader,
    uiStore: UiStore,
) : Store() {
    private val _currentNinjaObject = mutableVal<NinjaObject<*>?>(null)
    private val _currentTextures = mutableListVal<XvrTexture?>()
    private val _currentCharacterClass = mutableVal<CharacterClass?>(CharacterClass.VALUES.random())
    private val _currentSectionId = mutableVal(SectionId.VALUES.random())
    private val _currentBody =
        mutableVal((1.._currentCharacterClass.value!!.bodyStyleCount).random())
    private val _currentNinjaMotion = mutableVal<NjMotion?>(null)

    // Settings
    private val _showSkeleton = mutableVal(false)

    val currentNinjaObject: Val<NinjaObject<*>?> = _currentNinjaObject
    val currentTextures: ListVal<XvrTexture?> = _currentTextures
    val currentCharacterClass: Val<CharacterClass?> = _currentCharacterClass
    val currentSectionId: Val<SectionId> = _currentSectionId
    val currentBody: Val<Int> = _currentBody
    val currentNinjaMotion: Val<NjMotion?> = _currentNinjaMotion

    // Settings
    val showSkeleton: Val<Boolean> = _showSkeleton

    init {
        for (path in listOf(ViewerUrls.mesh, ViewerUrls.texture)) {
            addDisposables(
                uiStore.registerParameter(
                    PwToolType.Viewer,
                    path,
                    MODEL_PARAM,
                    setInitialValue = { initialValue ->
                        if (uiStore.path.value.startsWith(path)) {
                            CharacterClass.VALUES.find { it.slug == initialValue }?.let {
                                _currentCharacterClass.value = it
                            }
                        }
                    },
                    value = currentCharacterClass.map { it?.slug },
                    onChange = { newValue ->
                        scope.launch {
                            setCurrentCharacterClass(
                                CharacterClass.VALUES.find { it.slug == newValue },
                            )
                        }
                    },
                ),

                uiStore.registerParameter(
                    PwToolType.Viewer,
                    path,
                    SECTION_ID_PARAM,
                    setInitialValue = { initialValue ->
                        if (uiStore.path.value.startsWith(path)) {
                            initialValue?.let { enumValueOfOrNull<SectionId>(it) }?.let {
                                _currentSectionId.value = it
                            }
                        }
                    },
                    value = currentSectionId.map { it.name },
                    onChange = { newValue ->
                        scope.launch {
                            setCurrentSectionId(
                                newValue?.let { enumValueOfOrNull<SectionId>(it) }
                                    ?: SectionId.VALUES.random()
                            )
                        }
                    },
                ),

                uiStore.registerParameter(
                    PwToolType.Viewer,
                    path,
                    BODY_PARAM,
                    setInitialValue = { initialValue ->
                        if (uiStore.path.value.startsWith(path)) {
                            val maxBody = _currentCharacterClass.value?.bodyStyleCount ?: 1

                            initialValue?.toIntOrNull()?.takeIf { it <= maxBody }?.let {
                                _currentBody.value = it - 1
                            }
                        }
                    },
                    value = currentBody.map { (it + 1).toString() },
                    onChange = { newValue ->
                        scope.launch {
                            setCurrentBody((newValue?.toIntOrNull() ?: 1) - 1)
                        }
                    },
                ),
            )
        }

        scope.launch(Dispatchers.Default) {
            loadCharacterClassNinjaObject(clearAnimation = true)
        }
    }

    fun setCurrentNinjaObject(ninjaObject: NinjaObject<*>?) {
        if (_currentCharacterClass.value != null) {
            _currentCharacterClass.value = null
            _currentTextures.clear()
        }

        _currentNinjaMotion.value = null
        _currentNinjaObject.value = ninjaObject
    }

    fun setCurrentTextures(textures: List<XvrTexture>) {
        _currentTextures.replaceAll(textures)
    }

    suspend fun setCurrentCharacterClass(char: CharacterClass?) {
        val clearAnimation = _currentCharacterClass.value == null

        _currentCharacterClass.value = char

        if (char != null && _currentBody.value >= char.bodyStyleCount) {
            _currentBody.value = char.bodyStyleCount - 1
        }

        loadCharacterClassNinjaObject(clearAnimation)
    }

    suspend fun setCurrentSectionId(sectionId: SectionId) {
        _currentSectionId.value = sectionId
        loadCharacterClassNinjaObject(clearAnimation = false)
    }

    suspend fun setCurrentBody(body: Int) {
        _currentBody.value = body
        loadCharacterClassNinjaObject(clearAnimation = false)
    }

    fun setCurrentNinjaMotion(njm: NjMotion) {
        _currentNinjaMotion.value = njm
    }

    fun setShowSkeleton(show: Boolean) {
        _showSkeleton.value = show
    }

    private suspend fun loadCharacterClassNinjaObject(clearAnimation: Boolean) {
        val char = currentCharacterClass.value
            ?: return

        val sectionId = currentSectionId.value
        val body = currentBody.value

        try {
            val ninjaObject = assetLoader.loadNinjaObject(char)
            val textures = assetLoader.loadXvrTextures(char, sectionId, body)

            withContext(Dispatchers.Main) {
                if (clearAnimation) {
                    _currentNinjaMotion.value = null
                }

                _currentNinjaObject.value = ninjaObject
                _currentTextures.replaceAll(textures)
            }
        } catch (e: Exception) {
            logger.error(e) { "Couldn't load Ninja model for $char." }

            withContext(Dispatchers.Main) {
                _currentNinjaMotion.value = null
                _currentNinjaObject.value = null
                _currentTextures.clear()
            }
        }
    }

    companion object {
        private const val MODEL_PARAM = "model"
        private const val BODY_PARAM = "body"
        private const val SECTION_ID_PARAM = "section_id"
    }
}
