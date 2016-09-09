package apps.games

import Settings
import apps.chat.Chat
import apps.chat.ChatManager
import crypto.random.randomString
import entity.Group
import entity.User
import network.ConnectionManager
import network.Service
import network.dispatching.Dispatcher
import network.dispatching.EnumDispatcher
import org.apache.commons.collections4.map.LRUMap
import org.apache.commons.collections4.queue.CircularFifoQueue
import proto.GameMessageProto
import proto.GenericMessageProto
import proto.QueryProto
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future


/**
 * Created by user on 6/24/16.
 */

object GameManager : GameManagerClass(ConnectionManager) {}

open class GameManagerClass(private val connectionManager: network.ConnectionManagerClass) {
    val games = LRUMap<String, Game<*>>(1000)
    val runners = LRUMap<String, GameRunner<*>>(1000)
    internal val LAG_MESSAGE_LIMIT = 1000
    internal val threadPool: ExecutorService = Executors.newCachedThreadPool()
    internal val unprocessed = CircularFifoQueue<GameMessageProto.GameStateMessage>(
            LAG_MESSAGE_LIMIT)

    val IDENTIFIER_LENGTH = 32

    /**
     * Initialize services, start listening to ports
     */
    fun start() {
        connectionManager.addService(
                GenericMessageProto.GenericMessage.Type.GAME_MESSAGE,
                GameMessageService(this))
        connectionManager.addService(
                GenericMessageProto.GenericMessage.Type.QUERY,
                GameQueryService(this))
    }

    /**
     * send initial request to start game
     * GameInitMessage will be sent to all
     * users of gui
     * @param chat - where to conduct game
     * @param type - game type
     */
    fun sendGameInitRequest(chat: Chat, type: String) {

        val initMessage = GameMessageProto.GameInitMessage
                .newBuilder()
                .setUser(User(Settings.hostAddress, chat.username).getProto())
                .setChatID(chat.chatId)
                .setGameID(randomString(IDENTIFIER_LENGTH))
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
        chat.groupBroker.broadcastAsync(chat.group, finalMessage)
    }

    /**
     * Someone initialized a game. Process request
     * and start local game
     */
    fun initGame(msg: GameMessageProto.GameInitMessage): Future<Unit>? {
        val group = Group(msg.participants)
        val chat = ChatManager.getChatOrNull(msg.chatID) ?: return null
        val game = GameFactory.instantiateGame(msg.gameType, chat, group, msg.gameID)
        games[msg.gameID] = game
        if (group != chat.group) {
            sendEndGame(msg.gameID,
                    "gui member lists of [${msg.user.name}] and [${chat.username}] mismatch",
                    game.getVerifier())
            return null
        } else {
            val runner = GameRunner(game, Int.MAX_VALUE)
            resolveLag(runner)
            runners[msg.gameID] = runner
            return threadPool.submit(runner)
        }
    }


    /**
     * Init local game. Do not send any requests
     */
    fun <T> initSubGame(game: Game<T>, maxRetries: Int = 5): Future<T> {
        val runner: GameRunner<*>
        games[game.gameID] = game
        runner = GameRunner(game, maxRetries)

        resolveLag(runner)

        runners[game.gameID] = runner
        return threadPool.submit(runner)


    }

    /**
     * @param game - game to be removed
     */
    fun deleteGame(game: Game<*>) {
        println("${game.gameID} was canceled")
    }

    /**
     * For somewhat reason game decided, that it
     * has ended for us. So we acknowledge everyone about it
     */
    fun sendEndGame(gameID: String,
                    reason: String,
                    verifier: String?): GenericMessageProto.GenericMessage? {
        val game: Game<*>? = games[gameID]
        if (game != null) {
            val endMessage = GameMessageProto.GameEndMessage
                    .newBuilder()
                    .setUser(game.chat.me().getProto())
                    .setGameID(gameID)
                    .setReason(reason)
            if (verifier != null) {
                endMessage.verifier = verifier
            }

            val gameMessage = GameMessageProto.GameMessage
                    .newBuilder()
                    .setType(GameMessageProto.GameMessage.Type.GAME_END_MESSAGE)
                    .setGameEndMessage(endMessage)
            val finalMessage = GenericMessageProto.GenericMessage
                    .newBuilder()
                    .setType(
                            GenericMessageProto.GenericMessage.Type.GAME_MESSAGE)
                    .setGameMessage(gameMessage).build()
            game.chat.groupBroker.broadcastAsync(game.chat.group, finalMessage)
            return finalMessage
        }
        return null
    }

    fun close() {
        threadPool.shutdownNow()
    }

    private fun resolveLag(gameRunner: GameRunner<*>) {
        synchronized(unprocessed) {
            val it = unprocessed.iterator()
            while (it.hasNext()) {
                val msg = it.next()
                if (msg.gameID == gameRunner.game.gameID) {
                    gameRunner.stateMessageQueue.add(msg)
                    it.remove()
                }

            }
        }
    }


    fun requestUpdate(receiver: User,
                      game: Game<*>,
                      timestamp: Int): GenericMessageProto.GenericMessage? {
        val statusQuery = QueryProto.GameStatusQuery
                .newBuilder()
                .setGameID(game.gameID)
                .setUser(game.chat.me().getProto())
                .setTimestamp(timestamp)
        val query = QueryProto.Query
                .newBuilder()
                .setType(QueryProto.Query.Type.GAME_STATUS_QUERY)
                .setGameStatusQuery(statusQuery)
        val genericMessage = GenericMessageProto.GenericMessage
                .newBuilder()
                .setType(GenericMessageProto.GenericMessage.Type.QUERY)
                .setQuery(query)
                .build()
        return connectionManager.request(receiver.hostAddress, genericMessage)
    }

}

class GameQueryService(private val manager: GameManagerClass) : Service<QueryProto.Query> {
    fun queryStatus(msg: QueryProto.GameStatusQuery): GenericMessageProto.GenericMessage? {
        val runner = manager.runners[msg.gameID]
        if (runner == null || runner.messageLog[msg.timestamp] == null) {
            return errorMessage
        }
        return runner.messageLog[msg.timestamp]
    }

    override fun getDispatcher(): Dispatcher<QueryProto.Query> {
        val queryDispatcher = EnumDispatcher(
                QueryProto.Query.getDefaultInstance())
        queryDispatcher.register(QueryProto.Query.Type.GAME_STATUS_QUERY,
                { x: QueryProto.GameStatusQuery -> queryStatus(x) })
        return queryDispatcher
    }
}

/**
 * Service for dispatching game messages
 */
class GameMessageService(private val manager: GameManagerClass) : Service<GameMessageProto.GameMessage> {

    fun startGame(msg: GameMessageProto.GameInitMessage): GenericMessageProto.GenericMessage? {
        manager.initGame(msg)
        return null
    }

    fun processMessage(msg: GameMessageProto.GameStateMessage): GenericMessageProto.GenericMessage? {
        val runner = manager.runners[msg.gameID]
        if (runner != null) {
            runner.stateMessageQueue.add(msg)
        } else {
            synchronized(manager.unprocessed) {
                manager.unprocessed.add(msg)
            }
        }
        return null
    }

    fun endGame(msg: GameMessageProto.GameEndMessage): GenericMessageProto.GenericMessage? {
        println("[${Settings.hostAddress}] received endgame from [${msg.user.port}]")
        manager.runners[msg.gameID]?.processEndGame(msg)
        return null
    }

    override fun getDispatcher(): Dispatcher<GameMessageProto.GameMessage> {
        val messageDispatcher = EnumDispatcher(
                GameMessageProto.GameMessage.getDefaultInstance())
        messageDispatcher.register(
                GameMessageProto.GameMessage.Type.GAME_INIT_MESSAGE,
                { x: GameMessageProto.GameInitMessage -> startGame(x) })
        messageDispatcher.register(
                GameMessageProto.GameMessage.Type.GAME_STATE_MESSAGE,
                { x: GameMessageProto.GameStateMessage -> processMessage(x) })
        messageDispatcher.register(
                GameMessageProto.GameMessage.Type.GAME_END_MESSAGE,
                { x: GameMessageProto.GameEndMessage -> endGame(x) })
        return messageDispatcher

    }
}
