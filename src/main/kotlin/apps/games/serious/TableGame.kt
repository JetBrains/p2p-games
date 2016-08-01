package apps.games.serious

import apps.chat.Chat
import apps.games.Game
import apps.games.GameManager
import apps.games.GameManagerClass
import entity.Group
import entity.User

/**
 * Created by user on 7/29/16.
 *
 * Base class for various utils used in many talbe games
 */

abstract class TableGame(chat: Chat, group: Group, gameID: String,
                         gameManager: GameManagerClass = GameManager) : Game<Unit>(chat, group, gameID, gameManager){
    //to sorted array to preserve order
    protected val playerOrder: MutableList<User> = group.users.sortedBy { x -> x.name }.toMutableList()
    protected var playerID = playerOrder.indexOf(chat.me())

    //player whose turn is right now
    protected var currentPlayerID: Int = 0

    /**
     * return nothing
     */
    override fun getResult() {
        return Unit
    }

    /**
     * get ID of user in playerOrder
     */
    fun getUserID(user: User): Int {
        return playerOrder.indexOf(user)
    }


    /**
     * During the game in GUI - ew are always player 0,
     * meanwhile in game we are not
     */
    fun getTablePlayerId(id: Int): Int {
        var res: Int = id - playerID
        if (res < 0) {
            res += group.users.size
        }
        return res
    }

}