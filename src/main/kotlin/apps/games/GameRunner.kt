package apps.games

import entity.User
import proto.GameMessageProto
import proto.GenericMessageProto
import java.util.concurrent.BlockingDeque
import java.util.concurrent.LinkedBlockingDeque

/**
 * Created by user on 6/24/16.
 */


class GameRunner(val game: Game): Runnable{
    val stateMessageQueue: BlockingDeque<GameMessageProto.GameStateMessage> = LinkedBlockingDeque()
    val endGameMessageQueue: BlockingDeque<GameMessageProto.GameEndMessage> = LinkedBlockingDeque()
    internal val nextStepMessages: MutableList<GameMessageProto.GameStateMessage> = mutableListOf()
    internal var state = 0


    /**
     * wait for all users to give game state update
     * or claim end of game
     */
    //TODO - timeout interruption
    fun getResponsePack(): List<GameMessageProto.GameStateMessage>{
        val found: MutableMap<User, GameMessageProto.GameStateMessage> = mutableMapOf()
        val pending: MutableSet<User> = mutableSetOf(*game.group.users.toTypedArray())
        while(pending.isNotEmpty()){
            //TODO interrupt, if someone ends game
            if(endGameMessageQueue.isNotEmpty()){
                val gameEndMessage = endGameMessageQueue.takeFirst()
                val user = User(gameEndMessage.user)
                synchronized(game.group){
                    game.group.users.remove(user)
                }
                pending.remove(user)
                continue
            }
            val msg = stateMessageQueue.takeFirst()
            if(msg.state > state + 1 || msg.state < state){
                throw GameStateExcetion("Impossible state message received")
            } else if(msg.state == state + 1){ //Someone already finished next step computations
                nextStepMessages.add(msg)
            } else{
                val user = User(msg.user)
                if(pending.contains(user)){
                    pending.remove(user)
                }
                found.put(user, msg)
            }
        }
        state ++
        return found.values.toMutableList()
    }

    /**
     * broadcast result of game state computation
     */
    fun sendResponse(response: String){
        val msg = GameMessageProto.GameStateMessage
                .newBuilder()
                .setGameID(game.gameID)
                .setState(state)
                .setUser(User(Settings.hostAddress, game.chat.username).getProto())
                .setValue(response).build()
        val gameMessage = GameMessageProto.GameMessage
                .newBuilder()
                .setType(GameMessageProto.GameMessage.Type.GAME_STATE_MESSAGE)
                .setGameStateMessage(msg)
        val genericMessage = GenericMessageProto.GenericMessage
                .newBuilder()
                .setType(GenericMessageProto.GenericMessage.Type.GAME_MESSAGE)
                .setGameMessage(gameMessage)
                .build()
        game.chat.groupBroker.broadcast(game.group, genericMessage)
    }

    override fun run() {
        //send initial info
        sendResponse("GOGOGO")
        //check response
        while(!game.isFinished()){
            val computed = game.evaluate(getResponsePack())
            sendResponse(computed)
            stateMessageQueue.addAll(nextStepMessages)
            nextStepMessages.clear()
        }
        GameManager.sendEndGame(game.gameID,
                "[${game.chat.username}] finished playing! Final result: ${game.getFinalResult()}")
    }

}