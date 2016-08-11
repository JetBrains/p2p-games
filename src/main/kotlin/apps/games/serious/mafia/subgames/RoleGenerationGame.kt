package apps.games.serious.mafia.subgames

import apps.chat.Chat
import apps.games.Game
import apps.games.GameExecutionException
import apps.games.GameManager
import apps.games.GameManagerClass
import apps.games.primitives.Deck
import apps.games.primitives.protocols.RandomDeckGame
import crypto.random.randomBigInt
import crypto.random.randomPermutation
import entity.ChatMessage
import entity.Group
import entity.User
import org.apache.commons.codec.digest.DigestUtils
import org.bouncycastle.jce.spec.ECParameterSpec
import proto.GameMessageProto
import java.math.BigInteger
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException

/**
 * Created by user on 8/9/16.
 */
class RoleGenerationGame(chat: Chat, group: Group, gameID: String, val ECParams: ECParameterSpec,
                         val roleCounts: List<Int>, gameManager: GameManagerClass = GameManager) : Game<RoleDeck>(
        chat, group, gameID, gameManager = gameManager) {
    override val name: String
        get() = "Role Generation Game"

    private enum class State {
        INIT,
        VALIDATE_KEYS,
        GENERATE_IV,
        SHUFFLE,
        LOCK,
        VALIDATE,
        EXCHANGE_R,
        VALIDATE_R,
        END
    }

    private var state: State = State.INIT
    private val deckSize: Int
    private val N: Int
    private val roles: Deck
    private val originalRoles: Deck
    private val V: Deck
    private var step: Int = -1
    private val rolesLockKeys: List<BigInteger>
    private val rolesLockKeyHashes: MutableMap<User, String> = mutableMapOf()
    private val VLockKeyHashes: MutableMap<User, String> = mutableMapOf()
    private val VLockKeys: List<BigInteger>
    private val R: List<BigInteger>
    private val commonR: MutableList<BigInteger>
    private val playerOrder: List<User>
    private val playerID: Int
    private val Vlock1 = randomBigInt(ECParams.n)
    private val Vlock2 = randomBigInt(ECParams.n)
    private val X: List<BigInteger>
    private val XHashes: MutableMap<User, String> = mutableMapOf()
    private val extraData = mutableListOf<ByteArray>()

    init {
        deckSize = roleCounts.sum()
        N = group.users.size
        rolesLockKeys = listOf(*Array(deckSize, { i -> randomBigInt(ECParams.n) }))
        VLockKeys = listOf(*Array(deckSize, { i -> randomBigInt(ECParams.n) }))
        R = listOf(*Array(deckSize, { i -> randomBigInt(ECParams.n) }))
        commonR = mutableListOf(*Array(deckSize, { i -> BigInteger.ONE }))
        roles = Deck(ECParams, deckSize)
        originalRoles = Deck(ECParams, deckSize)
        V = Deck(ECParams, deckSize)
        playerOrder = group.users.sortedBy { x -> x.name }
        playerID = playerOrder.indexOf(chat.me())

        X = mutableListOf<BigInteger>()
        for (i in 0..roleCounts.size - 1) {
            val t = randomBigInt(ECParams.n)
            for (j in 0..roleCounts[i] - 1) {
                X.add(t)
            }
        }

    }

    override fun evaluate(responses: List<GameMessageProto.GameStateMessage>): String {
        for (msg in responses) {
            chat.showMessage(msg.value)
        }
        extraData.clear()
        when (state) {
            State.INIT -> {
                //agree on roles
                initRoles()
                state = State.VALIDATE_KEYS
                return DigestUtils.sha256Hex(rolesLockKeys.joinToString(" ")) + " " +
                        DigestUtils.sha256Hex(VLockKeys.joinToString(" ")) + " " +
                        DigestUtils.sha256Hex(X.joinToString(" "))
            }
            State.VALIDATE_KEYS -> {
                for (msg in responses) {
                    val split = msg.value.split(" ")
                    rolesLockKeyHashes[User(msg.user)] = split[0]
                    VLockKeyHashes[User(msg.user)] = split[1]
                    XHashes[User(msg.user)] = split[2]
                }
                state = State.GENERATE_IV
                step = -1
                return ""

            }
            State.GENERATE_IV -> {
                generateIV(responses)
                if (step >= N) {
                    state = State.SHUFFLE
                    step = -1
                }
            }
            State.SHUFFLE -> {
                processShffling(responses)
                if (step >= N) {
                    step = -1
                    state = State.LOCK
                }

            }
            State.LOCK -> {
                processLocking(responses)
                if (step >= N) {
                    step = -1
                    state = State.VALIDATE
                    return V.hashCode().toString() + roles.hashCode().toString()
                }
            }
            State.VALIDATE -> {
                val hashes = responses.map { x -> x.value }
                if (hashes.distinct().size != 1) {
                    throw GameExecutionException("Someone has a different deck")
                }
                state = State.EXCHANGE_R
                return R.joinToString(" ")
            }
            State.EXCHANGE_R -> {
                for(msg in responses){
                    val userID = playerOrder.indexOf(User(msg.user))
                    val rs = msg.value.split(" ").map { x -> BigInteger(x) }
                    if(rs.size != deckSize){
                        throw GameExecutionException("Someoone faile to provide his R")
                    }
                    for(i in 0..deckSize-1){
                        commonR[i] *= rs[i]
                        commonR[i] %= ECParams.n
                    }

                }
                state = State.VALIDATE_R
                return commonR.joinToString(" ").hashCode().toString()
            }
            State.VALIDATE_R -> {
                val hashes = responses.map { x -> x.value }
                if (hashes.distinct().size != 1) {
                    throw GameExecutionException("Someone has a different deck")
                }
                state = State.END
            }
            State.END -> {
            }
        }
        return ""
    }

    /**
     * Create a common vector, that describes
     * Roles in mafia game
     */
    private fun initRoles() {
        val deckFuture = runSubGame(RandomDeckGame(chat, group, subGameID(), ECParams, deckSize, gameManager))
        val rolesPoints: Deck
        try {
            rolesPoints = deckFuture.get()
        } catch(e: CancellationException) {
            // Task was cancelled - means that we need to stop. NOW!
            state = State.END
            return
        } catch(e: ExecutionException) {
            chat.showMessage(ChatMessage(chat,
                    e.message ?: "Something went wrong"))
            e.printStackTrace()
            throw GameExecutionException(
                    "[${chat.me().name}] Subgame failed")
        }
        for (i in 0..deckSize - 1) {
            roles.cards[i] = rolesPoints.cards[i]
            originalRoles.cards[i] = roles.cards[i]
        }

    }

    /**
     * Perform a step in IV generation: create a common IV vector encrypted with
     * different R_ij values
     *
     * @param responses - answers from other players
     */
    private fun generateIV(responses: List<GameMessageProto.GameStateMessage>) {
        for (msg in responses) {
            val userID = playerOrder.indexOf(User(msg.user))
            if (userID == step) {
                if (playerID == step + 1 || step == N-1) {
                    if (msg.dataCount != deckSize) {
                        throw GameExecutionException(
                                "Someone failed to provide their deck")
                    }
                    for (i in 0..deckSize - 1) {
                        V.cards[i] = ECParams.curve.decodePoint(msg.dataList[i].toByteArray())
                    }
                }
            }
        }
        step++
        if (step == playerID) {
            V.encrypt(Vlock1)
            V.enctyptSeparate(X)
            V.enctyptSeparate(R)
            extraData.addAll(V.cards.map { x -> x.getEncoded(false) })
        }
    }

    /**
     * suhuffling stage, shufle roles and V with common permutation and
     * lock cards
     *
     * @param responses - answers from other players
     */
    private fun processShffling(responses: List<GameMessageProto.GameStateMessage>){
        for (msg in responses) {
            val userID = playerOrder.indexOf(User(msg.user))
            if (userID == step) {
                if (playerID == step + 1 || step == N-1) {
                    if (msg.dataCount != 2 * deckSize) {
                        throw GameExecutionException(
                                "Someone failed to provide their deck")
                    }
                    for (i in 0..deckSize - 1) {
                        V.cards[i] = ECParams.curve.decodePoint(msg.dataList[i].toByteArray())
                    }
                    for (i in deckSize..2 * deckSize - 1) {
                        roles.cards[i-deckSize] = ECParams.curve.decodePoint(msg.dataList[i].toByteArray())
                    }
                }
            }
        }
        step++
        if (step == playerID) {
            V.decrypt(Vlock1)
            V.encrypt(Vlock2)
            roles.encrypt(Vlock2)
            val perm = randomPermutation(N)
            V.shuffle(perm)
            roles.shuffle(perm)
            extraData.addAll(V.cards.map { x -> x.getEncoded(false) })
            extraData.addAll(roles.cards.map { x -> x.getEncoded(false) })
        }
    }

    /**
     * Encrypt IV and Roles with separate keys for each card
     *
     * @param responses - answers from other players
     */
    private fun processLocking(responses: List<GameMessageProto.GameStateMessage>){
        for (msg in responses) {
            val userID = playerOrder.indexOf(User(msg.user))
            if (userID == step) {
                if (playerID == step + 1  || step == N-1) {
                    if (msg.dataCount != 2 * deckSize) {
                        throw GameExecutionException(
                                "Someone failed to provide their deck")
                    }
                    for (i in 0..deckSize - 1) {
                        V.cards[i] = ECParams.curve.decodePoint(msg.dataList[i].toByteArray())
                    }
                    for (i in deckSize..2 * deckSize - 1) {
                        roles.cards[i-deckSize] = ECParams.curve.decodePoint(msg.dataList[i].toByteArray())
                    }
                }
            }
        }
        step++
        if (step == playerID) {
            V.decrypt(Vlock2)
            roles.decrypt(Vlock2)
            V.enctyptSeparate(VLockKeys)
            roles.enctyptSeparate(rolesLockKeys)
            extraData.addAll(V.cards.map { x -> x.getEncoded(false) })
            extraData.addAll(roles.cards.map { x -> x.getEncoded(false) })
        }
    }

    override fun getData(): List<ByteArray> {
        return extraData
    }

    override fun isFinished(): Boolean {
        return state == State.END
    }

    override fun getResult(): RoleDeck {
        return RoleDeck(originalRoles, roles, X, V, R, commonR, rolesLockKeys, VLockKeys, rolesLockKeyHashes, VLockKeyHashes)
    }
}

/**
 * @property originalRoles - common vector of generated roles
 * @property shuffledRoles - shuffled vector for roles
 * @property X - vector used for generating random secret
 * @property V - shuffled deck consisting of encrypted XR
 * @property ownR - own part of R
 * @property commonR - mutiplication of all Rs modula curve order
 * @property roleKeys - keys from roles deck
 * @property VKeys - keys form V deck
 * @property roleKeyHashes - hashes of other user Roles keys (sort of commitment)
 * @property VkeyHashes - hashes of other user V keys (sort of commitment)
 */
data class RoleDeck(val originalRoles: Deck, val shuffledRoles: Deck,
                    val X: List<BigInteger>, val V: Deck,
                    val ownR: Collection<BigInteger>, val commonR: Collection<BigInteger>,
                    val roleKeys: Collection<BigInteger>, val VKeys: Collection<BigInteger>,
                    val roleKeyHashes: MutableMap<User, String>, val VkeyHashes: MutableMap<User, String>)
