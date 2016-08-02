package apps.games.serious.Cheat

import apps.chat.Chat
import apps.games.Game
import apps.games.GameExecutionException
import apps.games.serious.Card
import apps.games.serious.Cheat.GUI.CheatGame
import apps.games.serious.CardGame
import apps.games.serious.preferans.Bet
import apps.games.serious.preferans.GUI.PreferansGame
import apps.games.serious.preferans.ShuffledDeck
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import crypto.RSA.RSAKeyManager
import crypto.random.randomString
import entity.Group
import entity.User
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

    private enum class State{
        INIT,
        VALIDATE_KEYS,
        PICK_DECK_SIZE,
        GENERATE_DECK,
        DECRYPT_HAND,
        END,
    }

    private var state: State = State.INIT

    private lateinit var gameGUI: CheatGame
    private lateinit var application: LwjglApplication

    private val keyManager = RSAKeyManager()
    private val HANDSHAKE_PHRASE = "HANDSHAKE" // who would've thought
    private val N = group.users.size
    private val cardHolders: MutableMap<Int, Int> = mutableMapOf()
    private val playerCards = mutableSetOf<Card>()

    val logger = CheatGameLogger(N)

    override fun evaluate(responses: List<GameMessageProto.GameStateMessage>): String {
        for(msg in responses){
            chat.showMessage("[${msg.user.name}] said ${msg.value}")
        }
        when(state){

            State.INIT -> {
                for(msg in responses){
                    keyManager.registerUser(User(msg.user), msg.value)
                }
                initGame()
                currentPlayerID = -1
                state = State.VALIDATE_KEYS
            }
            State.VALIDATE_KEYS -> {
                if(playerID == currentPlayerID){
                    for(msg in responses){
                        try {
                            val s = keyManager.decodeString(msg.value)
                            val handshake = s.split(" ").last()
                            if(handshake != HANDSHAKE_PHRASE){
                                throw GameExecutionException("Invalid RSA key")
                            }
                        }catch (e: InvalidCipherTextException){
                            throw GameExecutionException("Malformed RSA key")
                        }
                    }
                }

                currentPlayerID ++
                if(currentPlayerID == N){
                    state = State.PICK_DECK_SIZE
                    chat.sendMessage("RSA is OK. Generating deck")
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
                for (msg in responses){
                    chat.showMessage("[${msg.user.name}] voted for ${msg.value} cardID deck")
                }
                val votes = responses.map { x -> x.value.toInt() }
                DECK_SIZE = votes.maxBy { x -> votes.count { s -> s == x } } ?:
                        throw GameExecutionException("Couldn't agree in deck size")
                chat.sendMessage("We are playing $DECK_SIZE cardID deck")
                state = State.DECRYPT_HAND
                initiateHands()
            }
            State.DECRYPT_HAND -> {
                decryptHand(responses)

            }
            State.END -> TODO()
        }
        return ""
    }


    /**
     * Start GUI for the Preferans game
     */
    private fun initGame(): String {
        val config = LwjglApplicationConfiguration()
        config.width = 1024
        config.height = 1024
        config.forceExit = false
        config.title = "Cheate Game[${chat.username}]"
        gameGUI = CheatGame(playerID)
        application = LwjglApplication(gameGUI, config)
        while (!gameGUI.loaded) {
            Thread.sleep(200)
        }
        return ""
    }

    /**
     * Let users pick desired DeckSize
     */
    private fun pickDeckSize(): Int{
        val deckSizeQueue = LinkedBlockingQueue<DeckSizes>(1)
        val callback = {x: DeckSizes -> deckSizeQueue.offer(x)}
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
        updateDeck()
        val resultKeys = mutableListOf<BigInteger>()
        for (i in 0..DECK_SIZE-1) {
            val holder = i % N
            cardHolders[i] = holder
            if (holder != playerID) {
                resultKeys.add(deck.encrypted.keys[i])
            }
        }
        return resultKeys.joinToString(" ")
    }

    /**
     * Take a list of keys sent by user,
     * keys should correspond to cards, that are not
     * by that user. I.E. If player holds cards
     * 1, 4, 7 in shuffled deck, keys - contains
     * key for every cardID, that is not in TALON,
     * and are not possesed by that user
     */
    private fun decryptHand(responses: List<GameMessageProto.GameStateMessage>){
        for (msg in responses){
            val keys = msg.value.split(" ").map { x -> BigInteger(x) }
            val userID = getUserID(User(msg.user))
            val positions = cardHolders.filterValues { x -> x != userID }.keys.toList()
            if(keys.size != positions.size){
                throw GameExecutionException("Someone failed to provide his keys")
            }
            for(i in 0..keys.size-1){
                if(logger.log.registerCardKey(userID, positions[i], keys[i])){
                    TODO()
                }
            }
        }
    }


    override fun isFinished(): Boolean {
        return state == State.END
    }

    override fun getInitialMessage(): String {
        return keyManager.getPublicKey()
    }

    override fun getResult() {}


}