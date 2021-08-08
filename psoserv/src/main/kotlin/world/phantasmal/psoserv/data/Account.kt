package world.phantasmal.psoserv.data

class Account(
    val id: Long,
    val username: String,
    val guildCardNo: Int,
    val teamId: Int,
    val characters: List<Character>,
) {
    init {
        require(username.length <= 16)
    }
}

class PlayingAccount(
    val account: Account,
    val char: Character,
    val blockId: Int,
)

class Character(
    val id: Long,
    val accountId: Long,
    val name: String,
    val sectionId: SectionId,
    val exp: Int,
    val level: Int,
) {
    init {
        require(name.length <= 16)
        require(exp >= 0)
        require(level in 1..200)
    }
}

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
    Whitill,
}
