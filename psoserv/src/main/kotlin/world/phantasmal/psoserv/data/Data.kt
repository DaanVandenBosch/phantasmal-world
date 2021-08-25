package world.phantasmal.psoserv.data

import mu.KLogger
import world.phantasmal.psolib.Episode
import world.phantasmal.psoserv.messages.Message
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/*
 * Whenever data is changed in these classes, locks should be acquired in this order:
 *  1. Store
 *  2. LobbyOrParty
 *  3. Client.
 */

private const val MAX_ACCOUNTS_PER_LOBBY: Int = 20
private const val MAX_ACCOUNTS_PER_PARTY: Int = 4

@RequiresOptIn(message = "This API is internal and should not be accessed from outside the file it was defined in.")
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
private annotation class Internal

// TODO: Periodically log out stale accounts.
// TODO: Periodically remove stale parties (lobbies too?).
@OptIn(Internal::class)
class Store(private val logger: KLogger) {
    /**
     * All operations on this object must synchronize on this lock.
     */
    private val lock = Any()
    private var nextAccountId: Long = 1L
    private var nextCharId: Long = 1L
    private var nextGuildCardNo: Int = 1

    private val nameToClient = mutableMapOf<String, Client>()

    private val blockIdToLobbyIdToLobby = mutableMapOf<Int, List<Lobby>>()
    private var nextPartyId: Int = 1
    private val blockIdToNameToParty = mutableMapOf<Int, MutableMap<String, Party>>()

    fun authenticate(name: String, password: String, sendMessage: (Message) -> Unit): AuthResult {
        val client = synchronized(lock) {
            // Simply create the account and client if it doesn't exist yet.
            nameToClient.getOrPut(name) {
                val accountId = nextAccountId++
                Client(
                    logger = logger,
                    account = Account(
                        id = accountId,
                        name = name,
                        guildCardNo = nextGuildCardNo++,
                        teamId = 1337,
                        characters = listOf(
                            Character(
                                id = nextCharId++,
                                accountId = accountId,
                                name = "${name.take(14)} 1",
                                sectionId = SectionId.Viridia,
                                exp = 1_000_000,
                                level = 200,
                            )
                        ),
                    ),
                    password = password,
                )
            }
        }

        return client.logIn(password, sendMessage)
    }

    fun logOut(client: Client) {
        synchronized(lock) {
            synchronizedClientAndLop(client) {
                val lop = client.lop

                if (lop is Party) {
                    lop.removeClient(client)

                    if (lop.isEmpty) {
                        val nameToParty = blockIdToNameToParty[lop.blockId]
                            ?: return

                        nameToParty.remove(lop.details.name, lop)
                    }
                }

                client.logOut()
            }
        }
    }

    fun getLobbies(blockId: Int): List<Lobby> =
        synchronized(lock) {
            blockIdToLobbyIdToLobby.getOrPut(blockId) {
                // Create lobbies if necessary.
                // BlueBurst needs precisely 15 lobbies. It seems like they need to have IDs 0..14.
                (0..14).map { lobbyId ->
                    Lobby(lobbyId, blockId)
                }
            }
        }

    /** Returns null if no lobbies are available or [client] is already in a lobby or party. */
    fun joinFirstAvailableLobby(blockId: Int, client: Client): Lobby? =
        synchronized(lock) {
            val lobbies = getLobbies(blockId)

            for (lobby in lobbies) {
                synchronized(lobby) {
                    // Do unnecessary check here, so we don't have to lock on client everytime.
                    if (!lobby.isFull) {
                        synchronized(client.lock) {
                            if (client.lop != null) {
                                return null
                            }

                            val id = lobby.addClient(client)

                            // Can't be -1 at this point.
                            if (id.toInt() != -1) {
                                client.setLop(lobby, id)
                                return lobby
                            }
                        }
                    }
                }
            }

            return null
        }

    /**
     * Creates a new party and adds [leader] to it. Return null if a party with [name] already
     * exists.
     */
    fun createAndJoinParty(
        blockId: Int,
        name: String,
        password: String,
        difficulty: Difficulty,
        episode: Episode,
        mode: Mode,
        leader: Client,
    ): CreateAndJoinPartyResult {
        synchronized(lock) {
            val nameToParty = blockIdToNameToParty.getOrPut(blockId, ::mutableMapOf)

            if (name in nameToParty) {
                return CreateAndJoinPartyResult.NameInUse
            }

            val details = PartySettings(name, password, difficulty, episode, mode)
            val party = Party(nextPartyId++, blockId, details)

            synchronized(party.lock) {
                synchronizedClientAndLop(leader) {
                    if (leader.lop is Party) {
                        return CreateAndJoinPartyResult.AlreadyInParty
                    }

                    leader.lop?.removeClient(leader)
                    val id = party.addClient(leader)
                    check(id.toInt() != -1) { "Couldn't add client to newly created party." }
                    leader.setLop(party, id)
                }
            }

            // Do this at the last possible time, so any exceptions and early returns can prevent
            // the party from being added to the store.
            nameToParty[name] = party

            return CreateAndJoinPartyResult.Ok(party)
        }
    }

    /**
     * Acquires the LOP lock and then the client lock.
     */
    private inline fun <T> synchronizedClientAndLop(
        client: Client,
        block: () -> T,
    ): T {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }

        while (true) {
            val lop = client.lop

            if (lop == null) {
                synchronized(client.lock) {
                    if (client.lop == lop) {
                        return block()
                    }

                    // At this point we know LOP changed since last check, retry because we need to
                    // lock on LOP first.
                }
            } else {
                synchronized(lop.lock) {
                    synchronized(client.lock) {
                        if (client.lop == lop) {
                            return block()
                        }

                        // At this point we know LOP changed since last check, we're holding the
                        // wrong LOP lock. Retry because we need to lock on LOP first.
                    }
                }
            }
        }
    }
}

@OptIn(Internal::class)
class Client(
    private val logger: KLogger,
    account: Account,
    private val password: String,
) {
    /**
     * All operations on this object must synchronize on this lock.
     */
    @Internal
    val lock = Any()
    private var _account: Account = account
    private var _playing: PlayingAccount? = null
    private var _lop: LobbyOrParty? = null
    private var _id: Byte = -1

    /** Non-null when logged in. */
    private var sendMessage: ((Message) -> Unit)? = null

    val account: Account get() = synchronized(lock) { _account }
    val playing: PlayingAccount? get() = synchronized(lock) { _playing }
    val lop: LobbyOrParty? get() = synchronized(lock) { _lop }

    /**
     * LOP-specific ID. -1 when not in a LOP.
     */
    val id: Byte get() = synchronized(lock) { _id }

    init {
        require(password.length <= 16)
    }

    fun setPlaying(char: Character, blockId: Int) {
        synchronized(lock) {
            require(sendMessage != null) { "Trying to set a logged out account to playing." }

            _playing = PlayingAccount(account, char, blockId)
        }
    }

    fun sendMessage(message: Message) {
        val snd = synchronized(lock) { sendMessage }
        // Do blocking call outside synchronized block.
        snd?.invoke(message)
    }

    @Internal
    fun logIn(password: String, sendMessage: (Message) -> Unit): AuthResult =
        synchronized(lock) {
            if (password != this.password) {
                AuthResult.BadPassword
            } else if (this.sendMessage != null) {
                AuthResult.AlreadyLoggedIn
            } else {
                this.sendMessage = sendMessage
                AuthResult.Ok(this)
            }
        }

    @Internal
    fun logOut() {
        synchronized(lock) {
            if (sendMessage == null) {
                logger.warn {
                    """Trying to log out account ${account.id} "${account.name}" while it wasn't logged in."""
                }
            }

            sendMessage = null
            _playing = null
            _lop = null
        }
    }

    @Internal
    fun setLop(lop: LobbyOrParty?, id: Byte) {
        synchronized(lock) {
            _id = id
            _lop = lop
        }
    }
}

@OptIn(Internal::class)
sealed class LobbyOrParty(val id: Int, val blockId: Int, private val maxClients: Int) {
    private var clientCount = 0

    /**
     * All operations on this object must synchronize on this lock. All exposed data must be deeply
     * immutable and represent a consistent snapshot of the object's state at the time of retrieval.
     */
    @Internal
    val lock = Any()

    private var _clients: MutableList<Client?> = MutableList(maxClients) { null }
    private var _leaderId: Byte = -1

    val isEmpty: Boolean get() = synchronized(lock) { clientCount == 0 }
    val isFull: Boolean get() = synchronized(lock) { clientCount >= maxClients }

    /** -1 If LOP has no clients. */
    val leaderId: Byte get() = synchronized(lock) { _leaderId }

    fun getClients(): List<Client> = synchronized(lock) { _clients.filterNotNull() }

    fun broadcastMessage(message: Message, exclude: Client?) {
        val clients = mutableListOf<Client>()

        synchronized(lock) {
            for (client in _clients) {
                if (client != null && client != exclude) {
                    clients.add(client)
                }
            }
        }

        // Do blocking calls outside of synchronized block.
        for (client in clients) {
            client.sendMessage(message)
        }
    }

    /**
     * Returns the ID of the client within this lobby when the client can be added, -1 otherwise.
     */
    @Internal
    fun addClient(client: Client): Byte {
        synchronized(lock) {
            val iter = _clients.listIterator()

            while (iter.hasNext()) {
                val id = iter.nextIndex().toByte()

                if (iter.next() == null) {
                    iter.set(client)

                    if (clientCount == 0) {
                        _leaderId = id
                    }

                    clientCount++
                    return id
                }
            }
        }

        return -1
    }

    @Internal
    fun removeClient(client: Client) {
        synchronized(lock) {
            // Find the client's ID ourselves, so we don't need to lock on client. This way we also
            // don't have to trust client.
            val id = _clients.indexOf(client)

            if (id != -1) {
                _clients[id] = null
                clientCount--

                if (clientCount == 0) {
                    _leaderId = -1
                }

                if (id == _leaderId.toInt()) {
                    _leaderId = _clients.firstNotNullOfOrNull { it }?.id ?: -1
                }
            }
        }
    }
}

class Lobby(id: Int, blockId: Int) : LobbyOrParty(id, blockId, MAX_ACCOUNTS_PER_LOBBY)

@OptIn(Internal::class)
class Party(id: Int, blockId: Int, details: PartySettings) :
    LobbyOrParty(id, blockId, MAX_ACCOUNTS_PER_PARTY) {

    private var _details: PartySettings = details

    val details: PartySettings get() = synchronized(lock) { _details }
}

class Account(
    val id: Long,
    val name: String,
    val guildCardNo: Int,
    val teamId: Int,
    val characters: List<Character>,
) {
    init {
        require(name.length <= 16)
    }

    override fun toString(): String = "Account[id=$id,name=$name]"
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

class PartySettings(
    val name: String,
    val password: String,
    val difficulty: Difficulty,
    val episode: Episode,
    val mode: Mode,
)

enum class Difficulty {
    Normal, Hard, VHard, Ultimate
}

enum class Mode {
    Normal, Battle, Challenge, Solo
}

sealed class AuthResult {
    class Ok(val client: Client) : AuthResult()
    object BadPassword : AuthResult()
    object AlreadyLoggedIn : AuthResult()
}

sealed class CreateAndJoinPartyResult {
    class Ok(val party: Party) : CreateAndJoinPartyResult()
    object NameInUse : CreateAndJoinPartyResult()
    object AlreadyInParty : CreateAndJoinPartyResult()
}
