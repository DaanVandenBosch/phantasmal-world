package world.phantasmal.web.questEditor.controllers

import kotlinx.coroutines.await
import mu.KotlinLogging
import world.phantasmal.cell.*
import world.phantasmal.core.*
import world.phantasmal.core.externals.browser.FileSystemDirectoryHandle
import world.phantasmal.core.externals.browser.arrayBuffer
import world.phantasmal.psolib.Endianness
import world.phantasmal.psolib.Episode
import world.phantasmal.psolib.asm.dataFlowAnalysis.FloorMapping
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.compression.prs.prsCompress
import world.phantasmal.psolib.cursor.cursor
import world.phantasmal.psolib.fileFormats.quest.*
import world.phantasmal.web.questEditor.loading.FreeRoamAreaInfo
import world.phantasmal.web.questEditor.loading.extractRawEntityDataByFloor
import world.phantasmal.web.questEditor.loading.parseFreeRoamFilename
import world.phantasmal.cell.list.ListCell
import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.core.files.cursor
import world.phantasmal.web.core.files.writeBuffer
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.questEditor.models.AreaModel
import world.phantasmal.web.questEditor.models.AreaVariantModel
import world.phantasmal.web.questEditor.models.QuestModel
import world.phantasmal.web.questEditor.models.SectionModel
import world.phantasmal.web.questEditor.stores.AreaStore
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.web.questEditor.stores.QuestEditorUiStore
import world.phantasmal.web.questEditor.stores.convertQuestFromModel
import world.phantasmal.web.questEditor.stores.convertQuestToModel
import world.phantasmal.webui.UserAgentFeatures
import world.phantasmal.webui.controllers.Controller
import world.phantasmal.webui.files.*
import world.phantasmal.webui.files.showDirectoryPicker
import world.phantasmal.webui.obj

private val logger = KotlinLogging.logger {}

enum class SaveFormat {
    QST,
    BIN_DAT,
    FREE_ROAM,
}

class AreaAndLabel(
    val area: AreaModel,
    val label: String,
    val variant: AreaVariantModel? = null,
    /** Floor IDs associated with this entry, used to filter entities. */
    val floorIds: Set<Int>? = null,
)

class QuestEditorToolbarController(
    uiStore: UiStore,
    private val areaStore: AreaStore,
    private val questEditorStore: QuestEditorStore,
    private val questEditorUiStore: QuestEditorUiStore,
) : Controller() {
    private val _resultDialogVisible = mutableCell(false)
    private val _result = mutableCell<PwResult<*>?>(null)
    private val saving = mutableCell(false)
    private val _selectedAreaAndLabel = mutableCell<AreaAndLabel?>(null)
    /** True while [setCurrentArea] is updating the store, to avoid clearing the selection. */
    private var settingAreaFromDropdown = false
    private var gameDirHandle: FileSystemDirectoryHandle? = null

    // City map toggle (View menu)
    private val _showCityMap = mutableCell(false)
    val showCityMap: Cell<Boolean> = _showCityMap

    // Lobby menu visibility (hidden by default, toggled via Ctrl+L)
    private val _showLobbyMenu = mutableCell(false)
    val showLobbyMenu: Cell<Boolean> = _showLobbyMenu

    // Free roam variant state (delegated to FreeRoamController)
    val freeRoam = FreeRoamController()

    // We mainly disable saving while a save is underway for visual feedback that a save is
    // happening/has happened.
    private val savingEnabled = questEditorStore.currentQuest.isNotNull() and !saving
    private val _saveAsDialogVisible = mutableCell(false)
    private val fileHolder = mutableCell<FileHolder?>(null)
    private val _filename = mutableCell("")
    private val _version = mutableCell(Version.BB)
    private val _compressed = mutableCell(true)
    private val _saveFormat = mutableCell(SaveFormat.QST)

    val saveFormat: Cell<SaveFormat> = _saveFormat
    val compressedVisible: Cell<Boolean> = _saveFormat.map { it == SaveFormat.BIN_DAT }
    val freeRoamFormatAvailable: Boolean = UserAgentFeatures.directoryPickerApi
    val availableSaveFormats: List<SaveFormat> = buildList {
        add(SaveFormat.QST)
        add(SaveFormat.BIN_DAT)
        if (UserAgentFeatures.directoryPickerApi) add(SaveFormat.FREE_ROAM)
    }

    // Result

    val resultDialogVisible: Cell<Boolean> = _resultDialogVisible
    val result: Cell<PwResult<*>?> = _result

    val supportedFileTypes = listOf(
        FileType(
            description = "Quests and Free Roam Files",
            accept = mapOf("application/pw-quest" to setOf(".qst", ".bin", ".dat")),
        ),
    )

    // Saving

    val saveEnabled: Cell<Boolean> =
        savingEnabled and questEditorStore.canSaveChanges and UserAgentFeatures.fileSystemApi
    val saveTooltip: Cell<String> =
        if (UserAgentFeatures.fileSystemApi) {
            questEditorStore.canSaveChanges.map {
                (if (it) "Save changes" else "No changes to save") + " (Ctrl-S)"
            }
        } else {
            cell("This browser doesn't support saving changes to existing files")
        }
    val saveAsEnabled: Cell<Boolean> = savingEnabled
    val saveAsDialogVisible: Cell<Boolean> = _saveAsDialogVisible
    val showSaveAsDialogNameField: Boolean = !UserAgentFeatures.fileSystemApi
    val filename: Cell<String> = _filename
    val version: Cell<Version> = _version
    val compressed: Cell<Boolean> = _compressed

    // Undo

    val undoTooltip: Cell<String> = questEditorStore.firstUndo.map { command ->
        (command?.let { "Undo \"${command.description}\"" } ?: "Nothing to undo") + " (Ctrl-Z)"
    }

    val undoEnabled: Cell<Boolean> = questEditorStore.canUndo

    // Redo

    val redoTooltip: Cell<String> = questEditorStore.firstRedo.map { command ->
        (command?.let { "Redo \"${command.description}\"" } ?: "Nothing to redo") +
                " (Ctrl-Shift-Z)"
    }

    val redoEnabled: Cell<Boolean> = questEditorStore.canRedo

    // Areas

    /**
     * Builds the area dropdown list for the toolbar. Reactively updates when entities are
     * added/removed (entity counts in labels) or when the current quest changes.
     *
     * Both branches maintain the canonical episode area order (Pioneer II → Forest 1 → … etc.).
     *
     * Two branches depending on whether the quest uses floor mappings (bb_map_designate):
     *
     * **With floorMappings** (challenge mode, multi-floor quests like PW4):
     *   - One floor can map to an area+variant (e.g., floor 16 → Tower variant 1).
     *   - Multiple floors may share the same area but with different variants, so entries are
     *     deduplicated by (areaId, variantId). Entity counts are summed across all floors that
     *     belong to each variant.
     *   - Unmapped episode areas are interleaved in canonical order (not appended at the end)
     *     without variant suffix or entity count. All floors are always shown — do NOT filter
     *     out empty areas.
     *
     * **Without floorMappings** (regular quests):
     *   - All areas for the episode are listed. Areas with multiple variants expand into
     *     separate entries (e.g., "Cave 1 - Map 1", "Cave 1 - Map 2").
     */
    val areas: Cell<List<AreaAndLabel>> = questEditorStore.currentQuest.flatMap { quest ->
        quest?.let {
            map(quest.entitiesPerArea, quest.areaVariants) { entitiesPerArea, variants ->
                val result = mutableListOf<AreaAndLabel>()

                if (quest.floorMappings.isNotEmpty()) {
                    // Group mappings by areaId for lookup. Each FloorMapping is a distinct floor,
                    // even when multiple floors share the same (areaId, variantId) — e.g. two
                    // Seaside Area floors with different floorIds but the same map and variant.
                    val mappingsByAreaId = mutableMapOf<Int, MutableList<FloorMapping>>()
                    for (mapping in quest.floorMappings) {
                        mappingsByAreaId.getOrPut(mapping.areaId) { mutableListOf() }
                            .add(mapping)
                    }

                    // Count how many floors share each (areaId, variantId) to detect duplicates.
                    val variantFloorCount = quest.floorMappings
                        .groupBy { Pair(it.areaId, it.variantId) }
                        .mapValues { (_, mappings) -> mappings.size }

                    // Track sequential numbering for same-(areaId, variantId) floors.
                    val variantFloorIndex = mutableMapOf<Pair<Int, Int>, Int>()

                    // Iterate in episode area order so the list matches the canonical floor order.
                    for (episodeArea in areaStore.getAreasForEpisode(quest.episode)) {
                        val areaMappings = mappingsByAreaId[episodeArea.id]

                        if (areaMappings != null) {
                            // Mapped area: add one entry per floor with entity count.
                            for (mapping in areaMappings) {
                                val mapEp = mapping.mapEpisode ?: quest.episode
                                val area = areaStore.getArea(mapEp, mapping.areaId) ?: continue
                                val variant = areaStore.getVariant(mapEp, mapping.areaId, mapping.variantId)
                                    ?: continue

                                // Count entities for this specific floor only.
                                val floorId = mapping.floorId
                                val entityCount =
                                    quest.npcs.value.count { it.areaId == floorId } +
                                        quest.objects.value.count { it.areaId == floorId }

                                // Disambiguate when multiple floors share the same area+variant.
                                val key = Pair(mapping.areaId, mapping.variantId)
                                val floorIdx = (variantFloorIndex[key] ?: 0) + 1
                                variantFloorIndex[key] = floorIdx
                                val floorSuffix = if (variantFloorCount.getValue(key) > 1) {
                                    " #$floorIdx"
                                } else ""

                                val displayName = buildAreaDisplayName(area, variant, if (entityCount > 0) entityCount else null) + floorSuffix
                                result.add(AreaAndLabel(area, displayName, variant, setOf(floorId)))
                            }
                        } else {
                            // Unmapped area: show without variant suffix or entity count.
                            val displayName = buildAreaDisplayName(episodeArea, null, null)
                            result.add(AreaAndLabel(episodeArea, displayName))
                        }
                    }
                } else {
                    for (area in areaStore.getAreasForEpisode(quest.episode)) {
                        val entityCount = getEntityCountForArea(entitiesPerArea, area)
                        val areaVariants = getAreaVariants(area, variants)

                        if (areaVariants.isNotEmpty()) {
                            for (variant in areaVariants) {
                                val displayName = buildAreaDisplayName(area, variant, entityCount)
                                result.add(AreaAndLabel(area, displayName, variant))
                            }
                        } else {
                            val displayName = buildAreaDisplayName(area, null, entityCount)
                            result.add(AreaAndLabel(area, displayName))
                        }
                    }
                }

                result
            }
        } ?: cell(emptyList())
    }

    private fun getEntityCountForArea(entitiesPerArea: Map<Int, Int>, area: AreaModel): Int? {
        return entitiesPerArea[area.id]
    }

    private fun getAreaVariants(area: AreaModel, variants: List<AreaVariantModel>): List<AreaVariantModel> {
        return variants.filter { it.area.id == area.id }.sortedBy { it.id }
    }

    private fun buildAreaDisplayName(area: AreaModel, variant: AreaVariantModel?, entityCount: Int?): String {
        val baseName = variant?.name ?: area.name
        val mapSuffix = getMapVariantSuffix(area, variant?.id)
        val countSuffix = createCountSuffix(entityCount)

        return baseName + mapSuffix + countSuffix
    }

    private fun getMapVariantSuffix(area: AreaModel, variantId: Int?): String {
        return when {
            area.id <= 0 -> ""
            area.bossArea -> ""
            variantId != null -> " - Map ${variantId + 1}"  // Show Map X if variant is specified
            else -> ""  // No Map suffix for areas without variants
        }
    }

    private fun createCountSuffix(entityCount: Int?): String {
        return entityCount?.let { " ($it)" } ?: ""
    }

    val currentArea: Cell<AreaAndLabel?> = areas.flatMap { areas ->
        map(_selectedAreaAndLabel, questEditorStore.currentArea, questEditorStore.currentFloorIds) { selectedAreaAndLabel, storeCurrentArea, storeFloorIds ->
            // If there's an explicitly selected AreaAndLabel, use it
            selectedAreaAndLabel ?:
            // Match by floorIds first (most precise), then by area, then fallback
            if (storeFloorIds != null) {
                areas.find { it.floorIds == storeFloorIds }
            } else if (storeCurrentArea != null) {
                areas.find { it.area.id == storeCurrentArea.id }
            } else null ?:
            // Final fallback to first area
            areas.firstOrNull()
        }
    }

    val areaSelectEnabled: Cell<Boolean> = questEditorStore.currentQuest.isNotNull()

    // Settings

    val showCollisionGeometry: Cell<Boolean> = questEditorUiStore.showCollisionGeometry
    val showSectionIds: Cell<Boolean> = questEditorUiStore.showSectionIds
    val showDoorIds: Cell<Boolean> = questEditorUiStore.showDoorIds
    val spawnMonstersOnGround: Cell<Boolean> = questEditorUiStore.spawnMonstersOnGround
    val showOriginPoint: Cell<Boolean> = questEditorUiStore.showOriginPoint
    val showQuestParticles: Cell<Boolean> = questEditorUiStore.showQuestParticles

    // Free roam variant controls (delegated)

    val freeRoamV1Options: ListCell<Int> get() = freeRoam.freeRoamV1Options
    val freeRoamV2Options: ListCell<Int> get() = freeRoam.freeRoamV2Options
    val showFreeRoamV1: Cell<Boolean> get() = freeRoam.showFreeRoamV1
    val showFreeRoamV2: Cell<Boolean> get() = freeRoam.showFreeRoamV2
    val showFreeRoamOnOff: Cell<Boolean> get() = freeRoam.showFreeRoamOnOff
    val showFreeRoamUltimate: Cell<Boolean> get() = freeRoam.showFreeRoamUltimate
    val freeRoamV1: Cell<Int?> get() = freeRoam.freeRoamV1
    val freeRoamV2: Cell<Int?> get() = freeRoam.freeRoamV2
    val freeRoamOffline: Cell<Boolean> get() = freeRoam.freeRoamOffline
    val freeRoamUltimate: Cell<Boolean> get() = freeRoam.freeRoamUltimate

    // Go to Section functionality
    // Use the store's selectedSection directly to ensure sync when clicking events
    val selectedSection: Cell<SectionModel?> = questEditorStore.selectedSection
    val gotoSectionEnabled: Cell<Boolean> = questEditorStore.currentAreaVariant.isNotNull()

    // Go to World Position. Three small numeric inputs (X/Y/Z); each commits to the goto on
    // change. Pasting a tuple (e.g. "(-10230, -2, -10)") into any field smart-distributes to
    // all three — see [smartPasteGotoPosition].
    val gotoWorldPositionEnabled: Cell<Boolean> = questEditorStore.currentQuest.isNotNull()
    private val _gotoWorldX = mutableCell(0.0)
    private val _gotoWorldY = mutableCell(0.0)
    private val _gotoWorldZ = mutableCell(0.0)
    val gotoWorldX: Cell<Double> = _gotoWorldX
    val gotoWorldY: Cell<Double> = _gotoWorldY
    val gotoWorldZ: Cell<Double> = _gotoWorldZ

    private val gotoSectionFilter: MutableCell<(SectionModel) -> Boolean> =
        mutableCell { true }

    val filteredSections: Cell<List<SectionModel>> =
        map(questEditorStore.currentAreaSections, gotoSectionFilter) { sections, filter ->
            sections.filter(filter)
        }

    init {
        // Clear explicit area selection when the store's area changes externally (e.g. via
        // goToEvent or setSelectedEntity), so the toolbar shows the correct current area.
        observe(questEditorStore.currentArea) {
            if (!settingAreaFromDropdown) {
                _selectedAreaAndLabel.value = null
            }
        }

        addDisposables(
            freeRoam,

            uiStore.onGlobalKeyDown(PwToolType.QuestEditor, "Ctrl-O") {
                openFiles(showOpenFilePicker(supportedFileTypes, multiple = true))
            },

            uiStore.onGlobalKeyDown(PwToolType.QuestEditor, "Ctrl-S") {
                save()
            },

            uiStore.onGlobalKeyDown(PwToolType.QuestEditor, "Ctrl-Shift-S") {
                saveAs()
            },

            uiStore.onGlobalKeyDown(PwToolType.QuestEditor, "Ctrl-Z") {
                undo()
            },

            uiStore.onGlobalKeyDown(PwToolType.QuestEditor, "Ctrl-Shift-Z") {
                redo()
            },

            uiStore.onGlobalKeyDown(PwToolType.QuestEditor, "Ctrl-Y") {
                redo()
            },

            uiStore.onGlobalKeyDown(PwToolType.QuestEditor, "Ctrl-L") {
                _showLobbyMenu.value = !_showLobbyMenu.value
            },
        )
    }

    /** Episode of the currently loaded quest, defaults to Episode I. */
    val currentEpisode: Episode
        get() = questEditorStore.currentQuest.value?.episode ?: Episode.I

    suspend fun createNewQuest(episode: Episode) {
        freeRoam.clearFreeRoamState()
        val quest = if (_showCityMap.value) {
            questEditorStore.getCityQuest(episode)
        } else {
            questEditorStore.getDefaultQuest(episode)
        }
        setCurrentQuest(fileHolder = null, Version.BB, quest)
    }

    suspend fun loadCityQuest(episode: Episode) {
        freeRoam.clearFreeRoamState()
        _showCityMap.value = true
        setCurrentQuest(fileHolder = null, Version.BB, questEditorStore.getCityQuest(episode))
    }

    suspend fun loadLobbyQuest(variant: Int) {
        freeRoam.clearFreeRoamState()
        _showCityMap.value = false
        setCurrentQuest(fileHolder = null, Version.BB, questEditorStore.getLobbyQuest(variant))
    }

    suspend fun setShowCityMap(show: Boolean) {
        if (show) {
            loadCityQuest(currentEpisode)
        } else {
            createNewQuest(currentEpisode)
        }
    }

    suspend fun openFiles(newFiles: List<FileHandle>?) {
        try {
            if (newFiles.isNullOrEmpty()) return

            when (val strategy = resolveLoadStrategy(newFiles)) {
                is QuestLoadStrategy.Qst -> loadQst(strategy)
                is QuestLoadStrategy.BinDat -> loadBinDat(strategy)
                is QuestLoadStrategy.FreeRoam -> loadFreeRoam(strategy)
                null -> setResult(
                    Failure(
                        listOf(
                            Problem(
                                Severity.Error,
                                "Please select a .qst file, a .bin file, a .bin + .dat pair, or a free roam .dat file.",
                            )
                        )
                    )
                )
            }
        } catch (e: Throwable) {
            setResult(
                PwResult.build<Nothing>(logger)
                    .addProblem(Severity.Error, "Couldn't parse file.", cause = e)
                    .failure()
            )
        }
    }

    private fun resolveLoadStrategy(files: List<FileHandle>): QuestLoadStrategy? {
        val qstFile = files.find { it.extension().equals("qst", ignoreCase = true) }
        if (qstFile != null) return QuestLoadStrategy.Qst(qstFile)

        val binFile = files.find { it.extension().equals("bin", ignoreCase = true) }
        val datFile = files.find { it.extension().equals("dat", ignoreCase = true) }

        // Regular bin+dat pair takes priority over free roam detection.
        if (binFile != null && datFile != null) {
            return QuestLoadStrategy.BinDat(binFile, datFile)
        }

        // Check if a single file matches a free roam pattern (bin or dat).
        val freeRoamFile = binFile ?: datFile
        if (freeRoamFile != null) {
            val freeRoamInfo = parseFreeRoamFilename(freeRoamFile.name)
            if (freeRoamInfo != null) return QuestLoadStrategy.FreeRoam(freeRoamInfo)
        }

        return null
    }

    private suspend fun loadQst(strategy: QuestLoadStrategy.Qst) {
        freeRoam.clearFreeRoamState()
        val parseResult = parseQstToQuest(strategy.file.cursor(Endianness.Little))
        setResult(parseResult)

        if (parseResult is Success) {
            setCurrentQuest(
                FileHolder.Qst(strategy.file),
                parseResult.value.version,
                parseResult.value.quest,
            )
        }
    }

    private suspend fun loadBinDat(strategy: QuestLoadStrategy.BinDat) {
        freeRoam.clearFreeRoamState()
        val shiftJis = isJapaneseFilename(strategy.binFile.name)
        val parseResult = parseBinDatToQuestAutoDetect(
            strategy.binFile.cursor(Endianness.Little),
            strategy.datFile.cursor(Endianness.Little),
            shiftJis = shiftJis,
        )
        setResult(parseResult)

        if (parseResult is Success) {
            val version = when (parseResult.value.quest.binFormat) {
                BinFormat.DC_GC -> Version.GC  // Can't distinguish DC from GC; default to GC
                BinFormat.PC -> Version.PC
                BinFormat.BB -> Version.BB
            }
            setCurrentQuest(
                FileHolder.BinDat(strategy.binFile, strategy.datFile, parseResult.value.compressed),
                version,
                parseResult.value.quest,
            )
        }
    }

    private suspend fun loadFreeRoam(strategy: QuestLoadStrategy.FreeRoam) {
        if (!UserAgentFeatures.directoryPickerApi) {
            setResult(
                PwResult.build<Nothing>(logger)
                    .addProblem(
                        Severity.Error,
                        "Loading free roam maps requires a browser that supports the File System Access API (Chrome or Edge).",
                    )
                    .failure()
            )
            return
        }

        var dirHandle = gameDirHandle

        if (dirHandle == null) {
            dirHandle = showDirectoryPicker() ?: return
            gameDirHandle = dirHandle
        }

        val info = strategy.info
        val result = questEditorStore.getFreeRoamQuest(dirHandle, info)

        freeRoam.setupFreeRoamState(
            episode = info.episode,
            floorRange = info.floorRange,
            gameDirHandle = dirHandle,
            binPrefix = info.binPrefix,
            isCity = info.isCity,
            initialOffline = info.offline,
            initialUltimate = info.ultimate,
            tokenBase = info.tokenBase,
        )

        setCurrentQuest(
            FileHolder.FreeRoamDir(dirHandle, result.binName, result.datFilesByFloor),
            Version.BB,
            result.questModel,
        )
    }

    suspend fun save() {
        if (!saveEnabled.value) return

        try {
            saving.value = true

            val quest = questEditorStore.currentQuest.value ?: return
            val headerFilename = filename.value.trim()

            when (val holder = fileHolder.value) {
                is FileHolder.Qst -> {
                    if (holder.file is FileHandle.System) {
                        val buffer = writeQuestToQst(
                            convertQuestFromModel(quest),
                            headerFilename,
                            version.value,
                            online = true,
                            compressed = compressed.value,
                        )

                        holder.file.writeBuffer(buffer)

                        questEditorStore.questSaved()
                        return
                    }
                }

                is FileHolder.BinDat -> {
                    if (holder.binFile is FileHandle.System &&
                        holder.datFile is FileHandle.System
                    ) {
                        var (bin, dat) = writeQuestToBinDat(
                            convertQuestFromModel(quest),
                            version.value,
                        )

                        if (compressed.value) {
                            bin = prsCompress(bin.cursor()).buffer()
                            dat = prsCompress(dat.cursor()).buffer()
                        }

                        holder.binFile.writeBuffer(bin)
                        holder.datFile.writeBuffer(dat)

                        questEditorStore.questSaved()
                        return
                    }
                }

                is FileHolder.FreeRoamDir -> {
                    saveToFreeRoamDir(holder, quest)
                    return
                }

                else -> {}
            }

            // When there's no existing file that can be saved, default to "Save as...".
            _saveAsDialogVisible.value = true
        } catch (e: Throwable) {
            setResult(
                PwResult.build<Nothing>(logger)
                    .addProblem(Severity.Error, "Couldn't save file.", cause = e)
                    .failure()
            )
        } finally {
            saving.value = false
        }
    }

    fun saveAs() {
        if (saveAsEnabled.value) {
            _saveAsDialogVisible.value = true
        }
    }

    fun setFilename(filename: String) {
        _filename.value = filename
    }

    fun setVersion(version: Version) {
        _version.value = version
    }

    fun setCompressed(compressed: Boolean) {
        _compressed.value = compressed
    }

    fun setSaveFormat(format: SaveFormat) {
        _saveFormat.value = format
    }

    suspend fun saveAsDialogSave() {
        if (!saveAsEnabled.value) return

        val quest = questEditorStore.currentQuest.value ?: return

        try {
            saving.value = true

            when (_saveFormat.value) {
                SaveFormat.QST -> saveAsQst(quest)
                SaveFormat.BIN_DAT -> saveAsBinDat(quest)
                SaveFormat.FREE_ROAM -> saveAsFreeRoam(quest)
            }
        } catch (e: Throwable) {
            setResult(
                PwResult.build<Nothing>(logger)
                    .addProblem(Severity.Error, "Couldn't save file.", cause = e)
                    .failure()
            )
        } finally {
            dismissSaveAsDialog()
            saving.value = false
        }
    }

    private suspend fun saveAsQst(quest: QuestModel) {
        val headerFilename = filename.value.trim()
        val filename =
            if (headerFilename.endsWith(".qst")) headerFilename
            else "$headerFilename.qst"

        val buffer = writeQuestToQst(
            convertQuestFromModel(quest),
            headerFilename,
            version.value,
            online = true,
            compressed = true, // QST is always PRS compressed.
        )

        if (UserAgentFeatures.fileSystemApi) {
            val fileHandle = showSaveFilePicker(
                listOf(
                    FileType("Quest file", mapOf("application/pw-quest" to setOf(".qst")))
                )
            )

            if (fileHandle != null) {
                fileHandle.writableStream().use { it.write(buffer.arrayBuffer).await() }

                setFileHolder(FileHolder.Qst(fileHandle))
                questEditorStore.questSaved()
            }
        } else {
            val fileHandle = downloadFile(buffer.arrayBuffer, filename)
            setFileHolder(FileHolder.Qst(fileHandle))
            questEditorStore.questSaved()
        }
    }

    private suspend fun saveAsBinDat(quest: QuestModel) {
        val headerFilename = filename.value.trim()
        // Strip .qst extension if present to derive a base name.
        val baseName = headerFilename
            .removeSuffix(".qst")
            .removeSuffix(".bin")
            .removeSuffix(".dat")

        val binFilename = "$baseName.bin"
        val datFilename = "$baseName.dat"

        var (bin, dat) = writeQuestToBinDat(
            convertQuestFromModel(quest),
            version.value,
        )

        if (compressed.value) {
            bin = prsCompress(bin.cursor()).buffer()
            dat = prsCompress(dat.cursor()).buffer()
        }

        if (UserAgentFeatures.fileSystemApi) {
            val binHandle = showSaveFilePicker(
                listOf(
                    FileType("BIN file", mapOf("application/octet-stream" to setOf(".bin")))
                )
            )

            if (binHandle != null) {
                binHandle.writableStream().use { it.write(bin.arrayBuffer).await() }

                val datHandle = showSaveFilePicker(
                    listOf(
                        FileType("DAT file", mapOf("application/octet-stream" to setOf(".dat")))
                    )
                )

                if (datHandle != null) {
                    datHandle.writableStream().use { it.write(dat.arrayBuffer).await() }

                    setFileHolder(FileHolder.BinDat(binHandle, datHandle, compressed.value))
                    questEditorStore.questSaved()
                }
            }
        } else {
            val binHandle = downloadFile(bin.arrayBuffer, binFilename)
            val datHandle = downloadFile(dat.arrayBuffer, datFilename)
            setFileHolder(FileHolder.BinDat(binHandle, datHandle, compressed.value))
            questEditorStore.questSaved()
        }
    }

    private suspend fun saveAsFreeRoam(quest: QuestModel) {
        if (!UserAgentFeatures.directoryPickerApi) {
            setResult(
                PwResult.build<Nothing>(logger)
                    .addProblem(
                        Severity.Error,
                        "Saving as Free Roam requires a browser that supports the File System Access API (Chrome or Edge).",
                    )
                    .failure()
            )
            return
        }

        // If the quest was already loaded from a free roam directory, save back to it.
        val holder = fileHolder.value
        if (holder is FileHolder.FreeRoamDir) {
            saveToFreeRoamDir(holder, quest)
            return
        }

        // Otherwise, ask the user for a directory.
        val dirHandle = showDirectoryPicker() ?: return
        gameDirHandle = dirHandle

        // For Save As to a new directory, we need the current holder's dat file info.
        // Since we don't have free roam file structure, show an informative error.
        setResult(
            PwResult.build<Nothing>(logger)
                .addProblem(
                    Severity.Error,
                    "Save As Free Roam is only supported for quests originally loaded from a free roam directory. Use Save (Ctrl+S) instead.",
                )
                .failure()
        )
    }

    fun dismissSaveAsDialog() {
        _saveAsDialogVisible.value = false
    }

    fun dismissResultDialog() {
        _resultDialogVisible.value = false
    }

    fun undo() {
        questEditorStore.undo()
    }

    fun redo() {
        questEditorStore.redo()
    }

    fun setCurrentArea(areaAndLabel: AreaAndLabel) {
        // Clear selected section when area changes to avoid showing wrong sections
        clearSelectedSection()
        // Store the selected AreaAndLabel to preserve variant information
        _selectedAreaAndLabel.value = areaAndLabel
        settingAreaFromDropdown = true
        try {
            questEditorStore.setCurrentArea(areaAndLabel.area)
            questEditorStore.setCurrentAreaVariant(areaAndLabel.variant)
            questEditorStore.setCurrentFloorIds(areaAndLabel.floorIds)
        } finally {
            settingAreaFromDropdown = false
        }
    }

    fun setShowCollisionGeometry(show: Boolean) {
        questEditorUiStore.setShowCollisionGeometry(show)
    }

    fun setShowSectionIds(show: Boolean) {
        questEditorUiStore.setShowSectionIds(show)
    }

    fun setShowDoorIds(show: Boolean) {
        questEditorUiStore.setShowDoorIds(show)
    }

    fun setSpawnMonstersOnGround(spawn: Boolean) {
        questEditorUiStore.setSpawnMonstersOnGround(spawn)
    }

    fun setShowOriginPoint(show: Boolean) {
        questEditorUiStore.setShowOriginPoint(show)
    }

    fun setShowQuestParticles(show: Boolean) {
        questEditorUiStore.setShowQuestParticles(show)
    }

    fun setSelectedSection(section: SectionModel?) {
        // Update the quest editor store (which is what selectedSection references)
        questEditorStore.setSelectedSection(section)
    }

    fun goToSelectedSection() {
        selectedSection.value?.let { section ->
            questEditorStore.goToSection(section.id)
        }
    }

    fun filterSections(text: String) {
        val trimmed = text.trim()
        gotoSectionFilter.value =
            if (trimmed.isEmpty()) {
                { true }
            } else {
                { it.id.toString().contains(trimmed) }
            }
    }

    fun selectSection(section: SectionModel) {
        ensureSectionsLoaded()
        setSelectedSection(section)
        questEditorStore.goToSection(section.id)
    }

    fun setGotoWorldX(x: Double) {
        _gotoWorldX.value = x
        commitGotoWorldPosition()
    }

    fun setGotoWorldY(y: Double) {
        _gotoWorldY.value = y
        commitGotoWorldPosition()
    }

    fun setGotoWorldZ(z: Double) {
        _gotoWorldZ.value = z
        commitGotoWorldPosition()
    }

    private fun commitGotoWorldPosition() {
        questEditorStore.goToWorldPosition(
            _gotoWorldX.value,
            _gotoWorldY.value,
            _gotoWorldZ.value,
        )
    }

    /**
     * Paste interceptor for the goto X/Y/Z inputs. When the clipboard contains 2+ numbers,
     * distribute them to X/Y/Z (missing trailing values default to 0), commit a goto, and
     * suppress the browser's default single-value paste. When it's a single number we let the
     * default paste handle it normally.
     *
     * Returns true when the paste was consumed.
     */
    fun smartPasteGotoPosition(text: String): Boolean {
        val parts = NUMBER_REGEX.findAll(text).map { it.value }.take(3).toList()
        if (parts.size < 2) return false
        val x = parts.getOrNull(0)?.toDoubleOrNull() ?: 0.0
        val y = parts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
        val z = parts.getOrNull(2)?.toDoubleOrNull() ?: 0.0
        _gotoWorldX.value = x
        _gotoWorldY.value = y
        _gotoWorldZ.value = z
        questEditorStore.goToWorldPosition(x, y, z)
        return true
    }

    /**
     * Trigger section loading when user interacts with the section dropdown
     */
    fun ensureSectionsLoaded() {
        val quest = questEditorStore.currentQuest.value
        val areaVariant = questEditorStore.currentAreaVariant.value

        if (areaVariant != null) {
            // Use quest episode if available, otherwise default to Episode I
            val episode = quest?.episode ?: Episode.I
            // Check if sections are already loaded
            val loadedSections = areaStore.getLoadedSections(episode, areaVariant)
            if (loadedSections == null) {
                // We'll trigger this from the store instead where scope is available
                questEditorStore.requestSectionLoading(episode, areaVariant)
            }
        }
    }

    /**
     * Clear selected section when area changes
     */
    fun clearSelectedSection() {
        // Clear the quest editor store selection (which selectedSection references)
        questEditorStore.setSelectedSection(null)
    }

    // Free roam variant change methods (delegated to freeRoam controller)

    suspend fun setFreeRoamV1(v1: Int) {
        freeRoam.setFreeRoamV1(v1, ::handleFreeRoamReload)
    }

    suspend fun setFreeRoamV2(v2: Int) {
        freeRoam.setFreeRoamV2(v2, ::handleFreeRoamReload)
    }

    suspend fun setFreeRoamOffline(offline: Boolean) {
        freeRoam.setFreeRoamOffline(offline, ::handleFreeRoamReload)
    }

    suspend fun setFreeRoamUltimate(ultimate: Boolean) {
        freeRoam.setFreeRoamUltimate(ultimate, ::handleFreeRoamReload)
    }

    private suspend fun handleFreeRoamReload(params: FreeRoamReloadParams) {
        // Preserve current area ID and variant ID across variant reload.
        val previousAreaId = currentArea.value?.area?.id
        val previousVariantId = currentArea.value?.variant?.id

        try {
            val result = questEditorStore.getFreeRoamQuest(
                params.gameDirHandle, params.info, params.v1, params.v2,
            )
            setCurrentQuest(
                FileHolder.FreeRoamDir(params.gameDirHandle, result.binName, result.datFilesByFloor),
                Version.BB,
                result.questModel,
            )

            // Restore area selection by setting the store's area/variant.
            // Don't set _selectedAreaAndLabel — let currentArea's fallback logic
            // find the matching AreaAndLabel from the current areas list, so it
            // stays in sync when areas is recomputed.
            if (previousAreaId != null) {
                // Match by both area ID and variant ID to correctly restore multi-variant areas
                // (e.g., challenge quests where the same area ID has multiple variants).
                val match = areas.value.find {
                    it.area.id == previousAreaId &&
                        (previousVariantId == null || it.variant?.id == previousVariantId)
                } ?: areas.value.find { it.area.id == previousAreaId }
                if (match != null) {
                    questEditorStore.setCurrentArea(match.area)
                    questEditorStore.setCurrentAreaVariant(match.variant)
                }
            }
        } catch (e: Throwable) {
            setResult(
                PwResult.build<Nothing>(logger)
                    .addProblem(Severity.Error, "Couldn't reload free roam area.", cause = e)
                    .failure()
            )
        }
    }

    private fun setFileHolder(fileHolder: FileHolder?) {
        // Default save format based on source format.
        setSaveFormat(when (fileHolder) {
            is FileHolder.Qst -> SaveFormat.QST
            is FileHolder.BinDat -> SaveFormat.BIN_DAT
            is FileHolder.FreeRoamDir -> SaveFormat.FREE_ROAM
            null -> SaveFormat.QST
        })

        // Default compression based on source format.
        setCompressed(when (fileHolder) {
            is FileHolder.Qst -> true
            is FileHolder.BinDat -> fileHolder.compressed
            is FileHolder.FreeRoamDir -> false
            null -> true
        })

        setFilename(
            when (fileHolder) {
                is FileHolder.Qst -> fileHolder.file.basename() ?: fileHolder.file.name

                is FileHolder.BinDat ->
                    fileHolder.binFile.basename()
                        ?: fileHolder.datFile.basename()
                        ?: fileHolder.binFile.name

                is FileHolder.FreeRoamDir ->
                    fileHolder.binName
                        ?: fileHolder.datFilesByFloor.values.firstOrNull()?.first
                        ?: "Free Roam"

                null -> ""
            }
        )
        this.fileHolder.value = fileHolder
    }

    private suspend fun setCurrentQuest(
        fileHolder: FileHolder?,
        version: Version,
        quest: QuestModel,
    ) {
        setFileHolder(fileHolder)
        setVersion(version)
        // Reset area selection when loading a new quest
        _selectedAreaAndLabel.value = null
        // Note: No need to clear selectedSection here since setCurrentQuest() in the store already does this
        questEditorStore.setCurrentQuest(quest)

        // Don't override the area/variant - let QuestEditorStore handle the correct initialization
        // The store already correctly sets the area based on floor 0 mapping or appropriate fallback
    }

    private suspend fun setCurrentQuest(
        fileHolder: FileHolder?,
        version: Version,
        quest: Quest,
    ) {
        setCurrentQuest(fileHolder, version, convertQuestToModel(quest, areaStore::getVariant))
    }

    private fun setResult(result: PwResult<*>) {
        _result.value = result

        if (result.problems.isNotEmpty()) {
            _resultDialogVisible.value = true
        }
    }

    private suspend fun saveToFreeRoamDir(holder: FileHolder.FreeRoamDir, quest: QuestModel) {
        val convertedQuest = convertQuestFromModel(quest)
        val dataDir = try {
            holder.dirHandle.getDirectoryHandle("data").await()
        } catch (_: Throwable) {
            holder.dirHandle
        }

        // 1. Backup original files.
        val backupDir = holder.dirHandle.getDirectoryHandle("backup", obj { create = true }).await()

        val allDatNames = holder.datFilesByFloor.values.flatMap { (obj, npc, evt) -> listOf(obj, npc, evt) }
        val allNames = listOfNotNull(holder.binName) + allDatNames
        for (name in allNames) {
            try {
                val originalData = dataDir.getFileHandle(name).await()
                    .getFile().await().arrayBuffer().await()

                val backupHandle = backupDir.getFileHandle(name, obj { create = true }).await()
                val writable = backupHandle.createWritable().await()
                writable.write(originalData).await()
                writable.close().await()
            } catch (e: Throwable) {
                // File might not exist (e.g., empty floor) — that's expected.
                // Log unexpected errors so the user can investigate if backup fails unexpectedly.
                if (e.message?.contains("not found", ignoreCase = true) == false &&
                    e.message?.contains("NotFoundError", ignoreCase = true) == false
                ) {
                    logger.warn(e) { "Failed to back up file $name before saving." }
                }
            }
        }

        // 2. Write modified bin (uncompressed) if we have a bin file.
        if (holder.binName != null) {
            val (bin, _) = writeQuestToBinDat(convertedQuest, _version.value)

            val binHandle = dataDir.getFileHandle(holder.binName, obj { create = true }).await()
            val binWritable = binHandle.createWritable().await()
            binWritable.write(bin.arrayBuffer).await()
            binWritable.close().await()
        }

        // 3. Write modified dat files (split by floor).
        val entityDataByFloor = extractRawEntityDataByFloor(convertedQuest)

        for ((floorId, datNames) in holder.datFilesByFloor) {
            val (objDatName, npcDatName, evtName) = datNames

            val entityData = entityDataByFloor[floorId]
            if (entityData != null) {
                val (objBuf, npcBuf) = entityData

                val objHandle = dataDir.getFileHandle(objDatName, obj { create = true }).await()
                val objWritable = objHandle.createWritable().await()
                objWritable.write(objBuf.arrayBuffer).await()
                objWritable.close().await()

                val npcHandle = dataDir.getFileHandle(npcDatName, obj { create = true }).await()
                val npcWritable = npcHandle.createWritable().await()
                npcWritable.write(npcBuf.arrayBuffer).await()
                npcWritable.close().await()
            }

            // Always write the evt file — if null (no events on this floor), write an empty
            // buffer so the old file is cleared and events aren't read back on next load.
            val evtBuf = writeEventDataForFloor(convertedQuest.events, floorId)
                ?: Buffer.withSize(0, Endianness.Little)
            val evtHandle = dataDir.getFileHandle(evtName, obj { create = true }).await()
            val evtWritable = evtHandle.createWritable().await()
            evtWritable.write(evtBuf.arrayBuffer).await()
            evtWritable.close().await()
        }

        questEditorStore.questSaved()
    }



    private sealed class QuestLoadStrategy {
        class Qst(val file: FileHandle) : QuestLoadStrategy()
        class BinDat(val binFile: FileHandle, val datFile: FileHandle) : QuestLoadStrategy()
        class FreeRoam(val info: FreeRoamAreaInfo) : QuestLoadStrategy()
    }

    private sealed class FileHolder {
        class Qst(val file: FileHandle) : FileHolder()
        class BinDat(val binFile: FileHandle, val datFile: FileHandle, val compressed: Boolean) : FileHolder()
        class FreeRoamDir(
            val dirHandle: FileSystemDirectoryHandle,
            val binName: String?,
            /** Map of floorId → (objDatName, npcDatName, evtName) for saving back. */
            val datFilesByFloor: Map<Int, Triple<String, String, String>>,
        ) : FileHolder()
    }

    companion object {
        /**
         * Detects Japanese quest files by filename suffix (e.g., `quest01_j.bin`).
         */
        fun isJapaneseFilename(filename: String): Boolean {
            val base = filename.substringBeforeLast('.')
            return base.endsWith("_j")
        }

        /** Matches signed integer / decimal components used by the goto-position parser. */
        private val NUMBER_REGEX = Regex("""-?\d+(?:\.\d+)?""")
    }
}
