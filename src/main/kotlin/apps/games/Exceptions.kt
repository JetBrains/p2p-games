package apps.games

import proto.GameMessageProto
import proto.GenericMessageProto

/**
 * Created by user on 6/24/16.
 */

/**
 * Someone tried to interfere with game state
 * (Impossible state reached)
 */
class GameStateException(msg: String): Exception(msg){}

class GameInputException(msg: String): Exception(msg){}

class GameExecutionException(msg: String): Exception(msg){}

val errorMessage = GenericMessageProto.GenericMessage
                    .newBuilder()
                    .setType(GenericMessageProto.GenericMessage.Type.GAME_MESSAGE)
                    .setGameMessage(GameMessageProto.GameMessage
                            .newBuilder()
                            .setType(GameMessageProto.GameMessage.Type.GAME_ERROR_MESSAGE)
                            .setGameErrorMessage(GameMessageProto.GameErrorMessage.getDefaultInstance())).build()