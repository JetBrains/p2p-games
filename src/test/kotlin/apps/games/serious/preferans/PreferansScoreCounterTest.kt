package apps.games.serious.preferans

import Settings
import entity.User
import org.junit.Assert.*
import org.junit.Test

/**
 * Created by user on 7/27/16.
 */
class PreferansScoreCounterTest {
    val user1 = User(Settings.hostAddress, "Alice")
    val user2 = User(Settings.hostAddress, "Bob")
    val user3 = User(Settings.hostAddress, "Charlie")


    /**
     * Simulate two rounds played in both rounds
     * first playerId takes 10 hands with other users just passing
     */
    @Test
    fun checkOneUserBullet() {
        val scoreCounter = PreferansScoreCounter(listOf(user1, user2, user3),
                maxBulletPerUser = 5)
        val handsTaken = mutableMapOf(user1 to 0, user2 to 0, user3 to 0)
        val whists = mapOf(user1 to Whists.UNKNOWN, user2 to Whists.PASS, user3 to Whists.PASS)
        scoreCounter.updateScore(handsTaken, Bet.TEN_NO_TRUMP, whists, user1)
        //Now user1 has 10 in bullet. game is played until 15
        assertFalse(scoreCounter.endOfGameReached())
        scoreCounter.updateScore(handsTaken, Bet.TEN_NO_TRUMP, whists, user1)
        //Now user1 has 20 in bullet.
        assertTrue(scoreCounter.endOfGameReached())
        assertEquals(20, scoreCounter.bullet[user1])
        assertEquals(0, scoreCounter.bullet[user2])
        assertEquals(0, scoreCounter.bullet[user3])
        assertEquals(0, scoreCounter.heap[user1])
        assertEquals(0, scoreCounter.heap[user2])
        assertEquals(0, scoreCounter.heap[user3])

        val res = scoreCounter.getFinalScores()
        assertEquals(266, res[user1])
        assertEquals(-133, res[user2])
        assertEquals(-133, res[user3])
    }

    /**
     * Simulate following game flow:
     * -user1 successfully played 10 NO TRUMP
     * -user2 successfully played 6 NO TRUMP
     * -user3 successfully played MIZER
     * -user1 successfully played 10 NO TRUMP
     */
    @Test
    fun checkEveryoneBullet() {
        val scoreCounter = PreferansScoreCounter(listOf(user1, user2, user3),
                maxBulletPerUser = 10)

        val handsTaken = mutableMapOf(user1 to 0, user2 to 0, user3 to 0)
        val whists = mapOf(user1 to Whists.PASS, user2 to Whists.PASS, user3 to Whists.PASS)
        scoreCounter.updateScore(handsTaken, Bet.TEN_NO_TRUMP, whists, user1)
        scoreCounter.updateScore(handsTaken, Bet.SIX_NO_TRUMP, whists, user2)
        scoreCounter.updateScore(handsTaken, Bet.MIZER, whists, user3)
        //Now user1 has 10 in bullet. user2 has 2, user3 has 10
        assertFalse(scoreCounter.endOfGameReached())
        scoreCounter.updateScore(handsTaken, Bet.TEN_NO_TRUMP, whists, user1)
        //Now user1 has 20 in bullet.
        assertTrue(scoreCounter.endOfGameReached())
        assertEquals(20, scoreCounter.bullet[user1])
        assertEquals(2, scoreCounter.bullet[user2])
        assertEquals(10, scoreCounter.bullet[user3])
        assertEquals(0, scoreCounter.heap[user1])
        assertEquals(0, scoreCounter.heap[user2])
        assertEquals(0, scoreCounter.heap[user3])

        val res = scoreCounter.getFinalScores()
        assertEquals(186, res[user1])
        assertEquals(-173, res[user2])
        assertEquals(-13, res[user3])
    }


    /**
     * Simulate game flow:
     * -user1 wants to play 10, takes 6
     * -user2 wants to play 6, takes 10
     * -user3 wants to play 9, takes 6
     * -user3 wants to play 9, takes 6
     * -everyone passed (2, 2, 6)
     * -everyone passed (3, 3, 4)
     * -everyone passed (0, 7, 3)
     * -user1 successfully played 10
     * -user1 successfully played 10
     * -user1 successfully played 10
     */
    @Test
    fun checkAllCells() {
        val scoreCounter = PreferansScoreCounter(listOf(user1, user2, user3),
                maxBulletPerUser = 10)

        val handsTaken = mutableMapOf(user1 to 0, user2 to 0, user3 to 0)
        val whists = mutableMapOf(user1 to Whists.PASS, user2 to Whists.WHIST_BLIND, user3 to Whists.PASS)
        //first playerId failed to play 10, took only 6
        handsTaken[user1] = 6
        handsTaken[user2] = 2
        handsTaken[user3] = 2
        scoreCounter.updateScore(handsTaken, Bet.TEN_NO_TRUMP, whists, user1)
        //user 2 said, he plays 6, both whisted, took 10
        handsTaken[user1] = 0
        handsTaken[user2] = 10
        handsTaken[user3] = 0
        whists[user2] = Whists.PASS
        whists[user1] = Whists.WHIST_BLIND
        whists[user3] = Whists.WHIST_BLIND
        scoreCounter.updateScore(handsTaken, Bet.SIX_NO_TRUMP, whists, user2)
        //first playerId failed to play 9, took only 6
        handsTaken[user1] = 2
        handsTaken[user2] = 2
        handsTaken[user3] = 6
        whists[user1] = Whists.PASS
        whists[user2] = Whists.WHIST_BLIND
        whists[user3] = Whists.PASS
        scoreCounter.updateScore(handsTaken, Bet.NINE_NO_TRUMP, whists, user3)
        //Now user1 has 10 in bullet. user2 has 2, user3 has 10
        assertFalse(scoreCounter.endOfGameReached())
        //First playerId won 30 two times in a row, ending the game
        handsTaken[user1] = 2
        handsTaken[user2] = 2
        handsTaken[user3] = 6
        whists[user1] = Whists.PASS
        whists[user2] = Whists.PASS
        whists[user3] = Whists.PASS
        scoreCounter.updateScore(handsTaken, Bet.PASS, null, null)
        handsTaken[user1] = 3
        handsTaken[user2] = 3
        handsTaken[user3] = 4
        scoreCounter.updateScore(handsTaken, Bet.PASS, null, null)
        handsTaken[user1] = 0
        handsTaken[user2] = 7
        handsTaken[user3] = 3
        scoreCounter.updateScore(handsTaken, Bet.PASS, null, null)
        scoreCounter.updateScore(handsTaken, Bet.TEN_NO_TRUMP, whists, user1)
        scoreCounter.updateScore(handsTaken, Bet.TEN_NO_TRUMP, whists, user1)
        scoreCounter.updateScore(handsTaken, Bet.TEN_NO_TRUMP, whists, user1)
        //Now user1 has 20 in bullet.
        assertTrue(scoreCounter.endOfGameReached())
        assertEquals(30, scoreCounter.bullet[user1])
        assertEquals(2, scoreCounter.bullet[user2])
        assertEquals(0, scoreCounter.bullet[user3])
        assertEquals(100, scoreCounter.heap[user1])
        assertEquals(58, scoreCounter.heap[user2])
        assertEquals(98, scoreCounter.heap[user3])
        assertEquals(80, scoreCounter.whists[user1 to user3])
        assertEquals(80, scoreCounter.whists[user2 to user3])
        assertEquals(120, scoreCounter.whists[user2 to user1])
        assertEquals(120, scoreCounter.whists[user3 to user1])

        val res = scoreCounter.getFinalScores()
        assertEquals(80, res[user1])
        assertEquals(300, res[user2])
        assertEquals(-380, res[user3])
    }


}