package world.phantasmal.psoserv.data

import mu.KLogger

class AccountStore(private val logger: KLogger) {
    /**
     * All operations on this object must synchronize on this lock.
     */
    private val lock = Any()
    private var nextId: Long = 1L
    private var nextGuildCardNo: Int = 1

    private val idToAccountData = mutableMapOf<Long, AccountData>()
    private val usernameToAccountData = mutableMapOf<String, AccountData>()

    fun getAccountData(username: String, password: String): AccountData =
        synchronized(lock) {
            // Simply create the account if it doesn't exist yet.
            usernameToAccountData.getOrPut(username) {
                val accountId = nextId++
                AccountData(
                    logger = logger,
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
                ).also {
                    // Ensure it can also be found by ID.
                    idToAccountData[accountId] = it
                }
            }
        }

    fun getPlayingAccountsForBlock(blockId: Int): List<PlayingAccount> =
        synchronized(lock) {
            idToAccountData.values.asSequence()
                .mapNotNull { it.playing } // Map before filtering to avoid race condition.
                .filter { it.blockId == blockId }
                .toList()
        }
}
