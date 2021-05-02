package world.phantasmal.web.viewer.stores

import kotlinx.coroutines.launch
import mu.KotlinLogging
import world.phantasmal.core.enumValueOfOrNull
import world.phantasmal.lib.fileFormats.AreaGeometry
import world.phantasmal.lib.fileFormats.CollisionGeometry
import world.phantasmal.lib.fileFormats.ninja.NinjaObject
import world.phantasmal.lib.fileFormats.ninja.NjMotion
import world.phantasmal.lib.fileFormats.ninja.NjObject
import world.phantasmal.lib.fileFormats.ninja.XvrTexture
import world.phantasmal.observable.cell.Cell
import world.phantasmal.observable.cell.and
import world.phantasmal.observable.cell.list.ListCell
import world.phantasmal.observable.cell.list.mutableListCell
import world.phantasmal.observable.cell.mutableCell
import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.core.rendering.conversion.PSO_FRAME_RATE
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.shared.dto.SectionId
import world.phantasmal.web.viewer.ViewerUrls
import world.phantasmal.web.viewer.loading.AnimationAssetLoader
import world.phantasmal.web.viewer.loading.CharacterClassAssetLoader
import world.phantasmal.web.viewer.models.AnimationModel
import world.phantasmal.web.viewer.models.CharacterClass
import world.phantasmal.webui.stores.Store

private val logger = KotlinLogging.logger {}

sealed class NinjaGeometry {
    class Object(val obj: NinjaObject<*, *>) : NinjaGeometry()
    class Render(val geometry: AreaGeometry) : NinjaGeometry()
    class Collision(val geometry: CollisionGeometry) : NinjaGeometry()
}

class ViewerStore(
    private val characterClassAssetLoader: CharacterClassAssetLoader,
    private val animationAssetLoader: AnimationAssetLoader,
    uiStore: UiStore,
) : Store() {
    // Ninja concepts.
    private val _currentNinjaGeometry = mutableCell<NinjaGeometry?>(null)
    private val _currentTextures = mutableListCell<XvrTexture?>()
    private val _currentNinjaMotion = mutableCell<NjMotion?>(null)

    // High-level concepts.
    private val _currentCharacterClass =
        mutableCell<CharacterClass?>(CharacterClass.VALUES.random())
    private val _currentSectionId = mutableCell(SectionId.VALUES.random())
    private val _currentBody =
        mutableCell((1.._currentCharacterClass.value!!.bodyStyleCount).random())
    private val _currentAnimation = mutableCell<AnimationModel?>(null)

    // Settings.
    private val _applyTextures = mutableCell(true)
    private val _showSkeleton = mutableCell(false)
    private val _animationPlaying = mutableCell(true)
    private val _frameRate = mutableCell(PSO_FRAME_RATE)
    private val _frame = mutableCell(0)

    // Ninja concepts.
    val currentNinjaGeometry: Cell<NinjaGeometry?> = _currentNinjaGeometry
    val currentTextures: ListCell<XvrTexture?> = _currentTextures
    val currentNinjaMotion: Cell<NjMotion?> = _currentNinjaMotion

    // High-level concepts.
    val currentCharacterClass: Cell<CharacterClass?> = _currentCharacterClass
    val currentSectionId: Cell<SectionId> = _currentSectionId
    val currentBody: Cell<Int> = _currentBody
    val animations: List<AnimationModel> = (0 until 572).map {
        AnimationModel(
            "Animation ${it + 1}",
            "/player/animation/animation_${it.toString().padStart(3, '0')}.njm",
        )
    }
    val currentAnimation: Cell<AnimationModel?> = _currentAnimation

    // Settings.
    val applyTexturesEnabled: Cell<Boolean> = _currentNinjaGeometry.map {
        it == null || it !is NinjaGeometry.Collision
    }
    val applyTextures: Cell<Boolean> = applyTexturesEnabled and _applyTextures
    val showSkeletonEnabled: Cell<Boolean> = _currentNinjaGeometry.map {
        it is NinjaGeometry.Object && it.obj is NjObject
    }
    val showSkeleton: Cell<Boolean> = showSkeletonEnabled and _showSkeleton
    val animationPlaying: Cell<Boolean> = _animationPlaying
    val frameRate: Cell<Int> = _frameRate
    val frame: Cell<Int> = _frame

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

        scope.launch {
            loadCharacterClassNinjaObject(clearAnimation = true)
        }
    }

    fun setCurrentNinjaGeometry(geometry: NinjaGeometry?) {
        if (_currentCharacterClass.value != null) {
            _currentCharacterClass.value = null
            _currentTextures.clear()
        }

        _currentAnimation.value = null
        _currentNinjaMotion.value = null
        _currentNinjaGeometry.value = geometry
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
        _animationPlaying.value = true
    }

    suspend fun setCurrentAnimation(animation: AnimationModel?) {
        _currentAnimation.value = animation

        if (animation == null) {
            _currentNinjaMotion.value = null
        } else {
            loadAnimation(animation)
        }
    }

    fun setApplyTextures(apply: Boolean) {
        _applyTextures.value = apply
    }

    fun setShowSkeleton(show: Boolean) {
        _showSkeleton.value = show
    }

    fun setAnimationPlaying(playing: Boolean) {
        _animationPlaying.value = playing
    }

    fun setFrameRate(frameRate: Int) {
        _frameRate.value = frameRate
    }

    fun setFrame(frame: Int) {
        val maxFrame = currentNinjaMotion.value?.frameCount ?: Int.MAX_VALUE

        _frame.value = when {
            frame > maxFrame -> 1
            frame < 1 -> maxFrame
            else -> frame
        }
    }

    private suspend fun loadCharacterClassNinjaObject(clearAnimation: Boolean) {
        val char = currentCharacterClass.value
            ?: return

        val sectionId = currentSectionId.value
        val body = currentBody.value

        try {
            val ninjaObject = characterClassAssetLoader.loadNinjaObject(char)
            val textures = characterClassAssetLoader.loadXvrTextures(char, sectionId, body)

            if (clearAnimation) {
                _currentAnimation.value = null
                _currentNinjaMotion.value = null
            }

            _currentNinjaGeometry.value = NinjaGeometry.Object(ninjaObject)
            _currentTextures.replaceAll(textures)
        } catch (e: Exception) {
            logger.error(e) { "Couldn't load Ninja model for $char." }

            _currentAnimation.value = null
            _currentNinjaMotion.value = null
            _currentNinjaGeometry.value = null
            _currentTextures.clear()
        }
    }

    private suspend fun loadAnimation(animation: AnimationModel) {
        try {
            _currentNinjaMotion.value = animationAssetLoader.loadAnimation(animation.filePath)
            _animationPlaying.value = true
        } catch (e: Exception) {
            logger.error(e) {
                "Couldn't load Ninja motion for ${animation.name} (path: ${animation.filePath})."
            }

            _currentNinjaMotion.value = null
        }
    }

    companion object {
        private const val MODEL_PARAM = "model"
        private const val BODY_PARAM = "body"
        private const val SECTION_ID_PARAM = "section_id"
    }
}
