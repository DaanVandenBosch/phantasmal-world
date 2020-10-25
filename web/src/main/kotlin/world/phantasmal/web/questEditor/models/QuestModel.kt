package world.phantasmal.web.questEditor.models

import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.mutableVal

class QuestModel(
    id: Int,
    language: Int,
    name: String,
    shortDescription: String,
    longDescription: String,
) {
    private val _id = mutableVal(0)
    private val _language = mutableVal(0)
    private val _name = mutableVal("")
    private val _shortDescription = mutableVal("")
    private val _longDescription = mutableVal("")

    val id: Val<Int> = _id
    val language: Val<Int> = _language
    val name: Val<String> = _name
    val shortDescription: Val<String> = _shortDescription
    val longDescription: Val<String> = _longDescription

    init {
        setId(id)
        setLanguage(language)
        setName(name)
        setShortDescription(shortDescription)
        setLongDescription(longDescription)
    }

    fun setId(id: Int): QuestModel {
        require(id >= 0) { "id should be greater than or equal to 0, was ${id}." }

        _id.value = id
        return this
    }

    fun setLanguage(language: Int): QuestModel {
        require(language >= 0) { "language should be greater than or equal to 0, was ${language}." }

        _language.value = language
        return this
    }

    fun setName(name: String): QuestModel {
        require(name.length <= 32) { """name can't be longer than 32 characters, got "$name".""" }

        _name.value = name
        return this
    }

    fun setShortDescription(shortDescription: String): QuestModel {
        require(shortDescription.length <= 128) {
            """shortDescription can't be longer than 128 characters, got "$shortDescription"."""
        }

        _shortDescription.value = shortDescription
        return this
    }

    fun setLongDescription(longDescription: String): QuestModel {
        require(longDescription.length <= 288) {
            """longDescription can't be longer than 288 characters, got "$longDescription"."""
        }

        _longDescription.value = longDescription
        return this
    }
}
