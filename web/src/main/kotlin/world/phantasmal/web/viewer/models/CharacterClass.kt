package world.phantasmal.web.viewer.models

enum class CharacterClass(
    val uiName: String,
    val bodyStyleCount: Int,
    val headStyleCount: Int,
    val hairStyleCount: Int,
    val hairStylesWithAccessory: Set<Int>,
) {
    HUmar(
        uiName = "HUmar",
        bodyStyleCount = 18,
        headStyleCount = 1,
        hairStyleCount = 10,
        hairStylesWithAccessory = setOf(6),
    ),
    HUnewearl(
        uiName = "HUnewearl",
        bodyStyleCount = 18,
        headStyleCount = 1,
        hairStyleCount = 10,
        hairStylesWithAccessory = setOf(),
    ),
    HUcast(
        uiName = "HUcast",
        bodyStyleCount = 25,
        headStyleCount = 5,
        hairStyleCount = 0,
        hairStylesWithAccessory = setOf(),
    ),
    HUcaseal(
        uiName = "HUcaseal",
        bodyStyleCount = 25,
        headStyleCount = 5,
        hairStyleCount = 0,
        hairStylesWithAccessory = setOf(),
    ),
    RAmar(
        uiName = "RAmar",
        bodyStyleCount = 18,
        headStyleCount = 1,
        hairStyleCount = 10,
        hairStylesWithAccessory = setOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9),
    ),
    RAmarl(
        uiName = "RAmarl",
        bodyStyleCount = 18,
        headStyleCount = 1,
        hairStyleCount = 10,
        hairStylesWithAccessory = setOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9),
    ),
    RAcast(
        uiName = "RAcast",
        bodyStyleCount = 25,
        headStyleCount = 5,
        hairStyleCount = 0,
        hairStylesWithAccessory = setOf(),
    ),
    RAcaseal(
        uiName = "RAcaseal",
        bodyStyleCount = 25,
        headStyleCount = 5,
        hairStyleCount = 0,
        hairStylesWithAccessory = setOf(),
    ),
    FOmar(
        uiName = "FOmar",
        bodyStyleCount = 18,
        headStyleCount = 1,
        hairStyleCount = 10,
        hairStylesWithAccessory = setOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9),
    ),
    FOmarl(
        uiName = "FOmarl",
        bodyStyleCount = 18,
        headStyleCount = 1,
        hairStyleCount = 10,
        hairStylesWithAccessory = setOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9),
    ),
    FOnewm(
        uiName = "FOnewm",
        bodyStyleCount = 18,
        headStyleCount = 1,
        hairStyleCount = 10,
        hairStylesWithAccessory = setOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9),
    ),
    FOnewearl(
        uiName = "FOnewearl",
        bodyStyleCount = 18,
        headStyleCount = 1,
        hairStyleCount = 10,
        hairStylesWithAccessory = setOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9),
    );

    val slug: String = name

    companion object {
        val VALUES: List<CharacterClass> = values().toList()
    }
}
