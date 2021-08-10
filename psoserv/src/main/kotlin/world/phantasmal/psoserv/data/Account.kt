package world.phantasmal.psoserv.data

import mu.KLogger

class AccountData(
    private val logger: KLogger,
    account: Account,
    playing: PlayingAccount?,
    private val password: String,
    private var loggedIn: Boolean,
) {
    /**
     * All access to this class' properties must synchronize on this lock.
     */
    private val lock = Any()
    private var _account = account
    private var _playing = playing

    val account: Account get() = synchronized(lock) { _account }
    val playing: PlayingAccount? get() = synchronized(lock) { _playing }

    init {
        require(password.length <= 16)
    }

    fun logIn(password: String): LogInResult =
        synchronized(lock) {
            if (password != this.password) {
                LogInResult.BadPassword
            } else if (loggedIn) {
                LogInResult.AlreadyLoggedIn
            } else {
                loggedIn = true
                LogInResult.Ok
            }
        }

    fun logOut() {
        synchronized(lock) {
            if (!loggedIn) {
                logger.warn {
                    """Trying to log out account ${account.id} "${account.username}" while it wasn't logged in."""
                }
            }

            _playing = null
            loggedIn = false
        }
    }

    fun setPlaying(char: Character, blockId: Int) {
        synchronized(lock) {
            _playing = PlayingAccount(account, char, blockId)
            loggedIn = true
        }
    }
}

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

enum class LogInResult {
    Ok, BadPassword, AlreadyLoggedIn
}
