package apps.games.serious.Cheat

import apps.chat.Chat
import apps.games.GameExecutionException
import apps.games.serious.Card
import apps.games.serious.CardGame
import apps.games.serious.Cheat.GUI.CheatGame
import apps.games.serious.Cheat.logger.CheatGameLogger
import apps.games.serious.Pip
import apps.games.serious.getCardById
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import crypto.RSA.RSAKeyManager
import crypto.random.randomString
import entity.Group
import entity.User
import org.apache.commons.codec.digest.DigestUtils
import org.bouncycastle.crypto.InvalidCipherTextException
import proto.GameMessageProto
import java.math.BigInteger
import java.util.concurrent.LinkedBlockingQueue

/**
 * Created by user on 7/28/16.
 */

class Cheat(chat: Chat, group: Group, gameID: String) :
        CardGame(chat, group, gameID, 36) {
    override val name: String
        get() = "Cheat Game "

    private enum class State {
        INIT,
        VALIDATE_KEYS,
        PICK_DECK_SIZE,
        GENERATE_DECK,
        DECRYPT_HAND,
        INIT_ROUND,
        REVEAL_CARD,
        VERIFY_CARD,
        PASS_KEYS,
        COMPARE_RESULTS,
        REVEAL_ALL_KEYS,
        VERIFY_LOGS,
        END,
    }

    private var state: State = State.INIT

    private lateinit var gameGUI: CheatGame
    private lateinit var application: LwjglApplication

    private val keyManager = RSAKeyManager()
    private val HANDSHAKE_PHRASE = "HANDSHAKE" // who would've thought
    private val WINNING_PHRASE = "I DECLARE, WITH UTTER CERTAINTY, THAT THIS ONE IS IN THE BAG!"
    private val LOSING_PHRASE = "I LOST"
    private val N = group.users.size
    private val SALT_SIZE = 128
    private val cardHolders: MutableMap<Int, Int> = mutableMapOf()
    private val playerCards = mutableSetOf<Card>()
    private val playQueue = mutableListOf<List<String>>()
    private val extraData = mutableListOf<ByteArray>()
    private var cardToReveal: Int = -1
    private var receiverPlayerID: Int = -1
    private val cardsCount = IntArray(N)
    private val winners = BooleanArray(N)

    val logger = CheatGameLogger(N)

    init {
        initGame()
    }

    override fun evaluate(responses: List<GameMessageProto.GameStateMessage>): String {
        for (msg in responses) {
            chat.showMessage("[${msg.user.name}] said ${msg.value}")
        }
        extraData.clear()
        when (state) {

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
                if (currentPlayerID == N) {
                    state = State.PICK_DECK_SIZE
                    chat.sendMessage("RSA is OK. Generating deck")
                    currentPlayerID = -1
                    return ""

                }
                val s = randomString(512) + " " + HANDSHAKE_PHRASE
                return keyManager.encodeForUser(playerOrder[currentPlayerID], s)
            }
            State.PICK_DECK_SIZE -> {
                val vote = pickDeckSize()
                state = State.GENERATE_DECK
                return vote.toString()
            }
            State.GENERATE_DECK -> {
                for (msg in responses) {
                    chat.showMessage("[${msg.user.name}] voted for ${msg.value} cardID deck")
                }
                val votes = responses.map { x -> x.value.toInt() }
                deckSize = votes.maxBy { x -> votes.count { s -> s == x } } ?:
                        throw GameExecutionException("Couldn't agree in deck size")
                gameGUI.updateDeckSize(deckSize)
                chat.sendMessage("We are playing $deckSize cardID deck")
                state = State.DECRYPT_HAND
                return initiateHands()
            }
            State.DECRYPT_HAND -> {
                decryptHand(responses)
                dealHand()
                state = State.INIT_ROUND
                currentPlayerID = -1
            }
            State.INIT_ROUND -> {
                val action = processGameResponses(responses)
                if (action == Choice.ADD) {
                    return makeMove()
                } else {
                    state = State.REVEAL_CARD
                }

            }
            State.REVEAL_CARD -> {
                state = State.VERIFY_CARD
                if (playerID == currentPlayerID) {
                    return playQueue.last()[cardToReveal]
                }
            }
            State.VERIFY_CARD -> {
                revealCard(responses)
                state = State.PASS_KEYS
                currentPlayerID = -1
            }
            State.PASS_KEYS -> {
                processEncryptedMessages(responses)
                if (logger.log.stackFinished()) {
                    currentPlayerID = getNextPlayer(receiverPlayerID)
                    receiverPlayerID = -1
                    if (playQueue.isNotEmpty()) {
                        throw GameExecutionException("Not all cards were revealed")
                    }
                    updateWinners()
                    logger.log.nextStack()
                    val m = winners.count { x -> x }
                    if (winners[playerID]) {
                        state = State.COMPARE_RESULTS
                        return WINNING_PHRASE
                    } else if (m != 0) {
                        state = State.COMPARE_RESULTS
                        return LOSING_PHRASE
                    } else {
                        state = State.INIT_ROUND
                    }

                    return ""
                }
                currentPlayerID = getUserID(logger.log.nextPlayerToReveal())
                if (playerID == currentPlayerID) {
                    extraData.clear()
                    val enc = playQueue.removeAt(0)
                    for (s in enc) {
                        extraData.add(keyManager.encodeForUser(playerOrder[receiverPlayerID], s).toByteArray())
                    }
                }
            }
            State.COMPARE_RESULTS -> {
                compareWinningMessages(responses)
                state = State.REVEAL_ALL_KEYS
                return keyManager.getPrivateKey()
            }
            State.REVEAL_ALL_KEYS -> {
                for (msg in responses) {
                    val user = User(msg.user)
                    keyManager.registerUserPrivateKey(user, msg.value)
                    val s = keyManager.decodeForUser(user, keyManager.encodeForUser(user, HANDSHAKE_PHRASE))
                    if (s != HANDSHAKE_PHRASE) {
                        throw GameExecutionException("[${user.name}] provided incorrect private key")
                    }
                }
                state = State.VERIFY_LOGS
                return deck.encrypted.keys.joinToString(" ")
            }
            State.VERIFY_LOGS -> {
                for (msg in responses) {
                    val user = User(msg.user)
                    val userId = getUserID(user)
                    val keys = msg.value.split(" ").map { x -> BigInteger(x) }
                    for (i in 0..deckSize - 1) {
                        logger.log.registerCardKey(userId, i, keys[i])
                    }
                }
                val f = logger.log.verifyRound(playerOrder,
                        { user: User, msg: String -> keyManager.decodeForUser(user, msg) },
                        winners.zip(playerOrder).map { x -> x.second to x.first }.toMap())
                if (!f) {
                    throw GameExecutionException("Someone cheated. After decrypting RSA something is not in place")
                }
                keyManager.reset()
                state = State.END
                Thread.sleep(2000)
            }
            State.END -> TODO()
        }
        return ""
    }

    /**
     * Start GUI for the Cheat game
     */
    private fun initGame(): String {
        val config = LwjglApplicationConfiguration()
        config.width = 1024
        config.height = 1024
        config.forceExit = false
        config.title = "Cheat Game[${chat.username}]"
        gameGUI = CheatGame(playerID, N = N)
        application = LwjglApplication(gameGUI, config)
        while (!gameGUI.loaded) {
            Thread.sleep(200)
        }
        for(i in 0..N-1){
            gameGUI.updatePlayerName(getTablePlayerId(i), playerOrder[i].name)
        }
        return ""
    }


    /**
     * Let users pick desired deckSize
     */
    private fun pickDeckSize(): Int {
        val deckSizeQueue = LinkedBlockingQueue<DeckSizes>(1)
        val callback = { x: DeckSizes -> deckSizeQueue.offer(x) }
        gameGUI.resetAllSizes()
        gameGUI.registerDeckSizeCallback(callback, *DeckSizes.values())
        gameGUI.showDecksizeOverlay()
        gameGUI.showHint("Pick desired deck size")
        val x = deckSizeQueue.take()
        gameGUI.hideDecksizeOverlay()
        return x.size
    }

    /**
     * Create new deck. Broadcast key for
     * cardID holders
     */
    private fun initiateHands(): String {
        gameGUI.showHint("Shuffling Cards")
        updateDeck()
        logger.newRound(deckSize, deck)
        val resultKeys = mutableListOf<BigInteger>()
        for (i in 0..deckSize - 1) {
            val holder = i % N
            cardHolders[i] = holder
            cardsCount[holder]++
            if (holder != playerID) {
                resultKeys.add(deck.encrypted.keys[i])
            }
        }
        return resultKeys.joinToString(" ")
    }

    /**
     * Take a list of keys sent by user,
     * keys should correspond to cards, that are not
     * by that user. I.E. If playerId holds cards
     * 1, 4, 7 in shuffled deck, keys - contains
     * key for every cardID, that is not in TALON,
     * and are not possessed by that user
     * Stores known cards in
     */
    private fun decryptHand(responses: List<GameMessageProto.GameStateMessage>) {
        for (i in 0..deckSize - 1) {
            logger.log.registerCardKey(playerID, i, deck.encrypted.keys[i])
        }
        for (msg in responses) {
            val keys = msg.value.split(" ").map { x -> BigInteger(x) }
            val userID = getUserID(User(msg.user))
            val positions = cardHolders.filterValues { x -> x != userID }.keys.toList()
            if (keys.size != positions.size) {
                throw GameExecutionException("Someone failed to provide his keys")
            }
            for (i in 0..keys.size - 1) {
                val pos = logger.log.registerCardKey(userID, positions[i], keys[i])
                if (pos != -1 && cardHolders[positions[i]] == playerID) {
                    playerCards.add(getCardById(pos, deckSize))
                }
            }
        }
    }

    /**
     * Deal decrypted cards to our self and unknown cards
     * to everyone else
     */
    private fun dealHand() {
        for (card in playerCards) {
            gameGUI.dealPlayer(getTablePlayerId(playerID), card)
        }
        for (i in 0..deckSize - 1) {
            if (cardHolders[i] != playerID) {
                gameGUI.dealPlayer(getTablePlayerId(cardHolders[i]!!), -1)
            }
        }
    }

    /**
     * Process messages receive from last turn (in game phase)
     * update logger, choose next playerId to make his move
     *
     * @return playerId action(Choice)
     */
    private fun processGameResponses(responses: List<GameMessageProto.GameStateMessage>): Choice {
        for (msg in responses) {
            val user = User(msg.user)
            val userID = getUserID(user)
            if (userID == currentPlayerID) {
                if (logger.log.isNewStack() && msg.value.isEmpty()) {
                    continue
                }
                val move = msg.value.split(" ")
                val action: Choice = Choice.valueOf(move[0])
                val prevPlayer = getPreviousPlayer(currentPlayerID)

                when (action) {
                    Choice.ADD -> {
                        processAddCardHashes(msg)
                        currentPlayerID = getNextPlayer()
                    }
                    Choice.CHECK_TRUE -> {
                        cardToReveal = move[1].toInt()
                        logger.log.registerVerify(playerOrder[prevPlayer], cardToReveal, false)
                        currentPlayerID = prevPlayer
                    }
                    Choice.CHECK_FALSE -> {
                        cardToReveal = move[1].toInt()
                        logger.log.registerVerify(playerOrder[prevPlayer], cardToReveal, true)
                        currentPlayerID = prevPlayer
                    }
                }
                return action
            }
        }
        return Choice.ADD
    }

    /**
     * if previous playerId decided to add cards - process his request
     * @param msg - GameStateMessage describing added cards
     */
    private fun processAddCardHashes(msg: GameMessageProto.GameStateMessage) {
        val move = msg.value.split(" ")
        val user = User(msg.user)
        val userID = getUserID(user)
        if (move.size < 2 || move.size > 4) {
            throw GameExecutionException("Unexpectex move: ${msg.value}")
        }
        val claim = if (move.size == 2) Pip.UNKNOWN else Pip.valueOf(move[2])
        if (claim != Pip.UNKNOWN) {
            gameGUI.showHint("All of these cards are ${claim.type}")
        }
        val count = BetCount.valueOf(move[1])
        if (count.size > cardsCount[userID]) {
            throw GameExecutionException("According to my logs, ${user.name} " +
                    "doesn't have ${count.size} cards")
        }
        val hashes = msg.dataList.map { x -> String(x.toByteArray()) }
        logger.log.registerAddPlayHashes(user, count, claim, hashes)
        if (playerID != currentPlayerID) {
            for (i in 0..count.size - 1) {
                gameGUI.animateCardPlay(getTablePlayerId(userID))
            }
        }

    }


    /**
     * If we are currentPlayer - make our move
     * Pick our claim(add card/belive/check for cheat)
     * if necessary - pick cards and return their hashes
     * otherwise - update hint
     */
    private fun makeMove(): String {
        if (currentPlayerID == -1) {
            currentPlayerID++
        }
        if (playerID == currentPlayerID) {
            gameGUI.showHint("Decide what will you do:")
            val choice = getPlayerChoise()
            if (choice == Choice.ADD) {
                val count = getAddCount()
                val pip = getPip()
                val pickedCards = mutableListOf<Int>()
                for (i in 0..count.size - 1) {
                    val card = gameGUI.pickCard(*playerCards.toTypedArray())
                    playerCards.remove(getCardById(card, deckSize))
                    pickedCards.add(card)
                }

                val stored = pickedCards
                        .map { x -> deck.encrypted.deck.cards.indexOf(deck.originalDeck.cards[x]) }
                        .map { x -> "$x ${deck.encrypted.keys[x]} ${randomString(SALT_SIZE)}" }
                playQueue.add(stored)
                extraData.clear()
                for (cardEntry in stored) {
                    extraData.add(DigestUtils.sha256Hex(cardEntry.toByteArray()).toByteArray())
                }
                return "${choice.name} ${count.type} ${pip.type}"
            } else {
                val prevPlayer = getPreviousPlayer()
                val totPlayed = logger.log.countUserCardOnStack(playerOrder[prevPlayer])
                val lastPlayed = logger.log.getLastUserBetCountSize(playerOrder[prevPlayer])
                val filter = { x: Int -> (lastPlayed == -1) || (totPlayed - x <= lastPlayed) }
                val cardToVerify = gameGUI.pickPlayedCard(getTablePlayerId(prevPlayer), filter)
                val res = cardToVerify - totPlayed + lastPlayed
                return "${choice.name} $res"
            }
        } else {
            gameGUI.showHint("Waiting for " +
                    "[${playerOrder[currentPlayerID].name}]" +
                    "to make his move")
        }
        return ""
    }

    /**
     * Get playerId Choice from UI
     */
    private fun getPlayerChoise(): Choice {
        if (logger.log.isNewStack()) {
            return Choice.ADD
        }
        val choiceQueue = LinkedBlockingQueue<Choice>(1)
        val callback = { x: Choice -> choiceQueue.offer(x) }
        gameGUI.registerChoicesCallback(callback, *Choice.values())
        gameGUI.resetAllChoices()
        if (cardsCount[playerID] == 0) {
            gameGUI.disableChoices(Choice.ADD)
        }
        gameGUI.showChoicesOverlay()
        val x = choiceQueue.take()
        gameGUI.hideChoicesOverlay()
        return x
    }


    /**
     * Get number of cards that playerId will play this round
     */
    private fun getAddCount(): BetCount {
        val betCountQueue = LinkedBlockingQueue<BetCount>(1)
        val callback = { x: BetCount -> betCountQueue.offer(x) }
        gameGUI.registerBetCountsCallback(callback, *BetCount.values())
        gameGUI.resetAllBetCounts()
        val toDisable = BetCount.values().filter { x -> x.size > cardsCount[playerID] }
        gameGUI.disableBetCounts(*toDisable.toTypedArray())
        gameGUI.showBetCountOverlay()
        val x = betCountQueue.take()
        gameGUI.hideBetCountOverlay()
        return x
    }

    /**
     * Get pip of cards that will be claimed this round
     */
    private fun getPip(): Pip {
        if (!logger.log.isNewStack()) {
            return Pip.UNKNOWN
        }
        val pipQueue = LinkedBlockingQueue<Pip>(1)
        val callback = { x: Pip -> pipQueue.offer(x) }
        gameGUI.registerPipCallback(callback, *Pip.values())
        gameGUI.resetAllPips()
        gameGUI.showPipOverlay()
        val x = pipQueue.take()
        gameGUI.hidePipOverlay()
        return x
    }

    /**
     * Reveal card sent by verifier.
     * Log it and check, that everything was consistent.
     * Reveal card and hide it(GUI animations)
     * set cardReceiver to the id of playerId,
     * who should receive all cards on the table
     */
    private fun revealCard(responses: List<GameMessageProto.GameStateMessage>) {
        for (msg in responses) {
            val user = User(msg.user)
            val userId = getUserID(user)
            if (userId == currentPlayerID) {
                logger.log.registerVerifyResponse(user, msg.value)
                val parts = msg.value.split(" ")
                val cardId = parts[0].toInt()
                val cardKey = BigInteger(parts[1])
                val card = logger.log.registerCardKey(userId, cardId, cardKey)
                val guess = logger.log.checkPip(card)
                if (playerID != currentPlayerID) {

                    if (playerCards.contains(getCardById(card, deckSize))) {
                        throw GameExecutionException("I already hald the card, that was revealed")
                    }
                    val totPlayed = logger.log.countUserCardOnStack(playerOrder[userId])
                    val lastPlayed = logger.log.getLastUserBetCountSize(playerOrder[userId])
                    gameGUI.revealAndHidePlayedCard(getTablePlayerId(userId),
                            totPlayed - lastPlayed + cardToReveal, card)
                }
                if (guess) {
                    receiverPlayerID = currentPlayerID
                } else {
                    receiverPlayerID = getNextPlayer()
                }
            }
        }
    }

    /**
     * This function is used during revealing cards to
     * one playerId. It tages list of responses, exactly one
     * of which contains encrypted info about passed cards
     * (this data is stored in data field of protobuf message
     */
    private fun processEncryptedMessages(responses: List<GameMessageProto.GameStateMessage>) {
        if (currentPlayerID == -1) {
            return
        }
        val cards = mutableListOf<Int>()
        for (msg in responses) {
            val user = User(msg.user)
            val userId = getUserID(user)
            if (userId == currentPlayerID) {
                val encrypts = msg.dataList.map { x -> String(x.toByteArray()) }
                logger.log.regiserEncryptedMessage(user, encrypts)
                if (playerID == receiverPlayerID) {
                    for (encrypted in encrypts) {
                        val parts = keyManager.decodeString(encrypted).split(" ")
                        val cardID = parts[0].toInt()
                        val key = BigInteger(parts[1])
                        val card = logger.log.registerCardKey(userId, cardID, key)
                        //todo - mb check commitment here, aside from endgame
                        cards.add(card)
                        playerCards.add(getCardById(card, deckSize))
                    }
                } else {
                    for (i in 0..encrypts.size - 1) {
                        cards.add(-1)
                    }
                }
            }
        }
        cardsCount[currentPlayerID] -= cards.size
        cardsCount[receiverPlayerID] += cards.size
        for (i in (cards.size - 1) downTo 0 step 1) {
            gameGUI.transferPlayedCardToPlayer(getTablePlayerId(currentPlayerID),
                    getTablePlayerId(receiverPlayerID),
                    i, cards[i])
        }
    }

    /**
     * If rount just ended - update list of winners
     * (check will be conducted afterwards)
     */
    private fun updateWinners() {
        if (!logger.log.stackFinished()) {
            return
        }
        for (i in 0..N - 1) {
            if (cardsCount[i] == 0) {
                winners[i] = true
            }
        }
    }

    /**
     * Get next active playerId starting
     * from n
     *
     * @param n - starting playerId
     * @return id of next active playerId
     */
    private fun getNextPlayer(n: Int = currentPlayerID): Int {
        var res = (n + 1) % N
        while (winners[res]) {
            res = (res + 1) % N
        }
        return res
    }

    /**
     * Get first active playerId befor n
     *
     * @param n - starting playerId
     * @return id of last active playerId
     */
    private fun getPreviousPlayer(n: Int = currentPlayerID): Int {
        var res = n - 1
        if (res < 0) {
            res += N
        }
        while (winners[res]) {
            res--
            if (res < 0) {
                res += N
            }
        }
        return res
    }

    /**
     * after someone won - everyone send either WINNING_PRASE
     * or LOSING_PHRASE. Verify that received messages match
     * our data about winners
     */
    private fun compareWinningMessages(responses: List<GameMessageProto.GameStateMessage>) {
        for (msg in responses) {
            val userID = getUserID(User(msg.user))
            when (msg.value) {
                WINNING_PHRASE -> if (!winners[userID]) throw GameExecutionException("[${msg.user.name}] claims that he won, but according to my logs he didn't")
                LOSING_PHRASE -> if (winners[userID]) throw GameExecutionException("[${msg.user.name}] claims that he lost, but according to my logs he didn't")
                else -> throw GameExecutionException("Unexpected phrase received")
            }
        }
    }

    override fun isFinished(): Boolean {
        return state == State.END
    }

    override fun getInitialMessage(): String {
        return keyManager.getPublicKey()
    }

    override fun getResult() {
    }

    override fun getData(): List<ByteArray> {
        return extraData
    }

    override fun getFinalMessage(): String {
        val winners = winners.zip(playerOrder).filter { x -> x.first }.joinToString("] and [", "[", "]", transform = { x -> x.second.name })
        return "Everything appears to be correct and consistent. I agree that $winners won! Contratz!"
    }

    override fun close() {
        application.stop()
    }
}
