package apps.games

import apps.chat.Chat
import entity.ChatMessage
import entity.Group
import entity.User
import proto.GameMessageProto

/**
 * Created by user on 6/24/16.
 * Simple game:
 * Say moo, succeed
 */
class MooGame(chat: Chat, group: Group, gameID: String) : Game(chat, group, gameID) {
    var finished = 0

    override fun isFinished(): Boolean {
        return finished >= 2
    }

    override fun evaluate(responses: List<GameMessageProto.GameStateMessage>): String {
        for(msg in responses){
            chat.showMessage(ChatMessage(chat.chatId, User(msg.user), "Game value: ${msg.value}"))
        }
        finished ++
        return "MOO"
    }
}