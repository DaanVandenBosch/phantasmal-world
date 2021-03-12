package world.phantasmal.web.shared.dto

enum class SectionId {
    Viridia,
    Greenill,
    Skyly,
    Bluefull,
    Purplenum,
    Pinkal,
    Redria,
    Oran,
    Yellowboze,
    Whitill;

    val uiName: String = name

    companion object {
        val VALUES: Array<SectionId> = values()
        val VALUES_LIST: List<SectionId> = VALUES.toList()
    }
}
