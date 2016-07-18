package apps.games.serious.preferans

/**
 * Created by user on 7/7/16.
 */


enum class Bet(var type: String, var value: Int, var trump: Suit) {
    UNKNOWN("UNKNOWN", -1, Suit.UNKNOWN),
    PASS("PASS", 0, Suit.UNKNOWN),

    SIX_OF_SPADES("6 SPADES", 1, Suit.SPADES),
    SIX_OF_CLUBS("6 CLUBS", 2, Suit.CLUBS),
    SIX_OF_DIAMONDS("6 DIAMOND", 3, Suit.DIAMONDS),
    SIX_OF_HEARTS("6 HEART", 4, Suit.HEARTS),
    SIX_NO_TRUMP("6 NO TRUMP", 5, Suit.UNKNOWN),

    SEVEN_OF_SPADES("7 SPADES", 6, Suit.SPADES),
    SEVEN_OF_CLUBS("7 CLUBS", 7, Suit.CLUBS),
    SEVEN_OF_DIAMONDS("7 DIAMOND", 8, Suit.DIAMONDS),
    SEVEN_OF_HEARTS("7 HEART", 9, Suit.HEARTS),
    SEVEN_NO_TRUMP("7 NO TRUMP", 10, Suit.UNKNOWN),

    EIGHT_OF_SPADES("8 SPADES", 11, Suit.SPADES),
    EIGHT_OF_CLUBS("8 CLUBS", 12, Suit.CLUBS),
    EIGHT_OF_DIAMONDS("8 DIAMOND", 13, Suit.DIAMONDS),
    EIGHT_OF_HEARTS("8 HEART", 14, Suit.HEARTS),
    EIGHT_NO_TRUMP("8 NO TRUMP", 15, Suit.UNKNOWN),

    MIZER("MIZER", 16, Suit.UNKNOWN),

    NINE_OF_SPADES("9 SPADES", 17, Suit.SPADES),
    NINE_OF_CLUBS("9 CLUBS", 18, Suit.CLUBS),
    NINE_OF_DIAMONDS("9 DIAMOND", 19, Suit.DIAMONDS),
    NINE_OF_HEARTS("9 HEART", 20, Suit.HEARTS),
    NINE_NO_TRUMP("9 NO TRUMP", 21, Suit.UNKNOWN),

    TEN_OF_SPADES("10 SPADES", 22, Suit.SPADES),
    TEN_OF_CLUBS("10 CLUBS", 23, Suit.CLUBS),
    TEN_OF_DIAMONDS("10 DIAMOND", 24, Suit.DIAMONDS),
    TEN_OF_HEARTS("10 HEART", 25, Suit.HEARTS),
    TEN_NO_TRUMP("10 NO TRUMP", 26, Suit.UNKNOWN),
}


data class Card(val suit: Suit, val pip: Pip)

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
}

/**
 * Different outcomes of whisting in preferance
 */
enum class Whists(val value: Int, val type: String){
    UNKNOWN(0, "UNKNOWN"),
    PASS(1, "PASS"),
    WHIST_HALF(2, "HALF\nWHIST"),
    WHIST_BLIND(3, "WHIST\nBLIND"),
    WHIST_OPEN(4, "WHIST\nOPEN"),

}