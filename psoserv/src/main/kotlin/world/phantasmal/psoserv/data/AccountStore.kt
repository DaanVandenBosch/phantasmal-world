package world.phantasmal.psoserv.data

import mu.KLogger

class AccountStore(private val logger: KLogger) {
    private var nextId: Long = 1L
    private var nextGuildCardNo: Int = 1

    /**
     * Maps usernames to accounts. Accounts are created on the fly.
     */
    private val accounts = mutableMapOf<String, AccountData>()

    /**
     * Logged in accounts must always be logged out with [logOut].
     */
    fun logIn(username: String, password: String): LogInResult =
        synchronized(this) {
            val data = accounts.getOrPut(username) {
                val accountId = nextId++
                AccountData(
                    account = Account(
                        id = accountId,
                        username = username,
                        guildCardNo = nextGuildCardNo++,
                        teamId = 1337,
                        characters = listOf(
                            Character(
                                id = nextId++,
                                accountId = accountId,
                                name = "${username.take(14)} 1",
                                sectionId = SectionId.Viridia,
                                exp = 1_000_000,
                                level = 200,
                            )
                        ),
                    ),
                    playing = null,
                    password = password,
                    loggedIn = false,
                )
            }

            if (password != data.password) {
                LogInResult.BadPassword
            } else if (data.loggedIn) {
                LogInResult.AlreadyLoggedIn
            } else {
                data.loggedIn = true
                LogInResult.Ok(data.account)
            }
        }

    fun logOut(accountId: Long) {
        synchronized(this) {
            val data = accounts.values.find { it.account.id == accountId }

            if (data == null) {
                logger.warn {
                    "Trying to log out nonexistent account $accountId."
                }
            } else {
                if (!data.loggedIn) {
                    logger.warn {
                        """Trying to log out account ${data.account.id} "${data.account.username}" while it wasn't logged in."""
                    }
                }

                data.playing = null
                data.loggedIn = false
            }
        }
    }

    fun getAccountById(accountId: Long): Account? =
        synchronized(this) {
            accounts.values.find { it.account.id == accountId }?.account
        }

    fun setAccountPlaying(accountId: Long, char: Character, blockId: Int): Account {
        synchronized(this) {
            val data = accounts.values.first { it.account.id == accountId }
            data.playing = PlayingAccount(data.account, char, blockId)
            return data.account
        }
    }

    fun getAccountsByBlock(blockId: Int): List<PlayingAccount> =
        synchronized(this) {
            accounts.values
                .filter { it.loggedIn && it.playing?.blockId == blockId }
                .mapNotNull { it.playing }
        }

    sealed class LogInResult {
        class Ok(val account: Account) : LogInResult()
        object BadPassword : LogInResult()
        object AlreadyLoggedIn : LogInResult()
    }

    private class AccountData(
        var account: Account,
        var playing: PlayingAccount?,
        val password: String,
        var loggedIn: Boolean,
    ) {
        init {
            require(password.length <= 16)
        }
    }
}
