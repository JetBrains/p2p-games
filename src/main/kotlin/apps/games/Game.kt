package apps.games

import apps.chat.Chat
import entity.Group

/**
 * Created by user on 6/24/16.
 * @param chat - will contain logs and messages
 * @param group - group of game participants
 */

abstract class Game(internal val chat: Chat, internal val group: Group){
    abstract fun getState()

    abstract fun processMessage()
}