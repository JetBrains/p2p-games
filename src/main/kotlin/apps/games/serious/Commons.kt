package apps.games.serious

import apps.games.primitives.Deck
import apps.games.primitives.EncryptedDeck

/**
 * Created by user on 8/2/16.
 */
/**
 * CardGUI Suit representation
 */
enum class Suit(val type: String, val index: Int) {
    UNKNOWN("unknown", -1),
    CLUBS("clubs", 0),
    DIAMONDS("diamonds", 1),
    HEARTS("hearts", 2),
    SPADES("spades", 3)
}

/**
 * CardGUI Pip representation
 */
enum class Pip(val value: Int) {
    UNKNOWN(-1),
    ACE(1),
    TWO(2),
    THREE(3),
    FOUR(4),
    FIVE(5),
    SIX(6),
    SEVEN(7),
    EIGHT(8),
    NINE(9),
    TEN(10),
    JACK(11),
    QUEEN(12),
    KING(13);

    val index = value - 1
    val type = name
}

data class ShuffledDeck(val originalDeck: Deck, val encrypted: EncryptedDeck)