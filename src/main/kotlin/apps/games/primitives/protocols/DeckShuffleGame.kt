package apps.games.primitives.protocols

import apps.chat.Chat
import apps.games.Game
import apps.games.GameExecutionException
import apps.games.primitives.Deck
import apps.games.primitives.EncryptedDeck
import crypto.random.randomBigInt
import entity.Group
import entity.User
import org.bouncycastle.jce.spec.ECParameterSpec
import proto.GameMessageProto
import java.math.BigInteger

/**
 * Created by user on 7/4/16.
 *
 * Class describes protocol for Shuffling privided
 * deck
 *
 * Algorithm has four stages
 *
 * INIT - all players ensure, everyone has the same deck
 *
 * SHUFFLE, LOCK - create shuffle deck using algorithms from
 * http://www.clee.kr/thesis.pdf
 *
 * VALIDATE - ensure everyone got the same result
 *
 * END - finish the game
 */


class DeckShuffleGame(chat: Chat, group: Group, gameID: String, val ECParams: ECParameterSpec, val deck: Deck) : Game<EncryptedDeck>(chat, group, gameID) {
    override val name: String
        get() = "Card Shuffle"

    private enum class State{

        INIT,
        SHUFFLE,
        LOCK,
        VALIDATE,
        END
    }
    private var state: State = State.INIT
    private var step: Int = -1
    //to sorted array to preserve order
    private val players: List<User> = group.users.sortedBy { x -> x.name }
    private val id = players.indexOf(chat.me())
    private val N = players.size

    //key for the first iteration
    private val shuffleKey: BigInteger = randomBigInt(ECParams.n)
    //keys for second iteration
    private val lockKeys: List<BigInteger> = listOf(*Array(deck.size, { i -> randomBigInt(ECParams.n)}))

    override fun isFinished(): Boolean {
        return state == State.END
    }

    override fun getInitialMessage(): String {
        return deck.hashCode().toString() + " " + players.hashCode()
    }

    override fun evaluate(responses: List<GameMessageProto.GameStateMessage>): String {
        when(state){
            State.INIT -> {
                val hashes = responses.distinctBy {x -> x.value}
                if(hashes.size != 1){
                    throw GameExecutionException("Someone has different deck")
                }
                state = State.SHUFFLE
                return ""
            }
            State.SHUFFLE -> {
                for(msg in responses){
                    if(players.indexOf(User(msg.user)) == step){
                        if(msg.dataCount != deck.size){
                            throw GameExecutionException("Someone failed to provide their deck")
                        }
                        for(i in 0..deck.size-1){
                            deck.cards[i] = ECParams.curve.decodePoint(msg.dataList[i].toByteArray())
                        }
                    }
                }
                step ++
                if(step == id){
                    deck.shuffle()
                    deck.encrypt(shuffleKey)
                }
                if(step > N){
                    step = -1
                    state = State.LOCK
                }
            }
            State.LOCK -> {
                for(msg in responses){
                    if(players.indexOf(User(msg.user)) == step){
                        if(msg.dataCount != deck.size){
                            throw GameExecutionException("Someone failed to provide their deck")
                        }
                        for(i in 0..deck.size-1){
                            deck.cards[i] = ECParams.curve.decodePoint(msg.dataList[i].toByteArray())
                        }
                    }
                }
                step ++
                if(step == id){
                    deck.decrypt(shuffleKey)
                    deck.enctyptSeparate(lockKeys)
                }
                if(step > N){
                    state = State.VALIDATE
                    return deck.hashCode().toString()
                }
            }
            State.VALIDATE -> {
                val hashes = responses.map { x -> x.value }
                if(hashes.distinct().size != 1){
                    throw GameExecutionException("Someone has a different deck")
                }
                state = State.END
            }
            State.END -> {}
        }
        return ""
    }

    /**
     * return a list of encrypted cards if needed
     */
    override fun getData(): List<ByteArray> {
        if(step == id){
            return listOf(*deck.cards.map { x -> x.getEncoded(false) }.toTypedArray())
        }else{
            return listOf()
        }
    }

    /**
     * our shuffled and encrypted deck is our result
     */
    override fun getResult(): EncryptedDeck {
        return EncryptedDeck(deck, lockKeys)
    }

    override fun getFinalMessage(): String {
        return "Everything appears to be OK. My shuffled and encrypted deck is: \n ${deck.toString()}"
    }
}

