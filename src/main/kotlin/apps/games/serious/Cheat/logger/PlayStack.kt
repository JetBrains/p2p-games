package apps.games.serious.Cheat.logger

import apps.games.GameExecutionException
import apps.games.serious.Cheat.BetCount
import apps.games.serious.Pip
import apps.games.serious.getCardById
import entity.User
import org.apache.commons.codec.digest.DigestUtils

/**
 * Created by user on 8/2/16.
 *
 * class descripts stack of plays:
 * plays stack untill someone verifies cards on top of the stack
 */

class PlayStack(val deckSize: Int) {
    private var pip = Pip.UNKNOWN

    class Claim(val user: User, val count: BetCount, val hashes: List<String>)

    val claims = mutableListOf<Claim>()
    val encrypts = mutableListOf<List<String>>()
    val size: Int
        get() = claims.size
    private lateinit var verifierPlayer: User
    private var verificationCard: Int = -1
    private var cheat: Boolean = false
    var guessedCorrect: Boolean = false
    /**
     * Check if it is a fresh stack. If it is -
     * we need to initialize pip before adding data
     */
    fun isNewStack(): Boolean = claims.isEmpty()

    /**
     * Check whether adding phase is completed,
     * revealing cards might me in process
     */
    fun isInRevealPhase(): Boolean = verificationCard != -1

    /**
     * Check, that Reveal Phase has ended (we got enctrypted
     * messages for all entries created on adding phase)
     */
    fun isFinished(): Boolean = claims.size > 0 && claims.size == encrypts.size && isInRevealPhase()

    /**
     * Initialize pip
     * @param pip - new pip of stack
     */
    fun setPip(pip: Pip) {
        if (this.pip != Pip.UNKNOWN) {
            throw GameExecutionException("Can not reassign pip")
        }
        this.pip = pip
    }

    /**
     * Register hashes of cards
     * played by user to stack
     * @param user - actor of played cards
     * @param count - BetCount of cards played
     * @param hashes - salted hashes of played
     * cards(commitment scheme)
     */
    fun registerAddCards(user: User, count: BetCount, hashes: List<String>) {
        if (count.size != hashes.size) {
            throw GameExecutionException("Number of hashes in comitment doesn't" +
                    " correspond to claimed BetCount")
        }
        claims.add(Claim(user, count, hashes))
    }

    /**
     * Count how many cards was played
     * by specified user on this stack
     *
     * @param user - whose cards to count
     */
    fun countUserCardOnStack(user: User): Int {
        return claims.filter { x -> x.user == user }.sumBy { x -> x.count.size }
    }

    /**
     * Count how many cards was played
     * by specified user on his last turn
     *
     * @param user - whose cards to count
     *
     * @return number of cards played on
     * his last turn. -1 if it is his first turn
     */
    fun getLastUserBetCountSize(user: User): Int {
        val temp = claims.lastOrNull { x -> x.user == user } ?: return -1
        return temp.count.size
    }

    /**
     * Register verifier player
     *
     * @param user - User who claimed cheat/believe
     * @param cardPos - position of card to be verified
     * @param cheat - whether user wants to confirm or refute
     */
    fun registerVerify(user: User, cardPos: Int, cheat: Boolean) {
        if (isInRevealPhase()) {
            throw GameExecutionException("Verifier already registered")
        }
        if (user != claims.last().user) {
            throw GameExecutionException("Someone else response instead of last player")
        }
        if (cardPos < 0 || cardPos >= claims.last().count.size) {
            throw GameExecutionException("Verification card index out of range")
        }
        verifierPlayer = user
        verificationCard = cardPos
        this.cheat = cheat
    }

    /**
     * Register response provided by verifier user
     *
     * @param user who was sending verification message
     * @param response - what he sent
     */
    fun registerVerifyResponse(user: User, response: String) {
        if (verifierPlayer != user) {
            throw GameExecutionException("Someone else response instead of last player")
        }
        val hash = DigestUtils.sha256Hex(response)
        if (hash != claims.last().hashes[verificationCard]) {
            throw GameExecutionException("Invalid card revealed during verificatoin")
        }
    }

    /**
     * @return True, if verification was successful,
     * i.e cheat was claimed and found or
     * player was right to believe
     */
    fun checkResponseCard(cardId: Int): Boolean {
        guessedCorrect = (getCardById(cardId, deckSize).pip == pip) xor cheat
        return guessedCorrect
    }

    /**
     * Get next user, who should decrypt his played cards
     */
    fun nextPlayerToReveal(): User {
        if (encrypts.size < claims.size) {
            return claims[encrypts.size].user
        }
        throw GameExecutionException("Everybody already revealed their cards")
    }

    /**
     * Number of cards, that should be revealed by next user
     */
    fun nextNumberOfCardsToReveal(): Int {
        if (encrypts.size < claims.size) {
            return claims[encrypts.size].count.size
        }
        throw GameExecutionException("Everybody already revealed their cards")
    }

    /**
     * register next enccrypt and put it on top of the stack
     *
     * @param encrypt - list of encrypted messaged correspondint
     * to hashes sent in adding phase
     */
    fun registerEncrypt(encrypt: List<String>) {
        if (isFinished()) {
            throw GameExecutionException("Stack already finished")
        }
        if (claims[encrypts.size].count.size != encrypt.size) {
            throw GameExecutionException("Number of encrypted cards doesn't match earlier claim")
        }
        encrypts.add(encrypt)
    }

    /**
     * Count number of cards already revealed by user
     *
     * @param user - whose cards to count
     */
    fun countRevealedCards(user: User): Int {
        var res = 0
        for (i in 0..encrypts.size - 1) {
            if (claims[i].user == user) {
                res += claims[i].count.size
            }
        }
        return res
    }

}

