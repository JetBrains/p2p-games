package apps.games.serious.Cheat.logger

import apps.games.GameExecutionException
import apps.games.serious.Cheat.BetCount
import apps.games.serious.Pip
import apps.games.serious.getPipsInDeck
import entity.User

/**
 * Created by user on 8/2/16.
 *
 * class descripts stack of plays:
 * plays stack untill someone verifies cards on top of the stack
 */

class PlayStack {
    private var pip = Pip.UNKNOWN
    data class Claim(val user: User, val count: BetCount, val hashes: List<String>)
    private val encryptedCards = mutableListOf<Claim>()

    /**
     * Check if it is a fresh stack. If it is -
     * we need to initialize pip before adding data
     */
    fun isNewStack(): Boolean = encryptedCards.isEmpty()

    /**
     * Initialize pip
     * @param pip - new pip of stack
     */
    fun setPip(pip: Pip){
        if(this.pip != Pip.UNKNOWN){
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
    fun registerAddCards(user: User, count: BetCount, hashes: List<String>){
        if(count.size != hashes.size){
            throw GameExecutionException("Number of hashes in comitment doesn't" +
                    " correspond to claimed BetCount")
        }
        encryptedCards.add(Claim(user, count, hashes))
    }

}

