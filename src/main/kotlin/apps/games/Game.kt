package apps.games

import apps.chat.Chat
import apps.games.primitives.protocols.RandomNumberGame
import entity.ChatMessage
import entity.Group
import entity.User
import proto.GameMessageProto
import java.util.concurrent.Future

/**
 * Created by user on 6/24/16.
 * @param chat - will contain logs and messages
 * @param group - group of active game participants(can ve cahnged during game)
 */

abstract class Game<T>(internal val chat: Chat, internal val group: Group, val gameID: String){
    private var subGameCounter: Int = 0
    private var nestedGames: MutableList<GameResult<*>> = mutableListOf()
    /**
     * Evaluate next game state based on responses from everyone
     * @param responses - results of previous state of all players
     */
    abstract fun evaluate(responses: List<GameMessageProto.GameStateMessage>) : String

    /**
     * We need to know, when to stop
     */
    abstract fun isFinished(): Boolean

    abstract val name: String

    /**
     * Some other players might decide, that game
     * is over. Process their messages
     */
    open fun evaluateGameEnd(msg: GameMessageProto.GameEndMessage){
        group.users.remove(User(msg.user))
        if(msg.reason.isNotBlank()){
            chat.showMessage(ChatMessage(chat.chatId, User(msg.user), msg.reason))
        }
    }

    /**
     * Message to send other players after the game has ended
     */
    open fun getFinalMessage(): String{
        return "GGWP"
    }

    /**
     * Verifier for game end results(if needed)
     */
    open fun getVerifier(): String?{
        return null
    }

    /**
     * Some games might have endgame result.
     * E.G. reusable primitives
     */
    abstract fun getResult(): T

    /**
     * Contains of first message sent to other
     * players. Most typically -  simple
     * handshake
     */
    open fun getInitialMessage(): String{
        return "GL HF"
    }

    /**
     * Ids to call subgame. At one moment
     * game can have more then one
     * direct subgame
     */
    fun subGameID(): String{
        subGameCounter ++
        return gameID + subGameCounter.toInt()
    }

    /**
     * Init subgame.
     * @param game - game to start
     */
    fun <S> runSubGame(game: Game<S>): Future<S> {
        val result: Future<S> = GameManager.initSubGame(game)
        synchronized(nestedGames){
            nestedGames.add(GameResult(game, result))
        }
        chat.showMessage(ChatMessage(chat, "Initiated subgame: ${game.name}"))
        return result
    }

    /**
     * cancel all running subgames
     */
    @Synchronized fun cancelSubgames(){
        for(gameResult in nestedGames){
            if(!gameResult.game.isFinished() && !gameResult.result.isDone){
                gameResult.game.cancelSubgames()
                GameManager.deleteGame(gameResult.game)
                gameResult.result.cancel(true)
            }
        }

    }

    /**
     * Some protocols might want to send some
     * binary data. Then they need to override
     * this function
     */
    open fun getData(): List<ByteArray>{
        return listOf()
    }

    override fun equals(other: Any?): Boolean{
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Game<*>

        if (gameID != other.gameID) return false

        return true
    }

    override fun hashCode(): Int{
        return gameID.hashCode()
    }
}

/**
 * class for representation of game results(e.g. for nested games)
 */
class GameResult<T>(val game: Game<T>, val result: Future<T>){}