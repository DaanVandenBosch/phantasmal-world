package world.phantasmal.web.questEditor.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class DockedItemDto {
    abstract val flex: Double?
}

@Serializable
sealed class DockedContainerDto : DockedItemDto() {
    abstract val items: List<DockedItemDto>
}

@Serializable
@SerialName("row")
class DockedRowDto(
    override val flex: Double?,
    override val items: List<DockedItemDto>,
) : DockedContainerDto()

@Serializable
@SerialName("column")
class DockedColumnDto(
    override val flex: Double?,
    override val items: List<DockedItemDto>,
) : DockedContainerDto()

@Serializable
@SerialName("stack")
class DockedStackDto(
    val activeItemIndex: Int?,
    override val flex: Double?,
    override val items: List<DockedItemDto>,
) : DockedContainerDto()

@Serializable
@SerialName("widget")
class DockedWidgetDto(
    val id: String,
    val title: String,
    override val flex: Double?,
) : DockedItemDto()
