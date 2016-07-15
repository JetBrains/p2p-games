package apps.games.serious.preferans

import java.math.BigInteger

/**
 * Created by user on 7/15/16.
 */

class RoundLogger(val N: Int,val  DECK_SIZE: Int,val  TALON_SIZE: Int){
    private val keyMap = Array(N, {i -> Array<BigInteger?>(DECK_SIZE, {j ->
        null})})

    private var gameType: Bet = Bet.UNKNOWN

    private var talon = Array(TALON_SIZE, {i -> -1})
    //todo - check talon. only main player can play it's cards

    private var fail: Boolean = false

    // log of plays <user, card>
    private val log = mutableListOf<Pair<Int, Int>>()

    /**
     * Register that
     * @param userID - user with this ID
     * @param card - holds for this card
     * (position in shuffled deck)
     * @param key - has that key
     */
    fun registerCardKey(userID: Int, card: Int, key: BigInteger){
        keyMap[userID][card] = key
    }

    /**
     * register one turn
     * @param cards - cards
     */
    fun regitsterPlay(player: Int, card: Int){
        log.add(player to card)
    }
}

class GameLogger(val N: Int,val  DECK_SIZE: Int,val  TALON_SIZE: Int){
    private val pastLogs = mutableListOf<RoundLogger>()
    var log = RoundLogger(N, DECK_SIZE, TALON_SIZE)

    fun newRound(){
        pastLogs.add(log)
        log = RoundLogger(N, DECK_SIZE,TALON_SIZE)
    }
}