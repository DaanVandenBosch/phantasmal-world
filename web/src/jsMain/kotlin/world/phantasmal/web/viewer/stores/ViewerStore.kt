package world.phantasmal.web.viewer.stores

import kotlinx.coroutines.launch
import mu.KotlinLogging
import world.phantasmal.core.enumValueOfOrNull
import world.phantasmal.core.math.clamp
import world.phantasmal.cell.Cell
import world.phantasmal.cell.and
import world.phantasmal.cell.list.ListCell
import world.phantasmal.cell.list.mutableListCell
import world.phantasmal.cell.map
import world.phantasmal.cell.mutableCell
import world.phantasmal.cell.mutate
import world.phantasmal.psolib.fileFormats.AreaGeometry
import world.phantasmal.psolib.fileFormats.CollisionGeometry
import world.phantasmal.psolib.fileFormats.ninja.NinjaObject
import world.phantasmal.psolib.fileFormats.ninja.NjMotion
import world.phantasmal.psolib.fileFormats.ninja.NjObject
import world.phantasmal.psolib.fileFormats.ninja.XvrTexture
import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.core.rendering.conversion.PSO_FRAME_RATE
import world.phantasmal.web.core.stores.Param
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.shared.dto.SectionId
import world.phantasmal.web.viewer.ViewerUrls
import world.phantasmal.web.viewer.loading.AnimationAssetLoader
import world.phantasmal.web.viewer.loading.CharacterClassAssetLoader
import world.phantasmal.web.viewer.loading.NpcAssetLoader
import world.phantasmal.web.viewer.models.AnimationModel
import world.phantasmal.web.viewer.models.CharacterClass
import world.phantasmal.web.viewer.models.ViewerModel
import world.phantasmal.webui.stores.Store

private val logger = KotlinLogging.logger {}

sealed class NinjaGeometry {
    class Object(val obj: NinjaObject<*, *>) : NinjaGeometry()
    class Render(val geometry: AreaGeometry) : NinjaGeometry()
    class Collision(val geometry: CollisionGeometry) : NinjaGeometry()
}

class ViewerStore(
    private val characterClassAssetLoader: CharacterClassAssetLoader,
    private val npcAssetLoader: NpcAssetLoader,
    private val animationAssetLoader: AnimationAssetLoader,
    uiStore: UiStore,
) : Store() {
    // Ninja concepts.
    private val _currentNinjaGeometry = mutableCell<NinjaGeometry?>(null)
    private val _currentTextures = mutableListCell<XvrTexture?>()
    private val _currentNinjaMotion = mutableCell<NjMotion?>(null)

    // High-level concepts.
    private val _currentModel =
        mutableCell<ViewerModel?>(ViewerModel.Character(CharacterClass.VALUES.random()))
    private val _currentSectionId = mutableCell(SectionId.VALUES.random())
    private val _currentBody =
        mutableCell(
            (0 until ((_currentModel.value as? ViewerModel.Character)
                ?.characterClass?.bodyStyleCount ?: 1)).random()
        )
    private val _currentAnimation = mutableCell<AnimationModel?>(null)

    // Params.
    private val modelParams = mutableListOf<Param>()
    private val sectionIdParams = mutableListOf<Param>()
    private val bodyParams = mutableListOf<Param>()

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
    val currentModel: Cell<ViewerModel?> = _currentModel
    val currentCharacterClass: Cell<CharacterClass?> = _currentModel.map {
        (it as? ViewerModel.Character)?.characterClass
    }
    val currentSectionId: Cell<SectionId> = _currentSectionId
    val currentBody: Cell<Int> = _currentBody
    private val playerAnimations: List<AnimationModel> = (0 until 572).map {
        AnimationModel(
            "Animation ${it + 1}",
            "/player/animation/animation_${it.toString().padStart(3, '0')}.njm",
        )
    }
    val animations: Cell<List<AnimationModel>> = _currentModel.map { model ->
        when (model) {
            is ViewerModel.Character -> playerAnimations
            is ViewerModel.Npc -> NpcAssetLoader.getAnimations(model.npcType)
            null -> emptyList()
        }
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
            val modelParam = addDisposable(
                uiStore.registerParameter(
                    PwToolType.Viewer,
                    path,
                    MODEL_PARAM,
                    onChange = { newValue ->
                        scope.launch {
                            setCurrentModel(
                                newValue?.let { ViewerModel.findBySlug(it) },
                            )
                        }
                    },
                ),
            )
            modelParams.add(modelParam)

            val sectionIdParam = addDisposable(
                uiStore.registerParameter(
                    PwToolType.Viewer,
                    path,
                    SECTION_ID_PARAM,
                    onChange = { newValue ->
                        scope.launch {
                            setCurrentSectionId(
                                newValue?.let { enumValueOfOrNull<SectionId>(it) }
                                    ?: SectionId.VALUES.random()
                            )
                        }
                    },
                ),
            )
            sectionIdParams.add(sectionIdParam)

            val bodyParam = addDisposable(
                uiStore.registerParameter(
                    PwToolType.Viewer,
                    path,
                    BODY_PARAM,
                    onChange = { newValue ->
                        scope.launch {
                            setCurrentBody((newValue?.toIntOrNull() ?: 1) - 1)
                        }
                    },
                ),
            )
            bodyParams.add(bodyParam)

            // Try to initialize settings from parameters.
            if (uiStore.currentTool.value == PwToolType.Viewer &&
                uiStore.path.value == path
            ) {
                modelParam.value?.let { paramValue ->
                    ViewerModel.findBySlug(paramValue)?.let {
                        _currentModel.value = it
                    }
                }

                sectionIdParam.value?.let { enumValueOfOrNull<SectionId>(it) }?.let {
                    _currentSectionId.value = it
                }

                val maxBody = (_currentModel.value as? ViewerModel.Character)
                    ?.characterClass?.bodyStyleCount ?: 1
                bodyParam.value?.toIntOrNull()?.let {
                    _currentBody.value = clamp(it, 1, maxBody) - 1
                }
            }
        }

        // Initialize parameters from settings.
        setCurrentModelValue(_currentModel.value)
        setCurrentSectionIdValue(_currentSectionId.value)
        setCurrentBodyValue(_currentBody.value)

        scope.launch {
            loadCurrentModel(clearAnimation = true)
        }
    }

    fun setCurrentNinjaGeometry(geometry: NinjaGeometry?) {
        mutate {
            if (_currentModel.value != null) {
                setCurrentModelValue(null)
                _currentTextures.clear()
            }

            _currentAnimation.value = null
            _currentNinjaMotion.value = null
            _currentNinjaGeometry.value = geometry
        }
    }

    fun setCurrentTextures(textures: List<XvrTexture>) {
        _currentTextures.replaceAll(textures)
    }

    suspend fun setCurrentModel(model: ViewerModel?) {
        val prevWasCharacter = _currentModel.value is ViewerModel.Character
        val newIsCharacter = model is ViewerModel.Character

        // Only preserve animation when switching between character classes.
        val clearAnimation = !(prevWasCharacter && newIsCharacter)

        setCurrentModelValue(model)

        if (model is ViewerModel.Character) {
            val char = model.characterClass

            if (_currentBody.value >= char.bodyStyleCount) {
                setCurrentBodyValue(char.bodyStyleCount - 1)
            }
        }

        loadCurrentModel(clearAnimation)
    }

    suspend fun setCurrentSectionId(sectionId: SectionId) {
        setCurrentSectionIdValue(sectionId)
        loadCharacterClassNinjaObject(clearAnimation = false)
    }

    suspend fun setCurrentBody(body: Int) {
        setCurrentBodyValue(body)
        loadCharacterClassNinjaObject(clearAnimation = false)
    }

    fun setCurrentNinjaMotion(njm: NjMotion) {
        mutate {
            _currentNinjaMotion.value = njm
            _animationPlaying.value = true
        }
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

    private suspend fun loadCurrentModel(clearAnimation: Boolean) {
        when (_currentModel.value) {
            is ViewerModel.Character -> loadCharacterClassNinjaObject(clearAnimation)
            is ViewerModel.Npc -> loadNpcNinjaObject(clearAnimation)
            null -> {
                mutate {
                    _currentNinjaGeometry.value = null
                    _currentTextures.clear()

                    if (clearAnimation) {
                        _currentAnimation.value = null
                        _currentNinjaMotion.value = null
                    }
                }
            }
        }
    }

    private suspend fun loadCharacterClassNinjaObject(clearAnimation: Boolean) {
        val char = currentCharacterClass.value
            ?: return

        try {
            val sectionId = currentSectionId.value
            val body = currentBody.value
            val ninjaObject = characterClassAssetLoader.loadNinjaObject(char)
            val textures = characterClassAssetLoader.loadXvrTextures(char, sectionId, body)

            mutate {
                if (clearAnimation) {
                    _currentAnimation.value = null
                    _currentNinjaMotion.value = null
                }

                _currentNinjaGeometry.value = NinjaGeometry.Object(ninjaObject)
                _currentTextures.replaceAll(textures)
            }
        } catch (e: Exception) {
            logger.error(e) { "Couldn't load Ninja model for $char." }

            mutate {
                _currentAnimation.value = null
                _currentNinjaMotion.value = null
                _currentNinjaGeometry.value = null
                _currentTextures.clear()
            }
        }
    }

    private suspend fun loadNpcNinjaObject(clearAnimation: Boolean) {
        val model = _currentModel.value as? ViewerModel.Npc ?: return
        val npcType = model.npcType

        try {
            val ninjaObject = npcAssetLoader.loadNinjaObject(npcType)
            val textures = npcAssetLoader.loadXvrTextures(npcType)

            mutate {
                if (clearAnimation) {
                    _currentAnimation.value = null
                    _currentNinjaMotion.value = null
                }

                _currentNinjaGeometry.value = NinjaGeometry.Object(ninjaObject)
                _currentTextures.replaceAll(textures)
            }
        } catch (e: Exception) {
            logger.error(e) { "Couldn't load Ninja model for ${npcType.uniqueName}." }

            mutate {
                _currentAnimation.value = null
                _currentNinjaMotion.value = null
                _currentNinjaGeometry.value = null
                _currentTextures.clear()
            }
        }
    }

    private suspend fun loadAnimation(animation: AnimationModel) {
        try {
            val ninjaMotion = animationAssetLoader.loadAnimation(animation.filePath)

            mutate {
                _currentNinjaMotion.value = ninjaMotion
                _animationPlaying.value = true
            }
        } catch (e: Exception) {
            logger.error(e) {
                "Couldn't load Ninja motion for ${animation.name} (path: ${animation.filePath})."
            }

            _currentNinjaMotion.value = null
        }
    }

    private fun setCurrentModelValue(model: ViewerModel?) {
        _currentModel.value = model

        for (param in modelParams) {
            param.set(model?.slug)
        }
    }

    private fun setCurrentSectionIdValue(sectionId: SectionId) {
        _currentSectionId.value = sectionId

        for (param in sectionIdParams) {
            param.set(sectionId.name)
        }
    }

    private fun setCurrentBodyValue(body: Int) {
        _currentBody.value = body
        val paramValue = (body + 1).toString()

        for (param in bodyParams) {
            param.set(paramValue)
        }
    }

    companion object {
        const val MODEL_PARAM = "model"
        const val BODY_PARAM = "body"
        const val SECTION_ID_PARAM = "section_id"
    }
}
