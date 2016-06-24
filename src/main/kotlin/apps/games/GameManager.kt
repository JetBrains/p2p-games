package apps.games

import apps.chat.Chat
import apps.chat.ChatManager
import entity.Group
import entity.User
import network.ConnectionManager
import network.Service
import network.dispatching.Dispatcher
import network.dispatching.EnumDispatcher
import proto.GameMessageProto
import proto.GenericMessageProto
import random.randomString

/**
 * Created by user on 6/24/16.
 */

object GameManager {
    val gameTypes: MutableMap<String, Class<Game>> = mutableMapOf()
    val games: MutableMap<String, Game> = mutableMapOf()
    val runners: MutableMap<String, GameRunner> = mutableMapOf()

    final val INDETIFIER_LENGTH = 32

    /**
     * Initialize services, start listening to ports
     */
    fun start() {
        ConnectionManager.addService(GenericMessageProto.GenericMessage.Type.GAME_MESSAGE, GameMessageService(this))
    }

    /**
     * send initial request to start game
     * GameInitMessage will be sent to all
     * users of Chat
     * @param chat - where to conduct game
     * @param type - game type
     */
    fun sendGameInitRequest(chat: Chat, type: String){

        val initMessage = GameMessageProto.GameInitMessage
                .newBuilder()
                .setUser(User(Settings.hostAddress, chat.username).getProto())
                .setChatID(chat.chatId)
                .setGameID(randomString(INDETIFIER_LENGTH))
                .setGameType(type)
                .setParticipants(chat.group.getProto())
        val gameMessage = GameMessageProto.GameMessage
                .newBuilder()
                .setType(GameMessageProto.GameMessage.Type.GAME_INIT_MESSAGE)
                .setGameInitMessage(initMessage)
        val finalMessage = GenericMessageProto.GenericMessage
                .newBuilder()
                .setType(GenericMessageProto.GenericMessage.Type.GAME_MESSAGE)
                .setGameMessage(gameMessage).build()
        chat.groupBroker.broadcast(chat.group, finalMessage)
    }

    /**
     * Someone initialized a game. Process request
     * and start local game
     */
    fun initGame(msg: GameMessageProto.GameInitMessage){
        val group = Group(msg.participants)
        val chat = ChatManager.getChatOrNull(msg.chatID)
        if(chat == null){
            //TODO - respond to someone
            return
        }
        val game = MooGame(chat, group, msg.gameID)
        games[msg.gameID] = game
        if(group != chat.group){
            sendEndGame(msg.gameID, "Chat member lists of [${msg.user.name}] and [${chat.username}] mismatch")
        }else{
            val runner = GameRunner(game)
            Thread(runner).start()
            runners[msg.gameID] = runner
        }
    }

    /**
     * For somewhat reason game decided, that it
     * has ended for us. So we acknowledge everyone about it
     */
    fun sendEndGame(gameID: String, reason: String){
        val game: Game? = games.remove(gameID)
        if(game == null){
            return
        }
        val endMessage = GameMessageProto.GameEndMessage
                .newBuilder()
                .setUser(User(Settings.hostAddress, game.chat.username).getProto())
                .setGameID(gameID)
                .setReason(reason)

        val gameMessage = GameMessageProto.GameMessage
                .newBuilder()
                .setType(GameMessageProto.GameMessage.Type.GAME_END_MESSAGE)
                .setGameEndMessage(endMessage)
        val finalMessage = GenericMessageProto.GenericMessage
                .newBuilder()
                .setType(GenericMessageProto.GenericMessage.Type.GAME_MESSAGE)
                .setGameMessage(gameMessage).build()
        game.chat.groupBroker.broadcast(game.chat.group, finalMessage)
    }
}

/**
 * Service for dispatching game messages
 */
class GameMessageService(private val manager: GameManager) : Service<GameMessageProto.GameMessage> {
    fun startGame(msg: GameMessageProto.GameInitMessage): GenericMessageProto.GenericMessage?{
        manager.initGame(msg)
        return null
    }

    fun processMessage(msg: GameMessageProto.GameStateMessage): GenericMessageProto.GenericMessage?{
        //todo
        val runner = manager.runners[msg.gameID]
        if (runner != null) {
            runner.stateMessageQueue.add(msg)
        }
        return null
    }

    fun endGame(msg: GameMessageProto.GameEndMessage): GenericMessageProto.GenericMessage?{
        //todo
        return null
    }

    override fun getDispatcher(): Dispatcher<GameMessageProto.GameMessage> {
        val queryDispatcher = EnumDispatcher(GameMessageProto.GameMessage.getDefaultInstance())
        queryDispatcher.register(GameMessageProto.GameMessage.Type.GAME_INIT_MESSAGE,
                {x: GameMessageProto.GameInitMessage -> startGame(x)})
        queryDispatcher.register(GameMessageProto.GameMessage.Type.GAME_STATE_MESSAGE,
                {x: GameMessageProto.GameStateMessage -> processMessage(x)})
        queryDispatcher.register(GameMessageProto.GameMessage.Type.GAME_END_MESSAGE,
                {x: GameMessageProto.GameEndMessage -> endGame(x)})
        return queryDispatcher

    }
}

