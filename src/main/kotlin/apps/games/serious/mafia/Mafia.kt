package apps.games.serious.mafia

import apps.chat.Chat
import apps.games.GameExecutionException
import apps.games.GameManager
import apps.games.GameManagerClass
import apps.games.serious.CardGame
import apps.games.serious.Cheat.GUI.CheatGame
import apps.games.serious.mafia.GUI.MafiaGame
import apps.games.serious.mafia.roles.*
import apps.games.serious.mafia.subgames.role.distribution.RoleDistributionGame
import apps.games.serious.mafia.subgames.role.distribution.RoleDistributionHelper
import apps.games.serious.mafia.subgames.role.generation.RoleDeck
import apps.games.serious.mafia.subgames.role.generation.RoleGenerationGame
import apps.games.serious.mafia.subgames.role.generation.RoleGenerationVerifier
import apps.games.serious.mafia.subgames.role.secret.SecretDeck
import apps.games.serious.mafia.subgames.role.secret.SecretSharingGame
import apps.games.serious.mafia.subgames.role.secret.SecretSharingVerifier
import apps.games.serious.mafia.subgames.sum.SMSfAResult
import apps.games.serious.mafia.subgames.sum.SecureMultipartySumForAnonymizationGame
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import crypto.RSA.ECParams
import crypto.RSA.RSAKeyManager
import crypto.random.randomBigInt
import crypto.random.randomString
import entity.Group
import entity.User
import org.apache.commons.codec.digest.DigestUtils
import org.bouncycastle.crypto.InvalidCipherTextException
import org.bouncycastle.math.ec.ECPoint
import proto.GameMessageProto
import java.math.BigInteger
import java.util.*
import java.util.concurrent.CancellationException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit


/**
 * Created by user on 8/9/16.
 */

class Mafia(chat: Chat, group: Group, gameID: String, gameManager: GameManagerClass = GameManager) : CardGame(chat, group, gameID, 52, gameManager) {
    override val name: String
        get() = "mafia game"

    private enum class State{
        INIT,
        VALIDATE_KEYS,
        INIT_ID,
        DAY_PHASE_PICK,
        DAY_PHASE_VERIFY,
        DAY_PHASE_REVEAL,
        DAY_PHASE_KILL,
        DOCTOR_I,
        DETECTIVE_CHANNEL,
        DETECTIVE_CHOICE,
        DETECTIVE_CHOICE_REVEAL,
        DETECTIVE_FINALIZE,
        MAFIA_COMMUNICATE,
        MAFIA_PICK,
        MAFIA_REVEAL,
        DOCTOR_II_REVEAL,
        DOCTOR_II_RESULT,
        NIGHT_RESULTS_REVEAL,
        NIGHT_RESULTS_KILL,
        VERIFY_BEGIN,
        VERYFY_END,
        END,
    }

    private lateinit var gameGUI: MafiaGame
    private lateinit var application: LwjglApplication
    private val logger = MafiaLogger()

    private var state: State = State.INIT
    private val keyManager = RSAKeyManager()
    private val HANDSHAKE_PHRASE = "HANDSHAKE" // who would've thought
    private val DEFAULT_MAFIA_MESSAGE = "Jesus, kill someone already"
    private val MAX_TEXT_LENGTH = 140
    private lateinit var role: PlayerRole
    private lateinit var roleDeck: RoleDeck
    private lateinit var secretDeck: SecretDeck
    private lateinit var roleGenerationVerifier: RoleGenerationVerifier
    private lateinit var secretSharingVerivier: SecretSharingVerifier
    private lateinit var roleDistributionHelper: RoleDistributionHelper
    private val ids: Array<BigInteger>
    private val alive = mutableSetOf(*group.users.toTypedArray())
    private val dead = mutableSetOf<User>()
    private val userRoles = mutableMapOf<User, Role>()

    private val N: Int
    private val M: Int //total mafia
    private var mafiaLeft: Int
    private lateinit var toKill: User


    private lateinit var detectiveExpSMS: SMSfAResult
    private lateinit var detectiveModSMS: SMSfAResult
    private lateinit var detextiveExp: BigInteger
    private lateinit var detextiveMod: BigInteger

    private val extraData = mutableListOf<ByteArray>()


    init {
        N = group.users.size
        ids = Array(N, {i -> BigInteger.ZERO})
        //if(N < 3) throw GameExecutionException("mafia is only viable with 4+ players")
        M = Math.sqrt(N * 1.0).toInt()
        for(user in playerOrder){
            userRoles[user] = Role.UNKNOWN
        }
        mafiaLeft = M
        initGame()
    }



    override fun evaluate(responses: List<GameMessageProto.GameStateMessage>): String {
        for (msg in responses) {
            chat.showMessage("[${msg.user.name}] said ${msg.value}")
        }
        extraData.clear()
        when(state){

            State.INIT -> {
                for (msg in responses) {
                    keyManager.registerUserPublicKey(User(msg.user), msg.value)
                }
                currentPlayerID = -1
                state = State.VALIDATE_KEYS
            }
            State.VALIDATE_KEYS -> {
                if (playerID == currentPlayerID) {
                    for (msg in responses) {
                        try {
                            val s = keyManager.decodeString(msg.value)
                            val handshake = s.split(" ").last()
                            if (handshake != HANDSHAKE_PHRASE) {
                                throw GameExecutionException("Invalid RSA key")
                            }
                        } catch (e: InvalidCipherTextException) {
                            throw GameExecutionException("Malformed RSA key")
                        }
                    }
                }

                currentPlayerID++
                if (currentPlayerID != N) {
                    val s = randomString(512) + " " + HANDSHAKE_PHRASE
                    return keyManager.encodeForUser(playerOrder[currentPlayerID], s)
                }else{
                    state = State.INIT_ID
                    chat.sendMessage("RSA is OK. Generating deck")
                    return randomBigInt(ECParams.n).toString()
                }
            }
            State.INIT_ID -> {
                for(msg in responses){
                    val user = User(msg.user)
                    val userID = getUserID(user)
                    ids[userID] = BigInteger(msg.value)
                }
                for(i in 0..N-1){
                    for(j in 0..N-1){
                        if((i != j && ids[i] == ids[j]) || ids[i] == BigInteger.valueOf(2) * ids[j]){
                            return randomBigInt(ECParams.n).toString()
                        }
                    }
                }
                initRoles()
                shareSecrets()
                state = State.DAY_PHASE_PICK
            }
            State.DAY_PHASE_PICK -> {
                state = State.DAY_PHASE_VERIFY
                if(chat.me() in alive){
                    val pick = pickUser()
                    return getUserID(pick).toString()
                }
            }
            State.DAY_PHASE_VERIFY -> {
                val voices = mutableMapOf<User, Int>()
                for(msg in responses){
                    val user = User(msg.user)
                    if(user in dead){
                        continue
                    }
                    toKill = playerOrder[msg.value.toInt()]
                    logger.registerDayVote(user, toKill)
                    if(toKill !in alive){
                        throw GameExecutionException("What is dead may never die")
                    }
                    if(toKill !in voices){
                        voices[toKill] = 0
                    }
                    voices[toKill] = voices[toKill]!! + 1
                }
                val deadUser = voices.maxBy { x -> x.value }?.key ?: throw GameExecutionException("Something wrong in this town")
                val draw = voices.filter { x -> x.value == voices[deadUser] }.size > 1
                if(draw){
                    state = State.DAY_PHASE_PICK
                    logger.resetDay()
                } else{
                    state = State.DAY_PHASE_REVEAL
                }
                return getUserID(deadUser).toString()
            }
            State.DAY_PHASE_REVEAL -> {
                val distinctResults = responses.distinctBy { x -> x.value }.size
                if(distinctResults > 1){
                    throw GameExecutionException("Something went wrong during day phase")
                }
                state = State.DAY_PHASE_KILL
                toKill = playerOrder[responses[0].value.toInt()]
                if(toKill == chat.me()){ // if we were killed
                    return roleDeck.roleKeys.elementAt(playerID).toString()
                }
            }
            State.DAY_PHASE_KILL -> {
                for(msg in responses){
                    val user = User(msg.user)
                    val userID = getUserID(user)
                    if(user == toKill){
                        if(toKill != chat.me()){
                            roleDistributionHelper.registerRoleKey(user, userID, BigInteger(msg.value))
                        }
                        val role = if(user == chat.me()) role.role else roleDistributionHelper.getRole(userID)
                        killUser(user, role)
                        break
                    }
                }
//                if(!gameEnded()){ // todo
//
//                }
                state = State.DOCTOR_I
                //state = State.DETECTIVE_CHANNEL
            }
            State.DOCTOR_I -> {
                if(dead.filter{x -> userRoles[x] == Role.DOCTOR}.isEmpty()){
                    processDoctorPick()
                }
                state = State.DETECTIVE_CHANNEL
                //state = State.DOCTOR_II_REVEAL
            }
            State.DETECTIVE_CHANNEL -> {
                if(dead.filter{x -> userRoles[x] == Role.DETECTIVE}.isNotEmpty()){
                    state = State.MAFIA_COMMUNICATE
                    return ""
                }
                processDetectiveRSA()
                state = State.DETECTIVE_CHOICE
                return "${detectiveModSMS.R} ${detectiveExpSMS.R}"
            }
            State.DETECTIVE_CHOICE -> {
                regiserDetectiveRSA(responses)
                processDetectivePick()
                state = State.DETECTIVE_CHOICE_REVEAL
                return logger.getDetectiveNoisedInput()
            }
            State.DETECTIVE_CHOICE_REVEAL -> {
                var noise = BigInteger.ZERO
                val hashes = mutableMapOf<User, String>()
                for(msg in responses){
                    val user = User(msg.user)
                    hashes[user] = DigestUtils.sha256Hex(msg.value)
                    val split = msg.value.split(" ")
                    noise += BigInteger(split[1])
                }
                if(!logger.verifyLastDetectiveRHashes(hashes)){
                    throw GameExecutionException("Someone cheated with his SMSfA value inputes")
                }
                val targetID = (logger.getDetectiveSum() - noise)
                state = State.DETECTIVE_FINALIZE
                return keyManager.encodeWithParams(detextiveMod, detextiveExp, secretDeck.getSecretForId(targetID).getEncoded(false))

            }
            State.DETECTIVE_FINALIZE -> {
                computeDetectiveChoiceResult(responses)
                state = State.MAFIA_COMMUNICATE
                //state = State.DOCTOR_II_REVEAL

            }
            State.MAFIA_COMMUNICATE -> {
                state = State.MAFIA_PICK
                return processMafiaCommunicationInput()
            }
            State.MAFIA_PICK -> {
                showMafiaMessages(responses)
                processMafiaPick()
                state = State.MAFIA_REVEAL
                return logger.getMafiaNoisedInput()
            }
            State.MAFIA_REVEAL -> {
                val consensus = computeMafiaChoiceConsensus(responses)
                if(ids.contains(consensus)){ //mafia came to consensus
                    if(role is MafiaRole && !(role as MafiaRole).verifyTarget(consensus)){
                        throw GameExecutionException("Someone probably tried to interfere with mafia picks")
                    }
                    logger.registerNightPlay(Role.MAFIA, playerOrder[ids.indexOf(consensus)])
                    state = State.DOCTOR_II_REVEAL
                }else{
                    state = State.MAFIA_COMMUNICATE
                }
            }
            State.DOCTOR_II_REVEAL -> {
                if(dead.filter{x -> userRoles[x] == Role.DOCTOR}.isEmpty()){
                    state = State.DOCTOR_II_RESULT
                    return logger.getDoctorNoisedInput()
                }else{
                    state = State.NIGHT_RESULTS_REVEAL
                }

            }
            State.DOCTOR_II_RESULT -> {
                computeDotcorChoice(responses)
                state = State.NIGHT_RESULTS_REVEAL
            }
            State.NIGHT_RESULTS_REVEAL -> {
                val died = logger.getLastMafiaTarget() ?: throw GameExecutionException("Somthing wrong with mafia pick")
                val survived = logger.getLastDoctorTarget()
                if(died == survived){
                    state = State.DAY_PHASE_PICK
                    return ""
                }
                toKill = died
                state = State.NIGHT_RESULTS_KILL
                if(died == chat.me()){ // if we were killed
                    return roleDeck.roleKeys.elementAt(playerID).toString()
                }
            }
            State.NIGHT_RESULTS_KILL -> {
                for(msg in responses){
                    val user = User(msg.user)
                    val userID = getUserID(user)
                    if(user == toKill){
                        if(toKill != chat.me()){
                            roleDistributionHelper.registerRoleKey(user, userID, BigInteger(msg.value))
                        }
                        val role = if(user == chat.me()) role.role else roleDistributionHelper.getRole(userID)
                        killUser(user, role)
                        break
                    }
                }
                if(gameEnded()){
                    state = State.VERIFY_BEGIN
                }else{
                    state = State.DAY_PHASE_PICK
                }
            }
            State.VERIFY_BEGIN -> {
                revealExtraData()
                state = State.VERYFY_END
                return keyManager.getPrivateKey()
            }
            State.VERYFY_END -> {
                verify(responses)
                state = State.END
            }
            State.END -> {}
        }
        return ""
    }

    /**
     * Start GUI for the Cheat game
     */
    private fun initGame(): String {
        Role.reset()
        val config = LwjglApplicationConfiguration()
        config.width = 1024
        config.height = 1024
        config.forceExit = false
        config.title = "Cheat Game[${chat.username}]"

        gameGUI = MafiaGame(group, logger, MAX_TEXT_LENGTH)
        application = LwjglApplication(gameGUI, config)
        while (!gameGUI.loaded) {
            Thread.sleep(200)
        }
        return ""
    }

    /**
     * process role generation and distribution.
     * after this step [role] holds current player role
     * with the list of known comrades
     */
    private fun initRoles(){
        val rolesCount = mutableMapOf<Role, Int>()
        for(role in Role.values()){
            when(role){
                Role.MAFIA -> rolesCount[role] = M
                else -> if(role != Role.INNOCENT && role != Role.UNKNOWN) rolesCount[role] = 1
            }
        }
        rolesCount[Role.INNOCENT] = N - rolesCount.values.sum()
        val roleGenerationGame = RoleGenerationGame(chat, group, subGameID(), ECParams, rolesCount, gameManager)
        try {
            val roleGenerationFuture = runSubGame(roleGenerationGame)
            roleGenerationVerifier = roleGenerationFuture.get().second
            roleDeck = roleGenerationFuture.get().first
            val roleFuture = runSubGame(RoleDistributionGame(chat, group, subGameID(), ECParams, roleDeck, gameManager))
            role = roleFuture.get().first
            roleDistributionHelper = roleFuture.get().second
        }catch (e: CancellationException){
            return
        }catch (e: Exception){
            throw GameExecutionException(e.message ?: "Something went wrong in role generation/distribution")
        }
        for(user in role.getComrades()){
            logger.registerUserRole(user, role.role)
        }
        gameGUI.setRole(role.role)
        for(i in 0..N-1){
            if(playerOrder[i] in role.getComrades()){
                gameGUI.dealPlayer(getTablePlayerId(i), role.role, logger.getUserRolePosition(playerOrder[i]))
                userRoles[playerOrder[i]] = role.role
            }else{
                gameGUI.dealPlayer(getTablePlayerId(i), Role.UNKNOWN)
            }
        }

    }

    /**
     * create share secrets via SecretSharing game
     */
    private fun shareSecrets(){
        try {
            val secretFuture = runSubGame(SecretSharingGame(chat, group, subGameID(), ECParams, role, ids[playerID], gameManager))
            secretDeck = secretFuture.get().first
            secretSharingVerivier = secretFuture.get().second
        } catch (e: CancellationException){
            return
        } catch (e: Exception){
            throw GameExecutionException(e.message ?: "Something went wrong in secret sharing")
        }
    }

    /**
     * Let users pick his target
     */
    private fun pickUser(seconds: Long = Long.MAX_VALUE): User {
        val userPickQueue = LinkedBlockingQueue<User>(1)
        val callback = { x: User -> userPickQueue.offer(x) }
        gameGUI.resetAllUserPicks()
        gameGUI.registerUserPickCallback(callback, playerOrder)
        gameGUI.disableUserPicks(dead)
        gameGUI.showUserPickOverlay()
        gameGUI.showHint("Pick user to kill (DAY PHASE)")
        val res = userPickQueue.poll(seconds, TimeUnit.SECONDS) ?: chat.me()
        //val res = userPickQueue.poll(seconds, TimeUnit.SECONDS)
        gameGUI.hideUserPickOverlay()
        return res
    }

    /**
     * get message for other members of mafia from GUI
     */
    private fun getMafiaInput(seconds: Long): String{
        val msgQueue = LinkedBlockingQueue<String>(1)
        val callback = { x: String -> msgQueue.offer(x) }
        gameGUI.resetAllUserPicks()
        gameGUI.registerMafiaEOICallback(callback)
        gameGUI.showMafiaInputOverlay()
        gameGUI.showHint("Just type message for other mafia members")
        val res = msgQueue.poll(seconds, TimeUnit.SECONDS) ?: DEFAULT_MAFIA_MESSAGE
        //val res = userPickQueue.poll(seconds, TimeUnit.SECONDS)
        gameGUI.hideMafiaInputOverlay()
        return res
    }

    /**
     * given user and his role - register him as dead
     */
    private fun killUser(user: User, role: Role){
        userRoles[user] = role
        alive.remove(user)
        dead.add(user)
        chat.showMessage("Killed [${user.name}] whose role was [${role.name}]")
        if(role == Role.MAFIA){
            mafiaLeft --
        }
        if(role != this.role.role){
            gameGUI.revealPlayerRole(getTablePlayerId(getUserID(user)), role, logger.getUserRolePosition(user))
        }
        gameGUI.animateRolePlay(role, logger.getUserRolePosition(user))
    }

    /**
     * Run first phase of doctor actions:
     * doctor picks his target via SMSfA
     */
    private fun processDoctorPick(){
        val deadline = Calendar.getInstance()
        deadline.add(Calendar.SECOND, DoctorRole.TIMEOUT.toInt())
        val targetID: BigInteger
        if(role is DoctorRole){
            val user = pickUser(DoctorRole.TIMEOUT)
            targetID = ids[getUserID(user)]
            (role as DoctorRole).registerTarget(targetID)
        }else{
            targetID = BigInteger.ZERO
        }
        val currentTime = Calendar.getInstance()
        if(currentTime < deadline){
            Thread.sleep(deadline.timeInMillis - currentTime.timeInMillis)
        }
        val targetFuture = gameManager.initSubGame(SecureMultipartySumForAnonymizationGame(chat, group, subGameID(), keyManager, targetID, ECParams.n, gameManager))
        logger.registerDoctorFirstPhase(targetFuture.get())
    }


    /**
     * Run detective choice phase:
     * detective picks his target and
     * broadcasts encoded id via SMSfA
     */
    private fun processDetectivePick(){
        val deadline = Calendar.getInstance()
        deadline.add(Calendar.SECOND, DetectiveRole.TIMEOUT.toInt())
        val encryptedTargetId: BigInteger
        if(role is DetectiveRole){
            val user = pickUser(DetectiveRole.TIMEOUT)
            val userID = getUserID(user)
            encryptedTargetId = ids[userID] * (role as DetectiveRole).getUserK(user)
            (role as DetectiveRole).registerTarget(user, ids[userID])
        }else{
            encryptedTargetId = BigInteger.ZERO
        }
        val currentTime = Calendar.getInstance()
        if(currentTime < deadline){
            Thread.sleep(deadline.timeInMillis - currentTime.timeInMillis)
        }
        val targetFuture = gameManager.initSubGame(SecureMultipartySumForAnonymizationGame(chat, group, subGameID(), keyManager, encryptedTargetId, ECParams.n, gameManager))
        logger.registerDetectiveChoiceSMS(targetFuture.get())
    }

    /**
     * get encoded input for madia communicatoin phase
     * (if we are not mafia - provide random string
     * as an input)
     *
     * @return get mafia input encoded for commrades
     */
    private fun processMafiaCommunicationInput(): String{
        val deadline = Calendar.getInstance()
        deadline.add(Calendar.SECOND, MafiaRole.MESSAGE_INPUT_TIMEOUT.toInt())
        val msg: String
        if(role is MafiaRole && !dead.contains(chat.me())){
            msg = getMafiaInput(MafiaRole.MESSAGE_INPUT_TIMEOUT).padEnd(MAX_TEXT_LENGTH)
        }else{
            msg = randomString(MAX_TEXT_LENGTH)
        }
        val currentTime = Calendar.getInstance()
        if(currentTime < deadline){
            Thread.sleep(deadline.timeInMillis - currentTime.timeInMillis)
        }
        return role.encryptForComrades(msg)
    }

    /**
     * exchange detective RSA public paramenters via SMSfA
     */
    private fun processDetectiveRSA(){
        val mod: BigInteger
        val exp: BigInteger


        if(role is DetectiveRole){
            mod = (role as DetectiveRole).getModulus()
            exp = (role as DetectiveRole).getPublicExponent()
        }else{
            mod = BigInteger.ZERO
            exp = BigInteger.ZERO
        }
        val maxValue = BigInteger.valueOf(2).pow(DetectiveRole.KEY_LENGTH)

        //No checks for this stage is required at this point: if something is wrong -detective decryption will fail
        //so we can only store data about sum
        val modFuture = gameManager.initSubGame(SecureMultipartySumForAnonymizationGame(chat, group, subGameID(), keyManager, mod, maxValue, gameManager))
        detectiveModSMS = modFuture.get()
        val expFuture = gameManager.initSubGame(SecureMultipartySumForAnonymizationGame(chat, group, subGameID(), keyManager, exp, maxValue, gameManager))
        detectiveExpSMS = expFuture.get()
    }

    /**
     * compute and store RSA parameters
     */
    private fun regiserDetectiveRSA(responses: List<GameMessageProto.GameStateMessage>){
        var modSum = BigInteger.ZERO
        var expSum = BigInteger.ZERO
        for(msg in responses){
            val split = msg.value.split(" ").map { x -> BigInteger(x) }
            modSum += split[0]
            expSum += split[1]
        }
        detextiveExp = detectiveExpSMS.sum - expSum
        detextiveMod = detectiveModSMS.sum - modSum
    }

    /**
     * Compute check result for detective based on preveous check results
     */
    private fun computeDetectiveChoiceResult(responses: List<GameMessageProto.GameStateMessage>){
        if(role !is DetectiveRole){
            return
        }
        var sum: ECPoint = ECParams.curve.infinity
        for(msg in responses){
            val part = (role as DetectiveRole).decodeSecretPart(msg.value)
            sum  = sum.add(part)
        }
        for(i in 0..N-1){
            if(ECParams.g.multiply(ids[i]) == sum){
                (role as DetectiveRole).registerTargetResult(ids[i], false)
                logger.registerDetectivePlay(playerOrder[i], false)
            }
            if(ECParams.g.multiply(ids[i] * BigInteger.valueOf(2)) == sum){
                (role as DetectiveRole).registerTargetResult(ids[i], true)
                logger.registerDetectivePlay(playerOrder[i], true)
            }
        }
    }

    /**
     * after R exchange we can compute, who was healed by the doctor
     */
    private fun computeDotcorChoice(responses: List<GameMessageProto.GameStateMessage>){
        val hashes = mutableMapOf<User, String>()
        var RSum = BigInteger.ZERO
        for(msg in responses){
            val user = User(msg.user)
            hashes[user] = DigestUtils.sha256Hex(msg.value)
            RSum += BigInteger(msg.value.split(" ")[1])
        }
        logger.verifyLastDoctorRHashes(hashes)
        val target = logger.getDoctorSum() - RSum
        val targetID = ids.indexOf(target)
        logger.registerNightPlay(Role.DOCTOR, playerOrder[targetID])
    }

    /**
     * assuming we got a bunch of messages encoded for commrades:
     * decrypt the and show to user
     */
    private fun showMafiaMessages(responses: List<GameMessageProto.GameStateMessage>){
        if(role is MafiaRole){
            val messages = mutableMapOf<User, String>()
            for(msg in responses){
                val user = User(msg.user)
                if(role.getComrades().contains(user)){
                    messages[user] = role.decryptForComrades(msg.value)
                }
            }
            gameGUI.registerMafiaMessages(messages)
            gameGUI.showMafiaMessagesOverlay()
        }
    }

    /**
     * getinput form mafia and run SMSfA subgame
     * store result in [mafiaSMS] for further use
     */
    private fun processMafiaPick(){
        val deadline = Calendar.getInstance()
        deadline.add(Calendar.SECOND, MafiaRole.TARGET_CHOICE_TIMEOUT.toInt())
        val targetId: BigInteger
        if(role is MafiaRole && !dead.contains(chat.me())){
            val user = pickUser(MafiaRole.TARGET_CHOICE_TIMEOUT)
            val userID = getUserID(user)
            targetId = ids[userID]
            (role as MafiaRole).registerTarget(targetId)
        }else{
            targetId = BigInteger.ZERO
        }
        val currentTime = Calendar.getInstance()
        if(currentTime < deadline){
            Thread.sleep(deadline.timeInMillis - currentTime.timeInMillis)
        }
        gameGUI.hideMafiaMessagesOverlay()
        val targetFuture = gameManager.initSubGame(SecureMultipartySumForAnonymizationGame(chat, group, subGameID(), keyManager, targetId, ECParams.n, gameManager))
        logger.registerMafiaChoiceSMS(targetFuture.get())
    }

    /**
     * compute average vote of all mafias.
     */
    private fun computeMafiaChoiceConsensus(responses: List<GameMessageProto.GameStateMessage>): BigInteger{
        val hashes = mutableMapOf<User, String>()
        var RSum = BigInteger.ZERO
        for(msg in responses){
            val user = User(msg.user)
            hashes[user] = DigestUtils.sha256Hex(msg.value)
            RSum += BigInteger(msg.value.split(" ")[1])
        }
        logger.verifyLastMafiaRHashes(hashes)
        return (logger.getMafiaSum() - RSum) / BigInteger.valueOf(mafiaLeft.toLong())
    }

    /**
     * reveal all data, that was privately stored
     * in secret generation and role generation/distributoin
     * with following order:
     *
     * [roleKeys]
     * [VKeys]
     * [RKeys]
     * [X]
     * [SKeys]
     */
    private fun revealExtraData(){
        extraData.addAll(roleDeck.roleKeys.map { x -> x.toByteArray() })
        extraData.addAll(roleDeck.VKeys.map { x -> x.toByteArray() })
        extraData.addAll(roleDeck.Rkeys.map { x -> x.toByteArray() })
        extraData.addAll(roleDeck.X.map { x -> x.toByteArray() })
        extraData.addAll(secretDeck.SKeys.map { x -> x.toByteArray() })
    }

    /**
     * run verification on all data, that was
     * tranmited in [revealExtraData]
     */
    private fun verify(responses: List<GameMessageProto.GameStateMessage>){
        val roleKeys = mutableMapOf<User, List<BigInteger>>()
        val VKeys = mutableMapOf<User, List<BigInteger>>()
        val RKeys = mutableMapOf<User, List<BigInteger>>()
        val Xs = mutableMapOf<User, List<BigInteger>>()
        val SKeys = mutableMapOf<User, String>()
        val roles = mutableMapOf<User, Role>()
        for(msg in responses){
            val user = User(msg.user)
            keyManager.registerUserPrivateKey(user, msg.value)
            val s = keyManager.decodeForUser(user, keyManager.encodeForUser(user, HANDSHAKE_PHRASE))
            if (s != HANDSHAKE_PHRASE) {
                throw GameExecutionException("[${user.name}] provided incorrect private key")
            }
            val roleDeckLen = roleDeck.Rkeys.size
            val secretDeckLen = secretDeck.SKeys.size
            roleKeys[user] = msg.dataList.slice(0..roleDeckLen-1).map { x -> BigInteger(x.toByteArray()) }
            VKeys[user] = msg.dataList.slice(roleDeckLen..2*roleDeckLen-1).map { x -> BigInteger(x.toByteArray()) }
            RKeys[user] = msg.dataList.slice(2*roleDeckLen..3*roleDeckLen-1).map { x -> BigInteger(x.toByteArray()) }
            Xs[user] = msg.dataList.slice(3*roleDeckLen..4*roleDeckLen-1).map { x -> BigInteger(x.toByteArray()) }
            val ss = msg.dataList.slice(4*roleDeckLen..4*roleDeckLen + secretDeckLen -1).map { x -> BigInteger(x.toByteArray()) }
            for(i in 0..secretDeckLen-1){
                secretSharingVerivier.registerSKey(user, i, ss[i])
            }
            for(i in 0..roleDeckLen-1){
                roleDistributionHelper.registerRoleKey(user, i, roleKeys[user]!![i])
            }
            roles[user] = roleDistributionHelper.getRole(getUserID(user))
            SKeys[user] = DigestUtils.sha256Hex(ss.joinToString(" "))
        }
        if(!roleGenerationVerifier.verify(roleKeys, VKeys, RKeys, Xs)){
            throw GameExecutionException("Someone cheated at role Generatoin phase")
        }
        if(!secretSharingVerivier.verifySKeys(SKeys)){
            throw GameExecutionException("Someone cheated at Secret sharing part")
        }
        if(!logger.verify(keyManager)){
            throw GameExecutionException("Someone cheated in SMSfA")
        }
        if(role is DetectiveRole && !(role as DetectiveRole).verifyChecks(roles)){
            throw GameExecutionException("Someone lied about their role")
        }
    }

    /**
     * check, whether game has ended:
     * either all mafia players are dead
     * or at least half of players, that are
     * still alive are mafia players
     *
     * @return whether game has ended
     */
    private fun gameEnded(): Boolean{
        return mafiaLeft == 0 || (alive.size <= 2*mafiaLeft)
    }

    override fun getInitialMessage(): String {
        return keyManager.getPublicKey()
    }

    override fun getFinalMessage(): String {
        return if (mafiaLeft == 0) "I agree that CITIZENS won" else "I Agree that MAFIA won"
    }

    override fun getResult() {
    }

    override fun isFinished(): Boolean {
        return  state == State.END
    }
    override fun getData(): List<ByteArray> {
        return extraData
    }

    override fun close() {
        application.stop()
    }
}