package apps.games.primitives.protocols

import apps.chat.Chat
import apps.games.Game
import entity.ChatMessage
import entity.Group
import entity.User
import proto.GameMessageProto

/**
 * Created by user on 6/24/16.
 * Simple game:
 * Say moo, succeed
 */
class MooGame(chat: Chat, group: Group, gameID: String) : Game<Unit>(chat,
        group, gameID) {
    override fun getResult() {
        return Unit
    }

    override val name: String
        get() = "MOO Game"
    var finished = 0

    override fun isFinished(): Boolean {
        return finished >= 2
    }

    override fun evaluate(responses: List<GameMessageProto.GameStateMessage>): String {
        for (msg in responses) {
            chat.showMessage(ChatMessage(chat.chatId, User(msg.user),
                    "Game value: ${msg.value}"))
        }
        finished++
        return "MOO"
    }
}