package apps.games.serious.preference

import apps.chat.Chat
import apps.games.Game
import apps.games.GameExecutionException
import apps.games.primitives.Deck
import apps.games.primitives.EncryptedDeck
import apps.games.primitives.protocols.DeckShuffleGame
import apps.games.primitives.protocols.RandomDeckGame
import apps.games.serious.preference.GUI.PreferenceGame
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import entity.ChatMessage
import entity.Group
import entity.User
import org.bouncycastle.jce.ECNamedCurveTable
import proto.GameMessageProto
import java.math.BigInteger
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException

/**
 * Created by user on 7/6/16.
 */

class Preference(chat: Chat, group: Group, gameID: String) : Game<Unit>(chat, group, gameID){
    override val name: String
        get() = "Preference Card Game"

    private enum class State{
        INIT,
        ROUND_INIT,
        DECRYPT,
        END
    }

    private val ECParams = ECNamedCurveTable.getParameterSpec("secp256k1")
    private var state: State = State.INIT

    private lateinit var gameGUI: PreferenceGame

    private val DECK_SIZE = 32
    private val TALON = 2

    //to sorted array to preserve order
    private val playerOrder: List<User> = group.users.sortedBy { x -> x.name }
    private val id = playerOrder.indexOf(chat.me())

    //Required - three players.
    //TODO - add checker for number of players

    private val N = 3
    private val cardHolders: MutableMap<Int, Int> = mutableMapOf()

    //shuffled Deck
    private lateinit var deck: ShuffledDeck

    override fun getInitialMessage(): String {
        return playerOrder.hashCode().toString()
    }

    override fun isFinished(): Boolean {
        return state == State.END
    }

    override fun evaluate(responses: List<GameMessageProto.GameStateMessage>): String {
        when(state){
            State.INIT -> {
                //validate player order
                val hashes = responses.distinctBy {x -> x.value}
                if(hashes.size != 1){
                    throw GameExecutionException("Someone has different deck")
                }

                val config = LwjglApplicationConfiguration()
                config.width = 1024
                config.height = 1024
                config.forceExit = false
                gameGUI = PreferenceGame()
                LwjglApplication(gameGUI, config)
                while(!gameGUI.loaded){
                    Thread.sleep(200)
                }
                state = State.ROUND_INIT
            }
            State.ROUND_INIT -> {
                //If we can not create deck - game aborted
                deck = newDeck() ?: return ""
                //Deal all cards, except last two
                cardHolders.clear()
                val resultKeys = mutableListOf<BigInteger>()

                for(i in 0..deck.originalDeck.size-1-TALON){
                    val holder = i % N
                    cardHolders[i] = holder
                    if(holder != id){
                        resultKeys.add(deck.encryptedDeck.keys[i])
                    }
                }
                state = State.DECRYPT
                return resultKeys.joinToString(" ")
            }
            State.DECRYPT -> {
                deck.encryptedDeck.deck.decryptSeparate(deck.encryptedDeck.keys)
                for(msg in responses){
                    // do not process messages from self
                    if(User(msg.user) == chat.me()){
                        continue
                    }
                    val keys = msg.value.split(" ")
                    val positions = mutableListOf<Int>()
                    for(key in cardHolders.keys){
                        if(cardHolders[key] != getUserID(User(msg.user))){
                            positions.add(key)
                        }
                    }
                    if(positions.size != keys.size){
                        throw GameExecutionException("Someone failed to provide correct keys for encrypted cards")
                    }
                    for(i in 0..positions.size-1){
                        val position = positions[i]
                        val key = BigInteger(keys[i])
                        deck.encryptedDeck.deck.decryptCardWithKey(position, key)
                        val card = deck.encryptedDeck.deck.cards[position]
                        if(deck.originalDeck.contains(card)){
                            gameGUI.dealCard(0, deck.originalDeck.cards.indexOf(card))
                        }
                    }
                }
                state = State.END
            }
            State.END -> {}
        }
        return ""
    }

    fun getUserID(user: User): Int{
        return playerOrder.indexOf(user)
    }

    override fun getResult() {
        return Unit
    }

    /**
     * Create a new deck and shuffle it.
     * In preference this is executed before
     * each round
     * @return Pair of original Deck and
     * shuffle result - EncryptedDeck
     */
    private fun newDeck(): ShuffledDeck?{
        val deckFuture = runSubGame(RandomDeckGame(chat, group.clone(), subGameID(), ECParams, DECK_SIZE))
        val deck: Deck
        try{
            deck = deckFuture.get()
        }catch(e: CancellationException){ // Task was cancelled - means that we need to stop. NOW!
            state = State.END
            return null
        }catch(e: ExecutionException){
            chat.showMessage(ChatMessage(chat, e.message?: "Something went wrong"))
            throw GameExecutionException("Subgame failed")
        }

        val shuffleFuture = runSubGame(DeckShuffleGame(chat, group.clone(), subGameID(), ECParams, deck.clone()))
        val shuffled: EncryptedDeck
        try{
            shuffled = shuffleFuture.get()
        }catch(e: CancellationException){ // Task was cancelled - means that we need to stop. NOW!
            state = State.END
            return null
        }catch(e: ExecutionException){
            chat.showMessage(ChatMessage(chat, e.message?: "Something went wrong"))
            throw GameExecutionException("Subgame failed")
        }
        return ShuffledDeck(deck, shuffled)
    }
}

data class ShuffledDeck(val originalDeck: Deck,val encryptedDeck: EncryptedDeck)