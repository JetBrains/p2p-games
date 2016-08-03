package apps.games.serious.Cheat.logger

import apps.games.GameExecutionException
import apps.games.serious.Cheat.BetCount
import apps.games.serious.Pip
import apps.games.serious.preferans.ShuffledDeck
import entity.User
import java.math.BigInteger

/**
 * Created by user on 8/2/16.
 */

class RoundLogger(val N: Int,val  DECK_SIZE: Int,val shuffledDeck: ShuffledDeck): Cloneable{
    //map: position in shuffled Deck and user -> keys
    private val keyMap = Array(N, {i -> Array<BigInteger?>(DECK_SIZE, { j ->
        null})})

    private val stacks = listOf<PlayStack>()
    private var currentStack = PlayStack(DECK_SIZE)

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
            keyMap[userID][cardID] = key
        }else{
            if (key != keyMap[userID][cardID]){
                throw GameExecutionException("Attempt to override card key")
            }
        }
        return shuffledDeck.originalDeck.cards.indexOf(shuffledDeck.encrypted.deck.cards[cardID])
    }

    /**
     * Check if current player - firs player
     * in the new stack
     */
    fun isNewStack() = currentStack.isNewStack()

    fun stackFinished() = currentStack.isFinished()


    /**
     * Register hash part of commitment scheme
     * used for adding cards.
     * @param user - actor, that played cards
     * @param count - BetCount of cards played
     * @param claim - claimed Pip of all cards
     * @param hashes - hashesh of cards played
     */
    fun registerAddPlayHashes(user: User, count: BetCount,
                              claim: Pip = Pip.UNKNOWN,
                              hashes: List<String>){
        if(hashes.size != count.size){
            throw GameExecutionException("Number of hashes in comitment doesn't" +
                                         " correspond to claimed BetCount")
        }
        if(isNewStack()){
            currentStack.setPip(claim)
        }
        currentStack.registerAddCards(user, count, hashes)
    }

    /**
     * @see [PlayStack.registerVerify]
     */
    fun registerVerify(user: User, cardPos: Int, cheat: Boolean){
        currentStack.registerVerify(user, cardPos, cheat)
    }

    /**
     * @serr [PlayStack.registerVerifyResponse]
     */
    fun registerVerifyResponse(user: User, response: String): Boolean{
        return currentStack.registerVerifyResponse(user, response)
    }

    /**
     * @see [PlayStack.countUserCardOnStack]
     */
    fun countUserCardOnStack(user: User): Int{
        return currentStack.countUserCardOnStack(user)
    }

    /**
     * @see [PlayStack.getLastUserBetCountSize]
     */
    fun getLastUserBetCountSize(user: User) : Int{
        return currentStack.getLastUserBetCountSize(user)
    }

    /**
     * @see [PlayStack.nextPlayerToReveal]
     */
    fun nextPlayerToReveal(): User{
        return currentStack.nextPlayerToReveal()
    }

    /**
     * @see [PlayStack.nextNumberOfCardsToReveal]
     */
    fun nextNumberOfCardsToReveal(): Int{
        return currentStack.nextNumberOfCardsToReveal()
    }

    /**
     * @see [PlayStack.countRevealedCards]
     */
    fun countRevealedCards(user: User): Int{
        return currentStack.countRevealedCards(user)
    }

    /**
     * Register encrypted message sent by user
     */
    fun regiserEncryptedMessage(user: User, encrypt: List<String>){
        if(user != nextPlayerToReveal()){
            throw GameExecutionException("Unexpected encrypted message encountered")
        }
        currentStack.registerEncrypt(encrypt)
    }

    fun formatLog(): String = ""

    override public fun clone(): RoundLogger {
        return super.clone() as RoundLogger
    }
}