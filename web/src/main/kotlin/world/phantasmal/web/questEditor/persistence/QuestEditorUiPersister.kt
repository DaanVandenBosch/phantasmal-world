package world.phantasmal.web.questEditor.persistence

import world.phantasmal.web.core.controllers.*
import world.phantasmal.web.core.persistence.Persister
import world.phantasmal.web.questEditor.dto.*

class QuestEditorUiPersister : Persister() {
    // TODO: Throttle this method.
    suspend fun persistLayoutConfig(config: DockedItem) {
        persist(LAYOUT_CONFIG_KEY, toDto(config))
    }

    suspend fun loadLayoutConfig(validWidgetIds: Set<String>): DockedItem? =
        load<DockedItemDto>(LAYOUT_CONFIG_KEY)?.let { config ->
            fromDto(config, validWidgetIds)
        }

    private fun toDto(item: DockedItem): DockedItemDto =
        when (item) {
            is DockedRow -> DockedRowDto(item.flex, item.items.map(::toDto))
            is DockedColumn -> DockedColumnDto(item.flex, item.items.map(::toDto))
            is DockedStack -> DockedStackDto(
                item.activeItemIndex,
                item.flex,
                item.items.map(::toDto)
            )
            is DockedWidget -> DockedWidgetDto(item.id, item.title, item.flex)
        }

    private fun fromDto(
        config: DockedItemDto,
        validWidgetIds: Set<String>,
    ): DockedItem? {
        val foundWidgetIds = mutableSetOf<String>()

        val sanitizedConfig = fromDto(config, validWidgetIds, foundWidgetIds)

        if (foundWidgetIds.size != validWidgetIds.size) {
            // A component was added or the persisted config is corrupt.
            return null
        }

        return sanitizedConfig
    }

    /**
     * Removes old components and adds titles and ids to current components.
     */
    private fun fromDto(
        item: DockedItemDto,
        validWidgetIds: Set<String>,
        foundWidgetIds: MutableSet<String>,
    ): DockedItem? =
        when (item) {
            is DockedContainerDto -> {
                val items = item.items.mapNotNull { fromDto(it, validWidgetIds, foundWidgetIds) }

                // Remove empty containers.
                if (items.isEmpty()) {
                    null
                } else {
                    when (item) {
                        is DockedRowDto -> DockedRow(item.flex, items)
                        is DockedColumnDto -> DockedColumn(item.flex, items)
                        is DockedStackDto -> DockedStack(
                            // Remove corrupted activeItemIndex properties.
                            item.activeItemIndex?.takeIf { it in items.indices },
                            item.flex,
                            items
                        )
                    }
                }
            }
            is DockedWidgetDto -> {
                // Remove deprecated components.
                if (item.id !in validWidgetIds) {
                    null
                } else {
                    foundWidgetIds.add(item.id)
                    DockedWidget(item.id, item.title, item.flex)
                }
            }
        }

    companion object {
        private const val LAYOUT_CONFIG_KEY = "QuestEditorUiPersister.layout_config"
    }
}
