package apps.games

import apps.chat.Chat
import entity.ChatMessage
import entity.Group
import entity.User
import proto.GameMessageProto

/**
 * Created by user on 6/24/16.
 * @param chat - will contain logs and messages
 * @param group - group of active game participants(can ve cahnged during game)
 */

abstract class Game(internal val chat: Chat, internal val group: Group, val gameID: String){

    /**
     * Evaluate next game state based on responses from everyone
     * @param responses - results of previous state of all players
     */
    abstract fun evaluate(responses: List<GameMessageProto.GameStateMessage>) : String

    /**
     * We need to know, when to stop
     */
    abstract fun isFinished(): Boolean

    /**
     * Some other players might decide, that game
     * is over. Process their messages
     */
    open fun evaluateGameEnd(msg: GameMessageProto.GameEndMessage){
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
     * Some games might have endgame result.
     * E.G. reusable primitives
     */
    open fun getResult(): String{
        return ""
    }

    open fun getInitialMessage(): String{
        return "GL HF"
    }

    //TODO - fun to call nested games

    override fun equals(other: Any?): Boolean{
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Game

        if (gameID != other.gameID) return false

        return true
    }

    override fun hashCode(): Int{
        return gameID.hashCode()
    }


}