package apps.games.serious.Cheat

import org.apache.commons.codec.digest.DigestUtils
import java.math.BigInteger


import apps.games.GameExecutionException
import apps.games.serious.preferans.ShuffledDeck


/**
 * Created by user on 7/15/16.
 */

class RoundLogger(val N: Int,val  DECK_SIZE: Int,val shuffledDeck: ShuffledDeck): Cloneable{
    //map: position in shuffled Deck and user -> keys
    private val keyMap = Array(N, {i -> Array<BigInteger?>(DECK_SIZE, { j ->
        null})})



    //map hoe many turns won
    private val turnsWon: MutableMap<Int, Int> = mutableMapOf()



    /**
     * Register that
     * @param userID - ID of holder
     * @param cardID - position in shuffled deck
     * @param key - key
     */
    fun registerCardKey(userID: Int, cardID: Int, key: BigInteger): Boolean{
        if(keyMap[userID][cardID] == null){
            shuffledDeck.encrypted.deck.decryptCardWithKey(cardID, key)
        }
        keyMap[userID][cardID] = key
        return shuffledDeck.originalDeck.contains(shuffledDeck.encrypted.deck.cards[cardID])
    }

    fun formatLog(): String = ""

    override public fun clone(): RoundLogger {
        return super.clone() as RoundLogger
    }
}



class CheatGameLogger(val N: Int){
    private val pastLogs = mutableListOf<RoundLogger>()
    private var currentLogger: RoundLogger? = null
    val log: RoundLogger
        get() {
            if (currentLogger == null) {
                throw GameExecutionException("Acessing logger before initialization")
            }
            return currentLogger as RoundLogger
        }

    fun newRound(DECK_SIZE: Int, shuffledDeck: ShuffledDeck){
        if(currentLogger != null){
            pastLogs.add(currentLogger!!.clone())
        }
        currentLogger = RoundLogger(N, DECK_SIZE, shuffledDeck)
    }

    /**
     * format complete game log
     */
    fun formatLog(): String{
        return pastLogs.map { x -> x.formatLog() }.joinToString("=================")
    }
}