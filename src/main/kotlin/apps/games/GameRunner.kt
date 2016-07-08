package apps.games

import com.google.protobuf.ByteString
import entity.ChatMessage
import entity.User
import org.apache.commons.collections4.map.LRUMap
import proto.GameMessageProto
import proto.GenericMessageProto
import proto.QueryProto
import sun.misc.LRUCache
import java.util.*
import java.util.concurrent.BlockingDeque
import java.util.concurrent.Callable
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit

/**
 * Created by user on 6/24/16.
 */


class GameRunner<T>(val game: Game<T>, val maxRetires:Int = 5): Callable<T>{

    val stateMessageQueue: BlockingDeque<GameMessageProto.GameStateMessage> = LinkedBlockingDeque()
    val endGameMessageQueue: BlockingDeque<GameMessageProto.GameEndMessage> = LinkedBlockingDeque()
    internal val nextStepMessages: MutableList<GameMessageProto.GameStateMessage> = mutableListOf()
    internal var timestamp = 0
    var messageLog = LRUMap<Int, GenericMessageProto.GenericMessage>(100)


    /**
     * wait for all users to give game state update
     * or claim end of game
     */
    fun getResponsePack(retries: Int = maxRetires): List<GameMessageProto.GameStateMessage>{
        val found: MutableMap<User, GameMessageProto.GameStateMessage> = mutableMapOf()
        val pending: MutableSet<User> = mutableSetOf(*game.group.users.toTypedArray())
        var failures = 0
        while(pending.isNotEmpty() && failures < retries){
            val msg = stateMessageQueue.pollFirst(200, TimeUnit.MILLISECONDS)
            if(msg == null){
                failures ++
                continue
            }
            if(msg.timestamp == timestamp){
                val user = User(msg.user)
                if(pending.contains(user)){
                    pending.remove(user)
                }
                found.put(user, msg)
            }else if(msg.timestamp == timestamp + 1){
                nextStepMessages.add(msg)
            }else{
                System.err.println("Warning: message from past received")
            }
        }
        for(i in 1..maxRetires){
            for(user in pending.toList()){
                val genericMessage = game.gameManager.requestUpdate(user, game, timestamp) ?: continue
                val response = genericMessage.responseGroup.responseList[0].gameMessage
                if(response.hasGameErrorMessage()){
                    continue
                }
                val msg = response.gameStateMessage
                if(msg.timestamp == timestamp){
                    val user2 = User(msg.user)
                    if(pending.contains(user2)){
                        pending.remove(user2)
                    }
                    found.put(user2, msg)
                }else{
                    System.err.println("Warning: [${game.chat.me().name}] User ${user.name} is at ${msg.timestamp} while I am at $timestamp")
                }
            }
            if(pending.isEmpty()){
                break
            }
            Thread.sleep(500)
        }
        timestamp++
        return found.values.toMutableList()
    }

    /**
     * broadcast result of game state computation
     */
    @Synchronized fun sendResponse(response: String, data: List<ByteArray>){
        val msg = GameMessageProto.GameStateMessage
                .newBuilder()
                .setGameID(game.gameID)
                .setTimestamp(timestamp)
                .setUser(game.chat.me().getProto())
                .setValue(response)
                .addAllData(data.map { x -> ByteString.copyFrom(x) })
        val gameMessage = GameMessageProto.GameMessage
                .newBuilder()
                .setType(GameMessageProto.GameMessage.Type.GAME_STATE_MESSAGE)
                .setGameStateMessage(msg)
        val genericMessage = GenericMessageProto.GenericMessage
                .newBuilder()
                .setType(GenericMessageProto.GenericMessage.Type.GAME_MESSAGE)
                .setGameMessage(gameMessage)
                .build()
        messageLog[timestamp] = genericMessage
        synchronized(game){
            game.chat.groupBroker.broadcastAsync(game.group, genericMessage)
        }

    }

    /**
     * deligate endGameMessage to game.
     */
    fun processEndGame(msg: GameMessageProto.GameEndMessage){
        synchronized(game){
            game.evaluateGameEnd(msg)
        }
        endGameMessageQueue.add(msg)
    }

    /**
     * Run game
     */
    override fun call(): T {
        sendResponse(game.getInitialMessage(), game.getData())
        while(!game.isFinished()){
            val responses: List<GameMessageProto.GameStateMessage>
                responses = getResponsePack()
                if(game.isFinished()){
                    break
                }
                if(responses.size + game.stopedPlaying.users.size < game.group.users.size){
                    val responded = (responses.map { x -> User(x.user) }).toSet()
                    for(user in game.group.users){
                        if(!responded.contains(user)){
                            error("[${game.chat.me().name}] User ${user.name} failed to respond at step $timestamp")
                        }
                    }
                    throw GameExecutionException("Failed to receive response from everyone")
                }

            val computed = game.evaluate(responses)

            sendResponse(computed, game.getData())



            stateMessageQueue.addAll(nextStepMessages)
            nextStepMessages.clear()
        }

        timestamp ++
        messageLog[timestamp] = game.gameManager.sendEndGame(game.gameID,
                "[${game.chat.username}] finished playing!\nWith final result:\n ${game.getFinalMessage()}",
                game.getVerifier())

        var failures = 0
        while(game.group != game.stopedPlaying){
            val msg = endGameMessageQueue.pollFirst(500, TimeUnit.MILLISECONDS)
            if (msg == null) {
                failures ++
            }
        }
        if(game.group != game.stopedPlaying){
            throw GameExecutionException("Not everyone stopped playing, or end game message lost")
        }
        val result = game.getResult()
        game.gameManager.deleteGame(game)
        game.chat.showMessage(ChatMessage(game.chat, "Game completed: ${game.name}"))
        return result


    }

}