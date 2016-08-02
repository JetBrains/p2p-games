package apps.games.serious.Cheat.logger

import apps.games.serious.preferans.ShuffledDeck
import java.math.BigInteger

/**
 * Created by user on 8/2/16.
 */

class RoundLogger(val N: Int,val  DECK_SIZE: Int,val shuffledDeck: ShuffledDeck): Cloneable{
    //map: position in shuffled Deck and user -> keys
    private val keyMap = Array(N, {i -> Array<BigInteger?>(DECK_SIZE, { j ->
        null})})

    private val stacks = listOf<PlayStack>()
    private var currentStack = PlayStack()

    /**
     * Register that
     * @param userID - ID of holder
     * @param cardID - position in shuffled deck
     * @param key - key
     * @return position of decrypted card in
     * original deck (-1 if abscent)
     */
    fun registerCardKey(userID: Int, cardID: Int, key: BigInteger): Int{
        if(keyMap[userID][cardID] == null){
            shuffledDeck.encrypted.deck.decryptCardWithKey(cardID, key)
        }
        keyMap[userID][cardID] = key
        return shuffledDeck.originalDeck.cards.indexOf(shuffledDeck.encrypted.deck.cards[cardID])
    }

    /**
     * Check if current player - firs player
     * in the new stack
     */
    fun isNewStack() = currentStack.isNewStack()


    fun formatLog(): String = ""

    override public fun clone(): RoundLogger {
        return super.clone() as RoundLogger
    }
}