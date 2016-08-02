package apps.games.serious.Cheat.logger

/**
 * Created by user on 8/2/16.
 *
 * class descripts stack of plays:
 * plays stack untill someone verifies cards on top of the stack
 */

class PlayStack {
    val encryptedCards = listOf<String>()

    fun isNewStack(): Boolean = encryptedCards.isEmpty()

}

