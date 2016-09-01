package apps.games.serious.Cheat.logger

import apps.games.GameExecutionException
import apps.games.serious.Cheat.BetCount
import apps.games.serious.Pip
import apps.games.serious.ShuffledDeck
import entity.User
import org.apache.commons.codec.digest.DigestUtils
import java.math.BigInteger

/**
 * Created by user on 8/2/16.
 */

class RoundLogger(val N: Int, val deckSize: Int, val shuffledDeck: ShuffledDeck) : Cloneable {
    //map: position in shuffled Deck and user -> keys
    private val keyMap = Array(N, { i ->
        Array<BigInteger?>(deckSize, { j ->
            null
        })
    })

    private val stacks = mutableListOf<PlayStack>()
    private var currentStack = PlayStack(deckSize)

    /**
     * Register that
     * @param userID - ID of holder
     * @param cardID - position in shuffled deck
     * @param key - key
     * @return position of decrypted card in
     * original deck (-1 if abscent)
     */
    fun registerCardKey(userID: Int, cardID: Int, key: BigInteger): Int {
        if (keyMap[userID][cardID] == null) {
            shuffledDeck.encrypted.deck.decryptCardWithKey(cardID, key)
            keyMap[userID][cardID] = key
        } else {
            if (key != keyMap[userID][cardID]) {
                throw GameExecutionException("Attempt to override card key")
            }
        }
        return shuffledDeck.originalDeck.cards.indexOf(shuffledDeck.encrypted.deck.cards[cardID])
    }

    /**
     * Check if current playerId - firs playerId
     * in the new stack
     */
    fun isNewStack() = currentStack.isNewStack()

    fun stackFinished() = currentStack.isFinished()

    /**
     * Start next stack
     */
    fun nextStack() {
        stacks.add(currentStack)
        currentStack = PlayStack(deckSize)
    }

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
                              hashes: List<String>) {
        if (hashes.size != count.size) {
            throw GameExecutionException("Number of hashes in comitment doesn't" +
                    " correspond to claimed BetCount")
        }
        if (isNewStack()) {
            currentStack.setPip(claim)
        }
        currentStack.registerAddCards(user, count, hashes)
    }

    /**
     * @see [PlayStack.registerVerify]
     */
    fun registerVerify(user: User, cardPos: Int, cheat: Boolean) {
        currentStack.registerVerify(user, cardPos, cheat)
    }

    /**
     * @see [PlayStack.registerVerifyResponse]
     */
    fun registerVerifyResponse(user: User, response: String) {
        currentStack.registerVerifyResponse(user, response)
    }

    /**
     * see [PlayStack.checkRsponseCard]
     */
    fun checkPip(cardId: Int): Boolean {
        return currentStack.checkResponseCard(cardId)
    }

    /**
     * @see [PlayStack.countUserCardOnStack]
     */
    fun countUserCardOnStack(user: User): Int {
        return currentStack.countUserCardOnStack(user)
    }

    /**
     * @see [PlayStack.getLastUserBetCountSize]
     */
    fun getLastUserBetCountSize(user: User): Int {
        return currentStack.getLastUserBetCountSize(user)
    }

    /**
     * @see [PlayStack.nextPlayerToReveal]
     */
    fun nextPlayerToReveal(): User {
        return currentStack.nextPlayerToReveal()
    }

    /**
     * @see [PlayStack.nextNumberOfCardsToReveal]
     */
    fun nextNumberOfCardsToReveal(): Int {
        return currentStack.nextNumberOfCardsToReveal()
    }

    /**
     * @see [PlayStack.countRevealedCards]
     */
    fun countRevealedCards(user: User): Int {
        return currentStack.countRevealedCards(user)
    }

    /**
     * Register encrypted message sent by user
     */
    fun regiserEncryptedMessage(user: User, encrypt: List<String>) {
        if (user != nextPlayerToReveal()) {
            throw GameExecutionException("Unexpected encrypted message encountered")
        }
        currentStack.registerEncrypt(encrypt)
    }

    fun formatLog(): String = ""

    /**
     * Verify that all plays of the round were consistent
     *
     * @param userList - list of participants in the round
     * @param decoder - function that takes user and encrypted message
     * and returns decoded message for that user
     * @param winners - maps User to the fact whether he won
     */
    fun verifyRound(userList: List<User>,
                    decoder: (User, String) -> String,
                    winners: Map<User, Boolean>): Boolean {
        //Check, that nobody exchanged cards before the game started
        for (user in 0..N - 1) {
            if (getUserKeysHash(user) != shuffledDeck.encrypted.hashes[userList[user]]) {
                return false
            }
        }
        //restore play order to see, that all plays were consistent
        val cards = Array(N, { i -> mutableSetOf<Int>() })
        for (i in 0..deckSize - 1) {
            val holder = i % N
            cards[holder].add(i)
        }
        for (stack in stacks) {
            checkStack(userList, stack, cards, decoder)
        }
        val tmp = winners.mapKeys { x -> userList.indexOf(x.key) }
        return tmp.all { x -> x.value == cards[x.key].isEmpty() }
    }

    /**
     * Verify, that all plays of this stack are consistent.
     * Only hashes and card ownership is checked. Player order
     * is enforced by design: if al least one playerId is not cheating
     * his game will crush due to unexpected message
     *
     * @param userList - list of all users, who participate in the game
     * @param stack - stack to be verified
     * @param userCards - array of mutalbeSets, represents cards owned
     * by users at the moment
     * @param decoder - function that takes user and encrypted message
     * and returns decoded message for that user
     *
     * @return true if all playes were consistent. Otherwise - returns false
     */
    fun checkStack(userList: List<User>,
                   stack: PlayStack,
                   userCards: Array<MutableSet<Int>>,
                   decoder: (User, String) -> (String)): Boolean {

        val cards = mutableListOf<Int>()
        var receiverId: Int = userList.indexOf(stack.claims.last().user)
        if (!stack.guessedCorrect) {
            receiverId = (receiverId + 1) % N
        }
        for (i in 0..stack.size - 1) {
            val userId = userList.indexOf(stack.claims[i].user)
            for (j in 0..stack.claims[i].count.size - 1) {
                val s = decoder(userList[receiverId], stack.encrypts[i][j])
                if (DigestUtils.sha256Hex(s) != stack.claims[i].hashes[j]) {
                    return false
                }
                val card = s.split(" ")[0].toInt()
                val key = BigInteger(s.split(" ")[1])
                if (key != keyMap[userId][card]) {
                    return false
                }
                if (!userCards[userId].contains(card)) {
                    return false
                }
                userCards[userId].remove(card)
                cards.add(card)
            }
        }

        userCards[receiverId].addAll(cards)
        return true
    }

    /**
     * Calculate hash for user key set - used to
     * validate, that no cardID exchange cooperation was present
     * @param player - id of playerId, whose key hash is being calculated
     * @return Sting - resulting hash. Null if current information is
     * insuffitient to calculate requested hash
     */
    fun getUserKeysHash(player: Int): String? {
        if (keyMap[player].contains(null)) {
            return null
        }
        return DigestUtils.sha256Hex(keyMap[player].joinToString(" "))
    }

    override public fun clone(): RoundLogger {
        return super.clone() as RoundLogger
    }
}