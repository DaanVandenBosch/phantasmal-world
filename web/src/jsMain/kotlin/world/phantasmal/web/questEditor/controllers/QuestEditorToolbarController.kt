package world.phantasmal.web.questEditor.controllers

import kotlinx.coroutines.await
import mu.KotlinLogging
import world.phantasmal.cell.*
import world.phantasmal.core.*
import world.phantasmal.psolib.Endianness
import world.phantasmal.psolib.Episode
import world.phantasmal.psolib.fileFormats.quest.*
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

    // We mainly disable saving while a save is underway for visual feedback that a save is
    // happening/has happened.
    private val savingEnabled = questEditorStore.currentQuest.isNotNull() and !saving
    private val _saveAsDialogVisible = mutableCell(false)
    private val fileHolder = mutableCell<FileHolder?>(null)
    private val _filename = mutableCell("")
    private val _version = mutableCell(Version.BB)

    // Result

    val resultDialogVisible: Cell<Boolean> = _resultDialogVisible
    val result: Cell<PwResult<*>?> = _result

    val supportedFileTypes = listOf(
        FileType(
            description = "Quests",
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
                    // For each area in episode order, either show floor mappings or simple entry

                    // Group floor mappings by area ID for easy lookup
                    val floorMappingsByArea = quest.floorMappings.groupBy { it.areaId }

                    // Process areas in episode order
                    for (area in areaStore.getAreasForEpisode(quest.episode)) {
                        val areaFloorMappings = floorMappingsByArea[area.id]

                        if (areaFloorMappings != null) {
                            // This area has floor mappings, add entries for each mapping
                            for (mapping in areaFloorMappings) {
                                val entityCount = entitiesPerArea[mapping.floorId]
                                val variant = areaStore.getVariant(quest.episode, mapping.areaId, mapping.variantId)

                                if (variant != null) {
                                    val displayName = buildAreaDisplayName(area, variant, entityCount)
                                    result.add(AreaAndLabel(area, displayName, variant))
                                }
                            }
                        } else {
                            // This area has no floor mappings, add simple entry
                            val displayName = buildAreaDisplayName(area, null, null)
                            result.add(AreaAndLabel(area, displayName, null))
                        }
                    }
                } else {
                    for (area in areaStore.getAreasForEpisode(quest.episode)) {
                        val entityCount = getEntityCountForArea(entitiesPerArea, area)
                        val areaVariants = getAreaVariants(area, variants)

                        if (areaVariants.isNotEmpty()) {
                            // Create an entry for each variant of this area
                            for (variant in areaVariants) {
                                val displayName = buildAreaDisplayName(area, variant, entityCount)
                                result.add(AreaAndLabel(area, displayName, variant))
                            }
                        } else {
                            // No variants found, show standard entry for this area
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

    val currentArea: Cell<AreaAndLabel?> = map(areas, _selectedAreaAndLabel, questEditorStore.currentArea) { areas, selectedAreaAndLabel, storeCurrentArea ->
        // If there's an explicitly selected AreaAndLabel, use it
        selectedAreaAndLabel ?:
        // Otherwise, find the AreaAndLabel that matches the store's current area
        areas.find { it.area.id == storeCurrentArea?.id } ?:
        // Final fallback to first area
        areas.firstOrNull()
    }

    val areaSelectEnabled: Cell<Boolean> = questEditorStore.currentQuest.isNotNull()

    // Settings

    val showCollisionGeometry: Cell<Boolean> = questEditorStore.showCollisionGeometry
    val showSectionIds: Cell<Boolean> = questEditorStore.showSectionIds
    val spawnMonstersOnGround: Cell<Boolean> = questEditorStore.spawnMonstersOnGround
    val showOriginPoint: Cell<Boolean> = questEditorStore.showOriginPoint

    // Go to Section functionality
    // Use the store's selectedSection directly to ensure sync when clicking events
    val selectedSection: Cell<SectionModel?> = questEditorStore.selectedSection
    val availableSections: Cell<List<SectionModel>> = questEditorStore.currentAreaSections
    val gotoSectionEnabled: Cell<Boolean> = questEditorStore.currentAreaVariant.isNotNull()

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
        setCurrentQuest(fileHolder = null, Version.BB, questEditorStore.getDefaultQuest(episode))
    }

    suspend fun openFiles(newFiles: List<FileHandle>?) {
        try {
            if (newFiles.isNullOrEmpty()) return

            val qstFile = newFiles.find { it.extension().equals("qst", ignoreCase = true) }

            if (qstFile != null) {
                val parseResult = parseQstToQuest(qstFile.cursor(Endianness.Little))
                setResult(parseResult)

                if (parseResult is Success) {
                    setCurrentQuest(
                        FileHolder.Qst(qstFile),
                        parseResult.value.version,
                        parseResult.value.quest,
                    )
                }
            } else {
                val binFile = newFiles.find { it.extension().equals("bin", ignoreCase = true) }
                val datFile = newFiles.find { it.extension().equals("dat", ignoreCase = true) }

                if (binFile == null || datFile == null) {
                    setResult(
                        Failure(
                            listOf(
                                Problem(
                                    Severity.Error,
                                    "Please select a .qst file or one .bin and one .dat file.",
                                )
                            )
                        )
                    )
                    return
                }

                val parseResult = parseBinDatToQuest(
                    binFile.cursor(Endianness.Little),
                    datFile.cursor(Endianness.Little),
                )
                setResult(parseResult)

                if (parseResult is Success) {
                    setCurrentQuest(
                        FileHolder.BinDat(binFile, datFile),
                        Version.BB,
                        parseResult.value,
                    )
                }
            }
        } catch (e: Throwable) {
            setResult(
                PwResult.build<Nothing>(logger)
                    .addProblem(Severity.Error, "Couldn't parse file.", cause = e)
                    .failure()
            )
        }
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
                        val (bin, dat) = writeQuestToBinDat(
                            convertQuestFromModel(quest),
                            version.value,
                        )

                        holder.binFile.writeBuffer(bin)
                        holder.datFile.writeBuffer(dat)

                        questEditorStore.questSaved()
                        return
                    }
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
                console.log("Triggering section loading for area variant ${areaVariant.id}")
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

    private fun setFileHolder(fileHolder: FileHolder?) {
        setFilename(
            when (fileHolder) {
                is FileHolder.Qst -> fileHolder.file.basename() ?: fileHolder.file.name

                is FileHolder.BinDat ->
                    fileHolder.binFile.basename()
                        ?: fileHolder.datFile.basename()
                        ?: fileHolder.binFile.name

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

    private sealed class FileHolder {
        class Qst(val file: FileHandle) : FileHolder()
        class BinDat(val binFile: FileHandle, val datFile: FileHandle) : FileHolder()
    }
}
