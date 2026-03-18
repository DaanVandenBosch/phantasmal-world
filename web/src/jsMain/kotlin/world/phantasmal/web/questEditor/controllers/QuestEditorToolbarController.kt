package world.phantasmal.web.questEditor.controllers

import kotlinx.coroutines.await
import mu.KotlinLogging
import world.phantasmal.cell.*
import world.phantasmal.core.*
import world.phantasmal.core.externals.browser.FileSystemDirectoryHandle
import world.phantasmal.core.externals.browser.arrayBuffer
import world.phantasmal.psolib.Endianness
import world.phantasmal.psolib.Episode
import world.phantasmal.psolib.compression.prs.prsCompress
import world.phantasmal.psolib.cursor.cursor
import world.phantasmal.psolib.fileFormats.quest.*
import world.phantasmal.web.questEditor.loading.FreeRoamAreaInfo
import world.phantasmal.web.questEditor.loading.extractRawEntityDataByFloor
import world.phantasmal.web.questEditor.loading.parseFreeRoamFilename
import world.phantasmal.cell.list.ListCell
import world.phantasmal.cell.list.mutableListCell
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
import world.phantasmal.web.questEditor.stores.convertQuestFromModel
import world.phantasmal.web.questEditor.stores.convertQuestToModel
import world.phantasmal.webui.UserAgentFeatures
import world.phantasmal.webui.controllers.Controller
import world.phantasmal.webui.files.*
import world.phantasmal.webui.files.showDirectoryPicker
import world.phantasmal.webui.obj

private val logger = KotlinLogging.logger {}

class AreaAndLabel(val area: AreaModel, val label: String, val variant: AreaVariantModel? = null)

class QuestEditorToolbarController(
    uiStore: UiStore,
    private val areaStore: AreaStore,
    private val questEditorStore: QuestEditorStore,
) : Controller() {
    private val _resultDialogVisible = mutableCell(false)
    private val _result = mutableCell<PwResult<*>?>(null)
    private val saving = mutableCell(false)
    private val _selectedAreaAndLabel = mutableCell<AreaAndLabel?>(null)
    private var gameDirHandle: FileSystemDirectoryHandle? = null

    // Free roam variant state
    private val _freeRoamState = mutableCell<FreeRoamState?>(null)
    private val _freeRoamV1 = mutableCell<Int?>(null)
    private val _freeRoamV2 = mutableCell<Int?>(null)
    private val _freeRoamOffline = mutableCell(false)
    private val _freeRoamUltimate = mutableCell(false)

    // We mainly disable saving while a save is underway for visual feedback that a save is
    // happening/has happened.
    private val savingEnabled = questEditorStore.currentQuest.isNotNull() and !saving
    private val _saveAsDialogVisible = mutableCell(false)
    private val fileHolder = mutableCell<FileHolder?>(null)
    private val _filename = mutableCell("")
    private val _version = mutableCell(Version.BB)
    private val _compressed = mutableCell(true)

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

    // Ensure the areas list is updated when entities are added or removed (the count in the label
    // should update).
    val areas: Cell<List<AreaAndLabel>> = questEditorStore.currentQuest.flatMap { quest ->
        quest?.let {
            map(quest.entitiesPerArea, quest.areaVariants) { entitiesPerArea, variants ->
                val result = mutableListOf<AreaAndLabel>()

                if (quest.floorMappings.isNotEmpty()) {
                    // Group floor mappings by area ID for lookup
                    val floorMappingsByArea = quest.floorMappings.groupBy { it.areaId }
                    // Group floor IDs by (areaId, variantId) for per-variant entity counting
                    val variantFloorIds = quest.floorMappings
                        .groupBy { Pair(it.areaId, it.variantId) }
                        .mapValues { (_, mappings) -> mappings.map { it.floorId }.toSet() }


                    for (area in areaStore.getAreasForEpisode(quest.episode)) {
                        val areaFloorMappings = floorMappingsByArea[area.id]

                        if (areaFloorMappings != null) {
                            // Deduplicate by variant ID
                            val seenVariants = mutableSetOf<Int>()
                            for (mapping in areaFloorMappings) {
                                if (!seenVariants.add(mapping.variantId)) continue
                                val variant = areaStore.getVariant(quest.episode, mapping.areaId, mapping.variantId)

                                if (variant != null) {
                                    // Count entities across all floor IDs for this area+variant
                                    val floorIds = variantFloorIds[Pair(area.id, mapping.variantId)]
                                    val entityCount = if (floorIds != null) {
                                        quest.npcs.value.count { it.areaId in floorIds } +
                                            quest.objects.value.count { it.areaId in floorIds }
                                    } else 0
                                    val displayName = buildAreaDisplayName(area, variant, if (entityCount > 0) entityCount else null)
                                    result.add(AreaAndLabel(area, displayName, variant))
                                }
                            }
                        } else {
                            val displayName = buildAreaDisplayName(area, null, null)
                            result.add(AreaAndLabel(area, displayName, null))
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
        map(_selectedAreaAndLabel, questEditorStore.currentArea, questEditorStore.currentAreaVariant) { selectedAreaAndLabel, storeCurrentArea, storeVariant ->
            // If there's an explicitly selected AreaAndLabel, use it
            selectedAreaAndLabel ?:
            // Otherwise, match by both area and variant for multi-floor quests
            if (storeVariant != null) {
                areas.find { it.area == storeCurrentArea && it.variant == storeVariant }
            } else {
                areas.find { it.area.id == storeCurrentArea?.id }
            } ?:
            // Final fallback to first area
            areas.firstOrNull()
        }
    }

    val areaSelectEnabled: Cell<Boolean> = questEditorStore.currentQuest.isNotNull()

    // Settings

    val showCollisionGeometry: Cell<Boolean> = questEditorStore.showCollisionGeometry
    val showSectionIds: Cell<Boolean> = questEditorStore.showSectionIds
    val showDoorIds: Cell<Boolean> = questEditorStore.showDoorIds
    val spawnMonstersOnGround: Cell<Boolean> = questEditorStore.spawnMonstersOnGround
    val showOriginPoint: Cell<Boolean> = questEditorStore.showOriginPoint

    // Free roam variant controls

    private val _freeRoamV1Options = mutableListCell<Int>()
    private val _freeRoamV2Options = mutableListCell<Int>()

    val freeRoamV1Options: ListCell<Int> = _freeRoamV1Options
    val freeRoamV2Options: ListCell<Int> = _freeRoamV2Options
    val showFreeRoamV1: Cell<Boolean> = _freeRoamState.map { it != null && it.v1Values.size > 1 }
    val showFreeRoamV2: Cell<Boolean> = _freeRoamState.map { it != null && it.v2Values.size > 1 }
    val showFreeRoamOnOff: Cell<Boolean> = _freeRoamState.map { it?.hasOnOff == true }
    val showFreeRoamUltimate: Cell<Boolean> = _freeRoamState.map { it?.hasUltimate == true }
    val freeRoamV1: Cell<Int?> = _freeRoamV1
    val freeRoamV2: Cell<Int?> = _freeRoamV2
    val freeRoamOffline: Cell<Boolean> = _freeRoamOffline
    val freeRoamUltimate: Cell<Boolean> = _freeRoamUltimate

    // Go to Section functionality
    // Use the store's selectedSection directly to ensure sync when clicking events
    val selectedSection: Cell<SectionModel?> = questEditorStore.selectedSection
    val gotoSectionEnabled: Cell<Boolean> = questEditorStore.currentAreaVariant.isNotNull()

    private val gotoSectionFilter: MutableCell<(SectionModel) -> Boolean> =
        mutableCell { true }

    val filteredSections: Cell<List<SectionModel>> =
        map(questEditorStore.currentAreaSections, gotoSectionFilter) { sections, filter ->
            sections.filter(filter)
        }

    init {
        addDisposables(
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
        )
    }

    suspend fun createNewQuest(episode: Episode) {
        clearFreeRoamState()
        setCurrentQuest(fileHolder = null, Version.BB, questEditorStore.getDefaultQuest(episode))
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
        clearFreeRoamState()
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
        clearFreeRoamState()
        val shiftJis = isJapaneseFilename(strategy.binFile.name)
        val parseResult = parseBinDatToQuestAutoDetect(
            strategy.binFile.cursor(Endianness.Little),
            strategy.datFile.cursor(Endianness.Little),
            shiftJis = shiftJis,
        )
        setResult(parseResult)

        if (parseResult is Success) {
            setCurrentQuest(
                FileHolder.BinDat(strategy.binFile, strategy.datFile, parseResult.value.compressed),
                Version.BB,
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

        setupFreeRoamState(
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

    suspend fun saveAsDialogSave() {
        if (!saveAsEnabled.value) return

        val quest = questEditorStore.currentQuest.value ?: return

        try {
            saving.value = true

            val headerFilename = filename.value.trim()
            val filename =
                if (headerFilename.endsWith(".qst")) headerFilename
                else "$headerFilename.qst"

            val buffer = writeQuestToQst(
                convertQuestFromModel(quest),
                headerFilename,
                version.value,
                online = true,
                compressed = compressed.value,
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
        questEditorStore.setCurrentArea(areaAndLabel.area)
        questEditorStore.setCurrentAreaVariant(areaAndLabel.variant)
    }

    fun setShowCollisionGeometry(show: Boolean) {
        questEditorStore.setShowCollisionGeometry(show)
    }

    fun setShowSectionIds(show: Boolean) {
        questEditorStore.setShowSectionIds(show)
    }

    fun setShowDoorIds(show: Boolean) {
        questEditorStore.setShowDoorIds(show)
    }

    fun setSpawnMonstersOnGround(spawn: Boolean) {
        questEditorStore.setSpawnMonstersOnGround(spawn)
    }

    fun setShowOriginPoint(show: Boolean) {
        questEditorStore.setShowOriginPoint(show)
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

    // Free roam variant change methods

    suspend fun setFreeRoamV1(v1: Int) {
        _freeRoamV1.value = v1
        reloadFreeRoam()
    }

    suspend fun setFreeRoamV2(v2: Int) {
        _freeRoamV2.value = v2
        reloadFreeRoam()
    }

    suspend fun setFreeRoamOffline(offline: Boolean) {
        _freeRoamOffline.value = offline
        reloadFreeRoam()
    }

    suspend fun setFreeRoamUltimate(ultimate: Boolean) {
        _freeRoamUltimate.value = ultimate
        reloadFreeRoam()
    }

    private suspend fun reloadFreeRoam() {
        val state = _freeRoamState.value ?: return
        val v1 = _freeRoamV1.value ?: 0
        val v2 = _freeRoamV2.value ?: 0

        // Preserve current area ID across variant reload.
        val previousAreaId = currentArea.value?.area?.id

        try {
            val info = FreeRoamAreaInfo(
                episode = state.episode,
                floorRange = state.floorRange,
                binPrefix = state.binPrefix,
                tokenBase = state.tokenBase,
                isCity = state.isCity,
                offline = _freeRoamOffline.value,
                ultimate = _freeRoamUltimate.value,
            )
            val result = questEditorStore.getFreeRoamQuest(state.gameDirHandle!!, info, v1, v2)
            setCurrentQuest(
                FileHolder.FreeRoamDir(state.gameDirHandle, result.binName, result.datFilesByFloor),
                Version.BB,
                result.questModel,
            )

            // Restore area selection by setting the store's area/variant.
            // Don't set _selectedAreaAndLabel — let currentArea's fallback logic
            // find the matching AreaAndLabel from the current areas list, so it
            // stays in sync when areas is recomputed.
            if (previousAreaId != null) {
                val match = areas.value.find { it.area.id == previousAreaId }
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

    private fun setupFreeRoamState(
        episode: Episode,
        floorRange: IntRange,
        gameDirHandle: FileSystemDirectoryHandle,
        binPrefix: String?,
        isCity: Boolean,
        initialOffline: Boolean = false,
        initialUltimate: Boolean = false,
        tokenBase: String? = null,
    ) {
        val (v1Values, v2Values) = computeSelectableVariants(episode, floorRange)
        _freeRoamState.value = FreeRoamState(
            episode, floorRange, gameDirHandle,
            v1Values, v2Values,
            binPrefix, isCity,
            hasOnOff = isCity,
            hasUltimate = episode == Episode.II,
            tokenBase = tokenBase,
        )
        _freeRoamV1.value = v1Values.firstOrNull()
        _freeRoamV2.value = v2Values.firstOrNull()
        _freeRoamV1Options.value = v1Values
        _freeRoamV2Options.value = v2Values
        _freeRoamOffline.value = initialOffline
        _freeRoamUltimate.value = initialUltimate
    }

    private fun clearFreeRoamState() {
        _freeRoamState.value = null
        _freeRoamV1.value = null
        _freeRoamV2.value = null
        _freeRoamV1Options.clear()
        _freeRoamV2Options.clear()
    }

    /**
     * Compute selectable v1/v2 values for an area group.
     * Only includes values from floors that have more than one option.
     */
    private fun computeSelectableVariants(
        episode: Episode,
        floorRange: IntRange,
    ): Pair<List<Int>, List<Int>> {
        val allV1 = mutableSetOf<Int>()
        val allV2 = mutableSetOf<Int>()
        for (floor in floorRange) {
            val info = getFloorFileInfo(episode, floor) ?: continue
            if (info.v1Values.size > 1) allV1.addAll(info.v1Values)
            if (info.v2Values.size > 1) allV2.addAll(info.v2Values)
        }
        return Pair(allV1.sorted(), allV2.sorted())
    }

    private fun setFileHolder(fileHolder: FileHolder?) {
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

        val allDatNames = holder.datFilesByFloor.values.flatMap { (obj, npc) -> listOf(obj, npc) }
        val allNames = listOfNotNull(holder.binName) + allDatNames
        for (name in allNames) {
            try {
                val originalData = dataDir.getFileHandle(name).await()
                    .getFile().await().arrayBuffer().await()

                val backupHandle = backupDir.getFileHandle(name, obj { create = true }).await()
                val writable = backupHandle.createWritable().await()
                writable.write(originalData).await()
                writable.close().await()
            } catch (_: Throwable) {
                // File might not exist (e.g., empty floor), skip backup.
            }
        }

        // 2. Write modified bin (uncompressed) if we have a bin file.
        if (holder.binName != null) {
            val (bin, _) = writeQuestToBinDat(convertedQuest, Version.BB)

            val binHandle = dataDir.getFileHandle(holder.binName).await()
            val binWritable = binHandle.createWritable().await()
            binWritable.write(bin.arrayBuffer).await()
            binWritable.close().await()
        }

        // 3. Write modified dat files (split by floor).
        val entityDataByFloor = extractRawEntityDataByFloor(convertedQuest)

        for ((floorId, datNames) in holder.datFilesByFloor) {
            val (objDatName, npcDatName) = datNames
            val (objBuf, npcBuf) = entityDataByFloor[floorId]
                ?: continue // No entities for this floor.

            val objHandle = dataDir.getFileHandle(objDatName, obj { create = true }).await()
            val objWritable = objHandle.createWritable().await()
            objWritable.write(objBuf.arrayBuffer).await()
            objWritable.close().await()

            val npcHandle = dataDir.getFileHandle(npcDatName, obj { create = true }).await()
            val npcWritable = npcHandle.createWritable().await()
            npcWritable.write(npcBuf.arrayBuffer).await()
            npcWritable.close().await()
        }

        questEditorStore.questSaved()
    }



    private sealed class QuestLoadStrategy {
        class Qst(val file: FileHandle) : QuestLoadStrategy()
        class BinDat(val binFile: FileHandle, val datFile: FileHandle) : QuestLoadStrategy()
        class FreeRoam(val info: FreeRoamAreaInfo) : QuestLoadStrategy()
    }

    private class FreeRoamState(
        val episode: Episode,
        val floorRange: IntRange,
        val gameDirHandle: FileSystemDirectoryHandle,
        val v1Values: List<Int>,
        val v2Values: List<Int>,
        val binPrefix: String?,
        val isCity: Boolean,
        val hasOnOff: Boolean,
        val hasUltimate: Boolean,
        val tokenBase: String? = null,
    )

    private sealed class FileHolder {
        class Qst(val file: FileHandle) : FileHolder()
        class BinDat(val binFile: FileHandle, val datFile: FileHandle, val compressed: Boolean) : FileHolder()
        class FreeRoamDir(
            val dirHandle: FileSystemDirectoryHandle,
            val binName: String?,
            /** Map of floorId → (objDatName, npcDatName) for saving back. */
            val datFilesByFloor: Map<Int, Pair<String, String>>,
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
    }
}
