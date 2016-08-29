package apps.games.serious.mafia.subgames.role.secret

import apps.chat.Chat
import apps.games.Game
import apps.games.GameExecutionException
import apps.games.GameManager
import apps.games.GameManagerClass
import apps.games.primitives.Deck
import apps.games.serious.mafia.roles.DetectiveRole
import apps.games.serious.mafia.roles.PlayerRole
import apps.games.serious.mafia.roles.Role
import apps.games.serious.mafia.subgames.role.distribution.RoleDistributionHelper
import apps.games.serious.mafia.subgames.role.generation.RoleDeck
import apps.games.serious.mafia.subgames.role.generation.RoleGenerationVerifier
import crypto.RSA.RSAKeyManager
import crypto.random.randomBigInt
import crypto.random.randomPermutation
import crypto.random.shuffle
import crypto.random.split
import entity.Group
import entity.User
import org.apache.commons.codec.digest.DigestUtils
import org.bouncycastle.jce.spec.ECParameterSpec
import org.bouncycastle.math.ec.ECPoint
import proto.GameMessageProto
import java.math.BigInteger

/**
 * Created by user on 8/24/16.
 *
 * this class distributes secretes among all players based on their roles
 *
 * Also registers K for detective (as a size effect)
 */

class SecretSharingGame(chat: Chat, group: Group, gameID: String, val ECParams: ECParameterSpec,
                         val role: PlayerRole, val id: BigInteger, gameManager: GameManagerClass = GameManager) : Game<Pair<SecretDeck, SecretSharingVerifier>>(chat, group, gameID, gameManager = gameManager) {

    override val name: String
        get() = "Secret sharing game"

    val playerOrder = group.users.sortedBy { x -> x.name }.toMutableList()
    val playerID = playerOrder.indexOf(chat.me())
    var currentPlayer: Int = -1
    val N = group.users.size


    private val FLock = randomBigInt(ECParams.n)
    private val HLock = randomBigInt(ECParams.n)
    private val TLock = randomBigInt(ECParams.n)
    private val SKeys: List<BigInteger>
    private val SKeyHashes: MutableMap<User, String> = mutableMapOf()
    private val secrets = Deck(ECParams, N*N)
    private val ids = Deck(ECParams, N)
    private val extraData = mutableListOf<ByteArray>()
    private val Ks: List<BigInteger>
    val verifier = SecretSharingVerifier(group.users, secrets)

    init{
        SKeys = listOf(*Array(N*N, { i -> randomBigInt(ECParams.n) }))
        Ks = listOf(*Array(N, { i -> randomBigInt(ECParams.n) }))
    }
    private enum class State{
        INIT,
        VALIDATE_KEYS,
        EXCHANGE_SPLIT,
        UNIFY_KEYS,
        DETECTIVE_MAGIC,
        LOCK,
        EXCHANGE_KEYS,
        END
    }

    private var state: State = State.INIT

    override fun evaluate(responses: List<GameMessageProto.GameStateMessage>): String {
        for(msg in responses){
            chat.showMessage(msg.value)
        }
        extraData.clear()
        when(state){
            State.INIT -> {
                state = State.VALIDATE_KEYS
                return DigestUtils.sha256Hex(SKeys.joinToString(" "))
            }
            State.VALIDATE_KEYS -> {
                for (msg in responses) {
                    SKeyHashes[User(msg.user)] = msg.value
                }
                createSecret()
                state = State.EXCHANGE_SPLIT
            }
            State.EXCHANGE_SPLIT -> {
                for(msg in responses){
                    val user = User(msg.user)
                    val userID = playerOrder.indexOf(user)
                    for(i in 0..N-1){
                        secrets.cards[userID*N + i] = ECParams.curve.decodePoint(msg.dataList[i].toByteArray())
                    }
                    ids.cards[userID] = ECParams.curve.decodePoint(msg.dataList[N].toByteArray())
                }
                currentPlayer = -1
                state = State.UNIFY_KEYS
            }
            State.UNIFY_KEYS -> {
                processKeyUnification(responses)
                if(currentPlayer >= N){
                    currentPlayer = -1
                    state = State.DETECTIVE_MAGIC
                }
            }
            State.DETECTIVE_MAGIC -> {
                doDetectiveMagic(responses)
                if(currentPlayer >= N){
                    currentPlayer = -1
                    state = State.LOCK
                }
            }
            State.LOCK -> {
                processLocking(responses)
                if(currentPlayer >= N){
                    val keys = mutableListOf<BigInteger>()
                    for(i in 0..N*N-1){
                        if(i%N != playerID){
                            keys.add(SKeys[i])
                        }else{
                            keys.add(BigInteger.ZERO)
                        }
                    }
                    state = State.EXCHANGE_KEYS
                    return keys.joinToString(" ")
                }
            }
            State.EXCHANGE_KEYS -> {
                for(msg in responses){
                    val user = User(msg.user)
                    val userID = playerOrder.indexOf(user)
                    val split = msg.value.split(" ").map { x -> BigInteger(x) }

                    for(i in 0..N*N-1){
                        if(i%N != userID){
                            verifier.registerSKey(user, i, split[i])
                        }
                    }
                }
                for(i in 0..N-1){
                    verifier.registerSKey(chat.me(), i*N + playerID, SKeys[i*N + playerID])
                }
                state = State.END
            }
            State.END -> {}
        }
        return ""
    }

    /**
     * create a secret based on our role and id
     * and exchange it via extradata
     */
    private fun createSecret(){
        val secretValue = if (role.role == Role.MAFIA) id * BigInteger.valueOf(2) else id
        val secret: List<ECPoint> = crypto.random.split(secretValue, N).map { x -> ECParams.g.multiply(x * FLock) }
        extraData.addAll(secret.map { x -> x.getEncoded(false) })
        extraData.add(ECParams.g.multiply(id * FLock).getEncoded(false))
    }

    /**
     * Arter whole secret deck is formed - decrypt our part with
     * F key and entrypt all secrets with new key.
     *
     * after that step every secret part is encrypted with the same
     * combination of keys
     */
    private fun processKeyUnification(responses: List<GameMessageProto.GameStateMessage>){
        for(msg in responses){
            val userID = playerOrder.indexOf(User(msg.user))
            if (userID == currentPlayer) {
                if (playerID == currentPlayer + 1  || currentPlayer == N-1) {
                    if (msg.dataCount != N*(N + 1)) {
                        throw GameExecutionException(
                                "Someone failed to provide their deck")
                    }
                    for (i in 0..N*N - 1) {
                        secrets.cards[i] = ECParams.curve.decodePoint(msg.dataList[i].toByteArray())
                    }
                    for (i in N*N..N*(N+1)-1)
                        ids.cards[i-N*N] = ECParams.curve.decodePoint(msg.dataList[i].toByteArray())
                }
            }
        }
        currentPlayer ++
        if(playerID == currentPlayer){
            for(i in N*playerID..N*(playerID+1)-1){
                secrets.decryptCardWithKey(i, FLock)
            }
            ids.decryptCardWithKey(playerID, FLock)
            secrets.encrypt(HLock)
            ids.encrypt(HLock)
            extraData.addAll(secrets.cards.map { x -> x.getEncoded(false) })
            extraData.addAll(ids.cards.map { x -> x.getEncoded(false) })
        }
    }

    /**
     * Here is where magic happens: everyone except detective do exactly
     * one thing: decrypt with H key and encrypt with T key.
     *
     * Detective on the other hand - not onely encrypts with T key, but also
     * shuffle N-sized blocks. For other player there is no way to tell whether
     * shuffling took place or not
     */
    private fun doDetectiveMagic(responses: List<GameMessageProto.GameStateMessage>){
        for(msg in responses){
            val userID = playerOrder.indexOf(User(msg.user))
            if (userID == currentPlayer) {
                if (playerID == currentPlayer + 1  || currentPlayer == N-1) {
                    if (msg.dataCount != N*(N + 1)) {
                        throw GameExecutionException(
                                "Someone failed to provide their deck")
                    }
                    for (i in 0..N*N - 1) {
                        secrets.cards[i] = ECParams.curve.decodePoint(msg.dataList[i].toByteArray())
                    }
                    for (i in N*N..N*(N+1)-1)
                        ids.cards[i-N*N] = ECParams.curve.decodePoint(msg.dataList[i].toByteArray())
                }
            }
        }
        currentPlayer ++
        if(playerID == currentPlayer){
            secrets.decrypt(HLock)
            ids.decrypt(HLock)
            secrets.encrypt(TLock)
            ids.encrypt(TLock)
            if(role.role == Role.DETECTIVE){
                ids.encryptSeparate(Ks)
                for(i in 0..N-1){
                    (role as DetectiveRole).registerUserK(playerOrder[i], Ks[i])
                }
                val perm = randomPermutation(N)
                ids.shuffle(perm)
                secrets.shuffle(perm, N)
            }
            extraData.addAll(secrets.cards.map { x -> x.getEncoded(false) })
            extraData.addAll(ids.cards.map { x -> x.getEncoded(false) })
        }
    }

    /**
     * Decrypt all ids(now they are shuffled, so only decetive known wich is
     * wich). Encrypt all secret parts with separate keys
     */
    private fun processLocking(responses: List<GameMessageProto.GameStateMessage>){
        for(msg in responses){
            val userID = playerOrder.indexOf(User(msg.user))
            if (userID == currentPlayer) {
                if (playerID == currentPlayer + 1  || currentPlayer == N-1) {
                    if (msg.dataCount != N*(N + 1)) {
                        throw GameExecutionException(
                                "Someone failed to provide their deck")
                    }
                    for (i in 0..N*N - 1) {
                        secrets.cards[i] = ECParams.curve.decodePoint(msg.dataList[i].toByteArray())
                    }
                    for (i in N*N..N*(N+1)-1)
                        ids.cards[i-N*N] = ECParams.curve.decodePoint(msg.dataList[i].toByteArray())
                }
            }
        }
        currentPlayer ++
        if(playerID == currentPlayer){
            secrets.decrypt(TLock)
            ids.decrypt(TLock)
            secrets.encryptSeparate(SKeys)
            extraData.addAll(secrets.cards.map { x -> x.getEncoded(false) })
            extraData.addAll(ids.cards.map { x -> x.getEncoded(false) })
        }
    }

    override fun getData(): List<ByteArray> {
        return extraData
    }

    override fun isFinished(): Boolean {
        return state == State.END
    }

    override fun getResult(): Pair<SecretDeck, SecretSharingVerifier> {
        val res = Deck(ECParams, N)
        for(i in 0..N-1){
            if(!verifier.cardIsDecrypted(i*N + playerID)){
                throw GameExecutionException("Somehow secret part is not decrypted")
            }
            res.cards[i] = secrets.cards[i*N + playerID]
        }
        return SecretDeck(res, ids, SKeys, SKeyHashes) to verifier
    }

}