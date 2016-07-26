package apps.games.serious.preferans

import apps.chat.Chat
import apps.games.Game
import apps.games.GameExecutionException
import apps.games.primitives.Deck
import apps.games.primitives.EncryptedDeck
import apps.games.primitives.protocols.DeckShuffleGame
import apps.games.primitives.protocols.RandomDeckGame
import apps.games.serious.preferans.GUI.Player
import apps.games.serious.preferans.GUI.PreferansGame
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import crypto.random.randomString
import entity.ChatMessage
import entity.Group
import entity.User
import org.apache.commons.codec.digest.DigestUtils
import org.bouncycastle.jce.ECNamedCurveTable
import proto.GameMessageProto
import java.math.BigInteger
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.LinkedBlockingQueue

/**
 * Created by user on 7/6/16.
 */

class Preferans(chat: Chat, group: Group, gameID: String) : Game<Unit>(chat,
                                                                       group,
                                                                       gameID) {
    override val name: String
        get() = "Preferans CardGUI Game"

    private enum class State {
        INIT, //start game
        ROUND_INIT, //generate deck
        DECRYPT_HAND, //decrypt hands
        BIDDING, //decide on game contract
        VERIFY_BET, // veryfy, that everyone agrees on bet
        REVEAL_TALON, // open talon
        REBID, //if contract is not PASS - give talon to contract holder
        WHISTING, //if contract os not PASS - dicide on whisting
        WHISTING_RESULT, //exchage results of whisting
        OPEN_HAND_KEY_EXHANGE, //if after whisting game whisted only ny one
        OPEN_HAND_DECRYPT, // player and he dycided top open hands - exchange keys
        PLAY, //play cards =)
        VERYFY_ROUND, //Verify talon, cards played and key sets
        END
    }

    private val ECParams = ECNamedCurveTable.getParameterSpec("secp256k1")
    private var state: State = State.INIT

    private lateinit var gameGUI: PreferansGame
    private lateinit var application: LwjglApplication

    private val DECK_SIZE = 32
    //TALON - always last two cards of the deck
    private val TALON = 2

    //to sorted array to preserve order
    private val playerOrder: MutableList<User> = group.users.sortedBy { x ->
                                                        x.name }.toMutableList()
    private var playerID = playerOrder.indexOf(chat.me())

    //Required - three players.
    //TODO - add checker for number of players

    private val N = 3

    //map cards to players, that holded that card.
    //cardholder -1 - for played card
    private val cardHolders: MutableMap<Int, Int> = mutableMapOf()
    private val originalCardHolders: MutableMap<Int, Int> = mutableMapOf()

    //player whose turn is right now
    private var currentPlayerID: Int = 0
    private val bets = Array(N, { i -> Bet.UNKNOWN })
    private val whists = Array(N, { i -> Whists.UNKNOWN })
    private var gameWhist: Whists = Whists.UNKNOWN
    //Player who has highest contract
    private var mainPlayerID: Int = -1

    //Bet, that will be played this round
    private var gameBet: Bet = Bet.UNKNOWN


    //shuffled Deck
    private lateinit var deck: ShuffledDeck

    //Receive bets
    private val betQueue = LinkedBlockingQueue<Bet>(1)

    //Log everything
    private val logger = GameLogger(N, DECK_SIZE, TALON)
    //Scoring
    private val scoreCounter = PreferansScoreCounter(playerOrder)

    //Salt and hash talon for later verification
    val SALT_LENGTH = 256
    private var salt: String = ""
    private var talonHash: String = ""
    private var talonKeys: String = ""

    override fun getInitialMessage(): String {
        return playerOrder.hashCode().toString()
    }

    override fun isFinished(): Boolean {
        return state == State.END
    }

    override fun evaluate(responses: List<GameMessageProto.GameStateMessage>): String {
        when (state) {
            State.INIT -> {
                if (group.users.size != N) {
                    throw GameExecutionException("Only 3 player Preferans is " +
                                                         "supported")
                }
                state = State.ROUND_INIT
                return initGame(responses)
            }
            State.ROUND_INIT -> {
                state = State.DECRYPT_HAND
                return initRound(responses)
            }
            State.DECRYPT_HAND -> {
                state = State.BIDDING
                decryptHand(responses)
                dealHands()
                gameGUI.showBiddingOverlay()
                gameGUI.disableAllBets()
                registerCallbacks()
                currentPlayerID = -1
            }
            State.BIDDING -> {
                betQueue.clear()
                for (msg in responses) {
                    // TODO - digital signatures on player or encrypt channel
                    val userID = getUserID(User(msg.user))
                    if (userID == currentPlayerID) {
                        bets[userID] = Bet.values().first { x -> x.value == msg.value.toInt() }
                    }
                }
                currentPlayerID = (currentPlayerID + 1) % N
                if (currentBet() != Bet.UNKNOWN) {
                    gameBet = currentBet()
                    logger.log.updateBet(gameBet)
                    mainPlayerID = bets.indexOf(gameBet)
                    state = State.VERIFY_BET
                    return gameBet.type
                }
                return getBid()
            }
            State.VERIFY_BET -> {
                state = State.REVEAL_TALON
                gameGUI.hideBiddingOverlay()


                val hashes = responses.map { x -> x.value }
                if (hashes.distinct().size != 1) {
                    throw GameExecutionException("Couldn't agree on final bet")
                }
                chat.sendMessage("We will play at least ${gameBet.type}")
                val keys = mutableListOf<BigInteger>()
                for (i in TALON downTo 1) {
                    keys.add(deck.encrypted.keys[DECK_SIZE - i])
                }
                gameGUI.hideBiddingOverlay()
                return keys.joinToString(" ")
            }
            State.REVEAL_TALON -> {
                decryptTalon(responses)
                revealTalon()

                if (gameBet != Bet.PASS) {
                    state = State.REBID
                } else {
                    logger.log.updateBet(Bet.PASS)
                    currentPlayerID = -1
                    //Talon belongs to noone
                    for(i in DECK_SIZE-TALON..DECK_SIZE-1){
                        cardHolders[i] = -1
                    }
                    state = State.PLAY
                }

                return ""
            }
            State.REBID -> {
                state = State.WHISTING
                dealTalon()
                currentPlayerID = mainPlayerID

                if (playerID != mainPlayerID) {
                    gameGUI.showHint("Waiting for " +
                                             "[${playerOrder[mainPlayerID].name}]" +
                                             "to update his bid according to " +
                                             "Talon")
                    return ""
                } else {
                    gameGUI.showBiddingOverlay()
                    val res = getBid()
                    gameGUI.hideBiddingOverlay()
                    //TODO - log turn for later validation
                    val card1 = playCard(playerID, restrictCards = false)
                    val card2 = playCard(playerID, restrictCards = false)
                    saltTalon(card1, card2)
                    return res + " " + talonHash
                }
            }
            State.WHISTING -> {
                state = State.WHISTING_RESULT
                for (msg in responses) {
                    val userID = getUserID(User(msg.user))
                    if (userID == mainPlayerID) {
                        val split = msg.value.split(" ")
                        bets[userID] = Bet.values().first { x -> x.value == split[0].toInt() }
                        gameBet = bets[mainPlayerID]
                        logger.log.updateBet(gameBet)
                        talonHash = split[1]
                    }
                }
                return startWhisting()

            }
            State.WHISTING_RESULT -> {
                whists.fill(Whists.UNKNOWN)
                gameWhist = Whists.UNKNOWN
                for (msg in responses) {
                    val userID = getUserID(User(msg.user))
                    if (userID != mainPlayerID) {
                        whists[userID] = Whists.valueOf(msg.value)
                    }
                }
                //Validate whists
                gameWhist = verifyWhists()
                when (gameWhist) {

                    Whists.UNKNOWN -> throw GameExecutionException("Couldn't agree on whisting")
                    Whists.PASS -> {
                        updateScore()
                        state = State.ROUND_INIT
                    }
                    Whists.WHIST_HALF -> {
                        updateScore()
                        state = State.ROUND_INIT
                    }
                    Whists.WHIST_BLIND -> {
                        state = State.PLAY
                        //TODO - store first player ID
                        currentPlayerID = -1
                    }
                    Whists.WHIST_OPEN -> {
                        state = State.OPEN_HAND_KEY_EXHANGE
                    }
                }
            }
            State.OPEN_HAND_KEY_EXHANGE -> {
                state = State.OPEN_HAND_DECRYPT
                if (playerID == mainPlayerID) {
                    return ""
                }
                val keys = mutableListOf<BigInteger>()
                for (i in 0..DECK_SIZE - 1) {
                    if (cardHolders[i] == playerID) {
                        keys.add(deck.encrypted.keys[i])
                    }
                }
                return keys.joinToString(" ")
            }
            State.OPEN_HAND_DECRYPT -> {
                for (msg in responses) {
                    val userID = getUserID(User(msg.user))
                    if (userID != mainPlayerID && userID != playerID) {
                        val keys = msg.value.split(" ").map { x ->
                            BigInteger(x)
                        }
                        decryptUserHand(User(msg.user), keys)
                    }
                }
                //TODO - store first player ID
                currentPlayerID = -1
                state = State.PLAY
            }
            State.PLAY -> {
                //In case of Open Whist we need spetial case -
                //one user can manage cards of another
                var mainUser: Int
                if(currentPlayerID != -1 && gameWhist == Whists.WHIST_OPEN &&
                        whists[currentPlayerID]
                        == Whists.PASS){
                    mainUser = whists.indexOf(Whists.WHIST_OPEN)
                }else{
                    mainUser = currentPlayerID
                }

                for (msg in responses) {
                    val userID = getUserID(User(msg.user))
                    if (userID == mainUser) {
                        //TODO - switch first players
                        val split = msg.value.split(" ")
                        if (split.size < 2) {
                            throw GameExecutionException("Invalid card " +
                                                                 "received")
                        }
                        val cardID = split[0].toInt()
                        val key = BigInteger(split[1])
                        var index = deck.originalDeck.cards.indexOf(deck.encrypted.deck.cards[cardID])

                        if (index == -1) {
                            logger.log.registerCardKey(currentPlayerID,
                                                       cardID, key)

                            deck.encrypted.deck.decryptCardWithKey(cardID, key)
                            index = deck.originalDeck.cards.indexOf(deck.encrypted.deck.cards[cardID])
                            if (index == -1) {
                                throw GameExecutionException("Can not decrypt" +
                                                                     " card")
                            }
                            originalCardHolders[index] = currentPlayerID
                            if (playerID != currentPlayerID) {
                                gameGUI.revealPlayerCard(
                                        getTablePlayerId(currentPlayerID),
                                        index)
                            }
                        }
                        if(playerID != currentPlayerID){
                            gameGUI.playCard(index)
                        }
                        currentPlayerID = logger.log.registerPlay(currentPlayerID
                                                                       to index) ?:
                                throw GameExecutionException("Can not find next player")


                        if(logger.log.newTurnStarted()){
                            gameGUI.tableScreen.clearTable()
                        }

                        if(logger.log.roundFinished()){
                            state = State.VERYFY_ROUND
                            if(playerID == mainPlayerID){
                                return salt + " " + talonKeys
                            }else{
                                return ""
                            }
                        }
                    }
                }
                if(mainUser == -1){
                    currentPlayerID ++
                }
                if(currentPlayerID != -1 && gameWhist == Whists.WHIST_OPEN &&
                                            whists[currentPlayerID] == Whists.PASS){
                    mainUser = whists.indexOf(Whists.WHIST_OPEN)
                }else{
                    mainUser = currentPlayerID
                }
                //in case of Open whists - someone plays instead of
                // passed player
                if (playerID == mainUser) {
                    gameGUI.showHint("[${gameBet.type}] It is your turn to " +
                                             "play " +
                                             "(play " +
                                             "${logger.log.enforcedSuit.type} " +
                                             "if possible)")
                    val pos = playCard(currentPlayerID)
                    val index = deck.encrypted.deck.cards.indexOf(
                            deck.originalDeck.cards[pos])
                    return "$index ${deck.encrypted.keys[index]}"
                }
                else{
                    gameGUI.showHint("[${gameBet.type}] Waiting for " +
                                             "${playerOrder[currentPlayerID]
                            .name} to make hist move")
                }

            }
            State.VERYFY_ROUND -> {
                verifyRound(responses)
                updateScore()

                state = State.ROUND_INIT
            }
            State.END -> { }
        }
        return ""
    }

    private fun updateScore() {
        val roundResult = logger.log.countWonTurns()
        var res: String = ""
        for(key in roundResult.keys){
            res += "<${playerOrder[key].name}> has won ${roundResult[key]} " +
                    "turns \n"
        }
        for(i in 0..N-1){
            if(!roundResult.containsKey(i)){
                roundResult[i] = 0
            }
        }
        val handsTaken = roundResult.mapKeys { x -> playerOrder[x.key] }

        val whisted = playerOrder.zip(whists).toMap()
        scoreCounter.updateScore(handsTaken, gameBet, whisted, playerOrder[mainPlayerID])
        chat.sendMessage(res)
        chat.sendMessage("Evething seems to be consistent. My current score " +
                                 "is: [TODO]")
    }

    /**
     * Start GUI for the Preferans game
     */
    private fun initGame(responses: List<GameMessageProto.GameStateMessage>): String {
        //validate player order
        val hashes = responses.distinctBy { x -> x.value }
        if (hashes.size != 1) {
            throw GameExecutionException("Someone has different deck")
        }

        val config = LwjglApplicationConfiguration()
        config.width = 1024
        config.height = 1024
        config.forceExit = false
        config.title = "Preferans Game[${chat.username}]"
        gameGUI = PreferansGame(scoreCounter, playerID      )
        application = LwjglApplication(gameGUI, config)
        while (!gameGUI.loaded) {
            Thread.sleep(200)
        }
        //gameGUI.initScoreOverlay(scoreCounter, playerID)
        return ""
    }

    /**
     * Start next round of the game:
     * create and shuffle deck. compute who holds which card
     * return keys for cards that I don't hold
     */
    private fun initRound(responses: List<GameMessageProto.GameStateMessage>): String {
        //shift order
        val first = playerOrder.removeAt(0)
        playerOrder.add(first)
        playerID = playerOrder.indexOf(chat.me())
        logger.newRound()

        gameGUI.showHint("Shuffling cards")
        //If we can not create deck - game aborted
        deck = newDeck() ?: return ""
        //Deal all cards, except last two
        cardHolders.clear()
        gameGUI.clear()
        //reset bet and whist
        gameBet = Bet.UNKNOWN
        gameWhist = Whists.UNKNOWN
        whists.fill(Whists.UNKNOWN)
        bets.fill(Bet.UNKNOWN)
        talonHash = ""
        talonKeys = ""
        salt = ""
        val resultKeys = mutableListOf<BigInteger>()

        for (i in 0..deck.originalDeck.size - 1 - TALON) {
            val holder = i % N
            cardHolders[i] = holder
            if (holder != playerID) {
                resultKeys.add(deck.encrypted.keys[i])
            }
        }
        return resultKeys.joinToString(" ")
    }

    /**
     * Given responses from ROUND_INIT stage
     * decrypt cards in my hand
     */
    private fun decryptHand(responses: List<GameMessageProto.GameStateMessage>): String {
        for (i in 0..DECK_SIZE - 1) {
            logger.log.registerCardKey(playerID, i, deck.encrypted.keys[i])
        }
        deck.encrypted.deck.decryptSeparate(deck.encrypted.keys)
        for (msg in responses) {
            if (getUserID(User(msg.user)) == playerID) {
                continue
            }
            val keys = msg.value.split(" ").map { x -> BigInteger(x) }
            decryptRoundInit(User(msg.user), keys)
        }
        return ""
    }

    /**
     * Verify thta all plays of this round were consistent
     */
    private fun verifyRound(responses: List<GameMessageProto.GameStateMessage>){
        var split: List<String> = listOf()
        for(msg in responses){
            val userID = getUserID(User(msg.user))
            if(userID == mainPlayerID){
                split = msg.value.split(" ")
                salt = split[0]
            }
        }
        if(!logger.log.verifyRoundPlays()){
            throw GameExecutionException("Someone cheated: didn't " +
                                                 "play correct card")
        }
        if(gameBet != Bet.PASS){
            val talon = logger.log.getDiscardedTalon() ?: throw
            GameExecutionException("can not figure out talon")
            val computedTalonHash = DigestUtils.sha256Hex(talon.joinToString(" ") + salt)
            if(computedTalonHash != talonHash){
                throw GameExecutionException("Failed to validate talon")
            }
            for(i in 1..split.size-1 step 2){
                val index = split[i].toInt()
                val key = BigInteger(split[i+1])
                logger.log.registerCardKey(mainPlayerID, index, key)
            }
        }
        for(i in 0..N-1){
            val playerKeyHash = logger.log.getUserKeysHash(i) ?:
                    throw GameExecutionException("Someone didn't " +
                                                         "provide all" +
                                                         " of his keys")
            if(playerKeyHash != deck.encrypted.hashes[playerOrder[i]]){
                throw GameExecutionException("Key hash validation " +
                                                     "failed. Maybe " +
                                                     "someone spawed " +
                                                     "thei keys")
            }
        }
    }


    /**
     * Show bidding overlay, get bet from
     * the player
     */
    private fun getBid(): String {
        val toDisplay = Array(N, { i -> Pair(playerOrder[i], bets[i]) })
        //display bets of players, also disables them
        if (playerID == currentPlayerID && bets[playerID] != Bet.PASS) {
            gameGUI.resetAllBets()
            gameGUI.markBets(*toDisplay)
            val maxBet = bets.maxBy { x -> x.value } ?: throw GameExecutionException(
                    "Something went wront in betting")
            gameGUI.disableAllBets()
            //player alvays can bet higher then current bidding
            gameGUI.enableBets(
                    *Bet.values().filter { x -> x.value > maxBet.value }.toTypedArray())

            //If player is currently highest bidder - he can stick to the same bet
            //if he is not - he can pass
            if (bets[playerID] == maxBet) {
                gameGUI.enableBets(bets[playerID])
                gameGUI.showHint(
                        "Your turn! You can bid [${maxBet.type}] or higher")
            } else {
                gameGUI.enableBets(Bet.PASS)
                gameGUI.showHint(
                        "Your turn! You can bid [${maxBet.type}] or higher or PASS")
            }
            if (bets[playerID] != Bet.UNKNOWN) {
                gameGUI.disableBets(Bet.MIZER)
            }
            betQueue.clear()
            val bet = betQueue.take()
            gameGUI.disableAllBets()
            return bet.value.toString()
        } else {
            gameGUI.resetAllBets()
            gameGUI.disableAllBets()
            gameGUI.markBets(*toDisplay)
            gameGUI.showHint(
                    "Waiting for [${playerOrder[currentPlayerID].name}] to make his move")
            if (bets[playerID] == Bet.PASS) {
                return Bet.PASS.value.toString()
            }
            return ""
        }
    }

    /**
     * Receive keys of talon, decrypt it, show to everyone
     */
    fun decryptTalon(responses: List<GameMessageProto.GameStateMessage>) {
        gameGUI.showHint("Revealing talon")
        for (msg in responses) {
            if (User(msg.user) == chat.me()) {
                continue
            }
            val keys = msg.value.split(" ").map { x -> BigInteger(x) }
            if (keys.size != TALON) {
                throw GameExecutionException(
                        "Someone sent incorrect number of talon keys")
            }
            for (i in 0..TALON - 1) {
                logger.log.registerCardKey(getUserID(User(msg.user)),
                                           DECK_SIZE - TALON + i, keys[i])

                deck.encrypted.deck.decryptCardWithKey(DECK_SIZE - TALON + i,
                                                       keys[i])
            }
        }
    }

    /**
     * Reveal Talon.
     */
    fun revealTalon() {
        val talon = mutableListOf<Int>()
        for (i in 0..TALON - 1) {
            cardHolders[DECK_SIZE - TALON + i] = mainPlayerID
            val index = deck.originalDeck.cards.indexOf(
                    deck.encrypted.deck.cards[DECK_SIZE - TALON + i])
            if (index == -1) {
                throw GameExecutionException(
                        "Someone sent incorrect talon keys")
            }
            //reveal Common
            talon.add(index)
            gameGUI.revealTalonCard(index)
        }
        logger.log.registerTalon(talon)
    }

    /**
     * Give talon to main player
     */
    fun dealTalon() {
        for (i in 0..TALON - 1) {
            val cardId = deck.originalDeck.cards.indexOf(deck.encrypted
                                                                 .deck.cards[DECK_SIZE - TALON + i])
            gameGUI.giveCard(cardId, -1, getTablePlayerId(mainPlayerID),
                             flip = (playerID != mainPlayerID))
        }
    }

    /**
     * Create a new deck and shuffle it.
     * In Preferans this is executed before
     * each round
     * @return Pair of original Deck and
     * shuffle result - EncryptedDeck
     */
    private fun newDeck(): ShuffledDeck? {
        val deckFuture = runSubGame(
                RandomDeckGame(chat, group.clone(), subGameID(), ECParams,
                               DECK_SIZE))
        val deck: Deck
        try {
            deck = deckFuture.get()
        } catch(e: CancellationException) { // Task was cancelled - means that we need to stop. NOW!
            state = State.END
            return null
        } catch(e: ExecutionException) {
            chat.showMessage(
                    ChatMessage(chat, e.message ?: "Something went wrong"))
            e.printStackTrace()
            throw GameExecutionException("Subgame failed")
        }

        val shuffleFuture = runSubGame(
                DeckShuffleGame(chat, group.clone(), subGameID(), ECParams,
                                deck.clone()))
        val shuffled: EncryptedDeck
        try {
            shuffled = shuffleFuture.get()
        } catch(e: CancellationException) { // Task was cancelled - means that we need to stop. NOW!
            state = State.END
            return null
        } catch(e: ExecutionException) {
            chat.showMessage(
                    ChatMessage(chat, e.message ?: "Something went wrong"))
            e.printStackTrace()
            throw GameExecutionException("Subgame failed")
        }
        return ShuffledDeck(deck, shuffled)
    }

    /**
     * Take a list of keys sent by user,
     * keys should correspond to cards, that are not
     * by that user. I.E. If player holds cards
     * 1, 4, 7 in shuffled deck, keys - contains
     * key for every card, that is not in TALON,
     * and are not possesed by that user
     */
    private fun decryptRoundInit(user: User, keys: List<BigInteger>) {
        val positions = mutableListOf<Int>()
        for (key in cardHolders.keys) {
            if (cardHolders[key] != getUserID(user)) {
                positions.add(key)
            }
        }
        if (positions.size != keys.size) {
            throw GameExecutionException(
                    "Someone failed to provide correct keys for encrypted cards")
        }
        for (i in 0..positions.size - 1) {
            val position = positions[i]
            logger.log.registerCardKey(getUserID(user), position, keys[i])
            deck.encrypted.deck.decryptCardWithKey(position, keys[i])
        }
    }

    /**
     * If open whist is played - user hand keys are r
     * revealed. Use this
     */
    private fun decryptUserHand(user: User, keys: List<BigInteger>) {
        val positions = mutableListOf<Int>()
        for (key in cardHolders.keys) {
            if (cardHolders[key] == getUserID(user)) {
                positions.add(key)
            }
        }
        if (positions.size != keys.size) {
            throw GameExecutionException(
                    "Someone failed to provide correct keys for encrypted cards")
        }

        for (i in 0..positions.size - 1) {
            logger.log.registerCardKey(getUserID(user), positions[i], keys[i])
            deck.encrypted.deck.decryptCardWithKey(positions[i], keys[i])
            val cardID = deck.originalDeck.cards.indexOf(deck.encrypted.deck
                                                                 .cards[positions[i]])
            gameGUI.revealPlayerCard(getTablePlayerId(getUserID(user)), cardID)
        }
    }

    /**
     * Give each player cards, that
     * belong to his hand(GUI)
     */
    private fun dealHands() {
        gameGUI.showHint("Dealing hands")
        gameGUI.tableScreen.showDeck()
        for (i in 0..DECK_SIZE - TALON - 1) {
            val cardID: Int
            if (cardHolders[i] == playerID) {
                cardID = deck.originalDeck.cards.indexOf(
                        deck.encrypted.deck.cards[i])
                if (cardID == -1) {
                    throw GameExecutionException(
                            "I can not decrypt my own cards")
                }
            } else {
                cardID = -1
            }
            val currentPlayerId: Int = cardHolders[i] ?: throw
            GameExecutionException(
                    "Invalid card distribution")
            val guiID = getTablePlayerId(currentPlayerId)
            gameGUI.dealPlayer(guiID, cardID)
        }
        //Deal unknown Talon cards
        for (i in 1..TALON) {
            gameGUI.dealCommon(-1)
        }
        gameGUI.tableScreen.hideDeck()
    }

    /**
     * Skip game if we are main player or
     * run subgame if we are not
     */
    private fun startWhisting(): String{
        if (playerID == mainPlayerID) {
            gameGUI.showHint("Waiting for other players to decide on " +
                                     "whisting")
            skipSubGame()
            // send (slated) hash of discarded cards to prove, that
            // you will not play them later
            return talonHash
        } else {
            //remove talon cards
            for (i in DECK_SIZE - TALON..DECK_SIZE - 1) {
                val index = deck.originalDeck.cards.indexOf(
                        deck.encrypted.deck.cards[i])
                gameGUI.playCard(index)
            }
            val whistingGroup = group.clone()
            whistingGroup.users.remove(playerOrder[mainPlayerID])
            val whistFuture = runSubGame(WhistingGame(chat,
                                                      whistingGroup,
                                                      subGameID(),
                                                      gameManager,
                                                      gameGUI,
                                                      bets[mainPlayerID]),
                                         Int.MAX_VALUE)
            val res = whistFuture.get().name
            println(res)
            return res
        }
    }

    /**
     * Check, that current whists array is in consistent
     * state
     *
     * @return greatest of all whists
     * (it defines game scenario)
     */
    private fun verifyWhists(): Whists {
        val activeWhists: MutableList<Whists> = whists.toMutableList()
        activeWhists.removeAt(mainPlayerID)
        return WhistingGame.verifyWhists(activeWhists.toTypedArray())
    }

    /**
     * Get current bet result:
     * UNKNOWN - if agreement is not met yet
     * or round is not finished
     *
     * Otherwise - return Bet that is played
     */
    private fun currentBet(): Bet {
        if (currentPlayerID != 0 || bets.contains(Bet.UNKNOWN)) {
            return Bet.UNKNOWN
        }
        when (bets.count { x -> x == Bet.PASS }) {
            N -> return Bet.PASS
            N - 1 -> return bets.first { x -> x != Bet.PASS }
            else -> return Bet.UNKNOWN
        }
    }

    /**
     * play any known card that is possesed by
     * player with given id
     * @param playerID - player whose card will be played
     * @param restrictCards - wether to allow to play
     * cards that don't match current trump suit
     * @return ID of picked card
     */
    fun playCard(vararg playerIDs: Int, restrictCards: Boolean = true): Int {
        val allowed = mutableListOf<Card>()
        for (key in cardHolders.keys) {
            if (playerIDs.contains(cardHolders[key]!!)) {
                val card = deck.encrypted.deck.cards[key]
                allowed.add(
                        getCardById32(deck.originalDeck.cards.indexOf(card)))
            }
        }
        if (restrictCards) {
            logger.log.filterPlayableCards(allowed)
        }
        val res = gameGUI.pickCard(*allowed.toTypedArray())
        val index = deck.encrypted.deck.cards.indexOf(deck.originalDeck
                                                              .cards[res])
        if (index == -1) {
            println(res)
            throw GameExecutionException("Can not pick unknown card" +
                                                 "(contradicts selector)")
        }
        cardHolders[index] = -1
        return res
    }

    fun registerCallbacks() {
        val callback = { x: Bet ->
            betQueue.offer(x)
        }
        gameGUI.registerBiddingCallback(callback, *Bet.values())
    }

    fun getUserID(user: User): Int {
        return playerOrder.indexOf(user)
    }

    /**
     * During the game in GUI - ew are always player 0,
     * meanwhile in game we are not
     */
    fun getTablePlayerId(id: Int): Int {
        var res: Int = id - playerID
        if (res < 0) {
            res += N
        }
        return res
    }

    fun saltTalon(card1: Int, card2: Int) {
        salt = randomString(SALT_LENGTH)
        val discarded = listOf(card1, card2).sorted()
        val temp = discarded.joinToString(" ")
        talonHash = DigestUtils.sha256Hex(temp + salt)
        talonKeys = discarded.map { x -> deck.encrypted.deck.cards.indexOf(deck.originalDeck.cards[x]) }
                             .map { x -> "$x ${deck.encrypted.keys[x]}"}.joinToString(" ")

    }

    override fun getResult() {
        return Unit
    }

    override fun close() {
        application.stop()
    }
}

data class ShuffledDeck(val originalDeck: Deck, val encrypted: EncryptedDeck)