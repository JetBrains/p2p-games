package apps.games.serious.preferans

import apps.games.GameExecutionException
import com.sun.javaws.exceptions.InvalidArgumentException
import entity.User
import java.util.*

/**
 * Created by user on 7/19/16.
 */

/**
 * Class for computing preferans score
 * (update heap/bullet/whists)
 * @param users - list of users who participate in this game
 * @param maxBulletSum - maximum bullet per player. Game will not stop
 * @param heapMultiplier - heap to whist ratio
 * @param bulletMultiplier - bullet to heap ratio
 * until total bullet is [maxBulletSum] * [N], where [N] - number of
 * players
 */
class PreferansScoreCounter(val users: List<User>, val maxBulletSum: Int = 5,
                            val heapMultiplier: Int = 10,
                            val bulletMultiplier: Int = 2){
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

    /**
     * Update score after the end of round
     * @param handsTaken - Map<User, Int>. Describes number
     * of hands taken by each user
     * @param gameBet - Bet, that was agreed upon in this round
     * @param whists - Map<User, Whists>? - describes whist status
     * of each user(might be null, if this doesn't apply)
     * @param mainPlayer - User? - holder of the best contract(or
     * null if no such user)
     */
    fun updateScore(handsTaken: Map<User, Int>, gameBet: Bet,
                    whists: Map<User, Whists>? = null, mainPlayer: User? = null){
        if(gameBet == Bet.PASS){
            allPassed(handsTaken)
            return
        }
        subsequentPasses = 0
        if(mainPlayer == null || whists == null){
            throw GameExecutionException("Failed to agree")
        }

        //mizer
        if(gameBet == Bet.MIZER){
            mizer(handsTaken, mainPlayer)
            return
        }

        //Half-whist
        if(whists.containsValue(Whists.WHIST_HALF)){
            halfWhisted(gameBet, whists, mainPlayer)
        }

        //Ordinary contract
        var totalWhisted: Int = 0
        val whisters = mutableListOf<User>()
        val passed = mutableListOf<User>()
        for(user in users){
            if(user != mainPlayer){
                totalWhisted += (handsTaken[user] as Int)
                if(whists[user] != Whists.PASS){
                    whisters.add(user)
                }else{
                    passed.add(user)
                }
            }
        }

        when(whisters.size){
            0 -> agreed(gameBet, mainPlayer)
            1 -> oneWhisted(gameBet, handsTaken, mainPlayer, whisters, passed)
            2 -> bothWhisted(gameBet, handsTaken, mainPlayer, whisters)
        }
    }

    /**
     * Check whether bullet sum exceeded end of game
     * value [maxBulletSum]
     */
    fun endOfGameReached(): Boolean{
        return bullet.values.sum() >= maxBulletSum * users.size
    }

    fun getFinalScores(): Map<User, Int>{
        val result = mutableMapOf<User, Int>()
        for(user1 in users){
            result[user1] = 0
        }
        val tBullet = HashMap(bullet)
        var tHeap = HashMap(heap)
        for(user in users){
            if(!tBullet.containsKey(user)){
                throw IllegalArgumentException("Can not compute score. Check, that game is valid")
            }
            tBullet[user] = (tBullet[user] as Int) - maxBulletSum
            tHeap[user] = (tHeap[user] as Int) - (tBullet[user] as Int) *
                    bulletMultiplier
        }
        val minHeap: Int = tHeap.values.min() ?: throw IllegalArgumentException("Can not compute score. Check, that game is valid")
        tHeap = HashMap(tHeap.mapValues { x -> x.value - minHeap })
        val avgHeap = tHeap.values.average()
        val resHeap = tHeap.mapValues { x ->  avgHeap - x.value}
        for(user1 in users){
            result[user1] = (result[user1] as Int) + ((resHeap[user1] as Double) * heapMultiplier).toInt()
            for(user2 in users){
                result[user1] = (result[user1] as Int) +
                                (whists[user1 to user2] as Int) -
                                (whists[user2 to user1] as Int)
            }
        }
        return result
    }

    /**
     * Update scores if everyone passed
     * Arithmetic progression of hand costs
     * @param handsTaken - Map<User, Int>. Describes number
     * of hands taken by each user
     */
    private fun allPassed(handsTaken: Map<User, Int>){
        if(subsequentPasses < 3){
            subsequentPasses ++
        }
        val handCost = subsequentPasses * 2
        for(user in users){
            heap[user] = (heap[user] as Int) + handCost * (handsTaken[user] as Int)
        }
    }


    /**
     * update scores if someone is playing mizer contract
     * @param handsTaken - Map<User, Int>. Describes number
     * of hands taken by each user
     * @param mainPlayer - holder of mizer contract
     */
    private fun mizer(handsTaken: Map<User, Int>, mainPlayer: User){
        val penalty: Int = (handsTaken[mainPlayer] as Int) * Bet.MIZER.penalty
        heap[mainPlayer] = (heap[mainPlayer] as Int) + penalty
        if(handsTaken[mainPlayer] == 0){
            bullet[mainPlayer] = (bullet[mainPlayer] as Int) + Bet.MIZER.bullet

        }
    }

    /**
     * update scores if everyone agreed with contract(passed)
     * @param gameBet - highest contract
     * @param mainPlayer - holder of gameBet
     */
    private fun agreed(gameBet: Bet, mainPlayer: User){
        bullet[mainPlayer] = (bullet[mainPlayer] as Int) + gameBet.bullet
    }


    /**
     * update scores if everyone agreed with contract(one player passed,
     * another half-whisted)
     * @param gameBet - highest contract
     * @param mainPlayer - holder of gameBet
     */
    private fun halfWhisted(gameBet: Bet, whists: Map<User, Whists>, mainPlayer: User){
        val user = whists.filterKeys { x -> whists[x] == Whists.WHIST_HALF }.keys.first()
        val gameBonus = (gameBet.whistNorm / 2) * gameBet.whistBounty
        this.whists[user to mainPlayer] = (this.whists[user to mainPlayer] as Int) + gameBonus
        bullet[mainPlayer] = (bullet[mainPlayer] as Int) + gameBet.bullet
    }


    /**
     * update scores if one player passed and second
     * player whisted contract
     * @param gameBet - highest contract
     * @param handsTaken - Map<User, Int>, describes number
     * if hands taken by each player
     * @param mainPlayer - holder of gameBet contract
     * @param whisters - list of users, that whisted(should contain only one
     * user for 3-player game)
     * @param passed - list of users, that passed(should contain only one
     * user for 3-player game)
     */
     private fun oneWhisted(gameBet: Bet, handsTaken: Map<User, Int>,
                           mainPlayer: User, whisters: List<User>, passed: List<User>){
        if(whisters.size != 1 || passed.size != 1){
            throw IllegalArgumentException("One player must pass, another whist")
        }
        val whister = whisters[0]
        val pass = passed[0]

        if((handsTaken[mainPlayer] as Int) >= gameBet.contract){
            bullet[mainPlayer] = (bullet[mainPlayer] as Int) + gameBet.bullet
        }else{
            val remiz = gameBet.contract - (handsTaken[mainPlayer] as Int)
            heap[mainPlayer] = (heap[mainPlayer] as Int) + remiz * gameBet.penalty
        }

        if(!handsTaken.containsKey(whister) || !handsTaken.containsKey(pass)){
            throw IllegalArgumentException("Not enough data on  hands taken")
        }
        val totalWhisted = (handsTaken[whister] as Int) + (handsTaken[pass] as Int)
        if(totalWhisted <= gameBet.whistNorm){
            val penalty = (gameBet.whistNorm - totalWhisted) * gameBet.whistPenalty
            heap[whister] = (heap[whister] as Int) + penalty
            whists[whister to mainPlayer] = (whists[whister to mainPlayer] as Int) +
                                             totalWhisted * gameBet.whistBounty
        }else{
            var bounty: Int = gameBet.whistBounty * totalWhisted
            if((handsTaken[mainPlayer] as Int) < gameBet.contract){
                bounty += (gameBet.contract- (handsTaken[mainPlayer] as Int)) * 2 * gameBet.whistBounty
            }
            whists[whister to mainPlayer] = (whists[whister to mainPlayer] as Int) + bounty / 2
            whists[pass to mainPlayer] = (whists[pass to mainPlayer] as Int)+ bounty / 2
        }

    }

    /**
     * update scores if both players pwhisted contract
     * @param gameBet - highest contract
     * @param handsTaken - Map<User, Int>, describes number
     * if hands taken by each player
     * @param mainPlayer - holder of gameBet contract
     * @param whisters - list of users, that whisted(should contain two
     * user for 3-player game)
     */
    private fun bothWhisted(gameBet: Bet, handsTaken: Map<User, Int>,
                            mainPlayer: User, whisters: List<User>){
        if(whisters.size != 2){
            throw IllegalArgumentException("One player must pass, another whist")
        }
        val whister1 = whisters[0]
        val whister2 = whisters[1]

        if((handsTaken[mainPlayer] as Int) >= gameBet.contract){
            bullet[mainPlayer] = (bullet[mainPlayer] as Int) + gameBet.bullet
        }else{
            val remiz = gameBet.contract - (handsTaken[mainPlayer] as Int)
            heap[mainPlayer] = (heap[mainPlayer] as Int) + remiz * gameBet.penalty
        }

        if(!handsTaken.containsKey(whister1) || !handsTaken.containsKey(whister2)){
            throw IllegalArgumentException("Not enough data on  hands taken")
        }
        val norm1 = gameBet.whistNorm / 2
        val norm2 = gameBet.whistNorm - norm1
        var consolation = (gameBet.contract - (handsTaken[mainPlayer] as Int)) * 2 * gameBet.whistBounty
        if(consolation < 0){
            consolation = 0
        }
        whists[whister1 to mainPlayer] = (whists[whister1 to mainPlayer] as Int) +
                (handsTaken[whister1] as Int) * gameBet.whistBounty + consolation / 2
        if((handsTaken[whister1] as Int) <= norm1){
            val penalty = (norm1 - (handsTaken[whister1] as Int)) * gameBet.whistPenalty
            heap[whister1] = (heap[whister1] as Int) + penalty

        }

        whists[whister2 to mainPlayer] = (whists[whister2 to mainPlayer] as Int) +
                (handsTaken[whister2] as Int) * gameBet.whistBounty + consolation / 2
        if((handsTaken[whister2] as Int) <= norm2){
            val penalty = (norm2 - (handsTaken[whister2] as Int)) * gameBet.whistPenalty
            heap[whister2] = (heap[whister2] as Int) + penalty

        }
    }

    override fun toString(): String {
        return "${heap.values.first()}"
    }
}