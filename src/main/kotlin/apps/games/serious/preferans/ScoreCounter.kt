package apps.games.serious.preferans

import apps.games.GameExecutionException
import com.sun.javaws.exceptions.InvalidArgumentException
import entity.User

/**
 * Created by user on 7/19/16.
 */

/**
 * Class for computing preferans score
 * (update heap/bullet/whists
 */
class PreferansScoreCounter(users: Collection<User>){
    val heap: MutableMap<User, Int> = mutableMapOf()
    val bullet: MutableMap<User, Int> = mutableMapOf()
    //Pair of <A, B> - how many whists A has on player B
    val whists: MutableMap<Pair<User, User>, Int> = mutableMapOf()

    init {
        for(user in users){
            heap[user] = 0
            bullet[user] = 0
            for(otherUser in users){
                whists[user to otherUser] = 0
            }
        }
    }
    private var subsequentPasses: Int = 0

    fun updateScore(handsTaken: Map<User, Int>, gameBet: Bet,
                    whists: Map<User, Whists>? = null, mainPlayer: User? = null){
        if(gameBet == Bet.PASS){
            if(subsequentPasses < 3){
                subsequentPasses ++
            }
            val handCost = subsequentPasses * 2
            for(user in handsTaken.keys){
                heap[user] = (heap[user] as Int) + handCost * (handsTaken[user] as Int)
            }
            return
        }
        if(mainPlayer == null || whists == null){
            throw GameExecutionException("Failed to agree")
        }

        subsequentPasses = 0

    }
}