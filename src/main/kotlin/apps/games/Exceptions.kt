package apps.games

/**
 * Created by user on 6/24/16.
 */

/**
 * Someone tried to interfere with game state
 * (Impossible state reached)
 */
class GameStateException(msg: String): Exception(msg){}

class GameInputException(msg: String): Exception(msg){}