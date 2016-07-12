package apps.games.serious.preference

/**
 * Created by user on 7/7/16.
 */


enum class Bet(var type: String, var value: Int) {
    UNKNOWN("UNKNOWN", -1),
    PASS("PASS", 0),

    SIX_OF_SPADES("6 SPADES", 1),
    SIX_OF_CLUBS("6 CLUBS", 2),
    SIX_OF_DIAMONDS("6 DIAMOND", 3),
    SIX_OF_HEARTS("6 HEART", 4),
    SIX_NO_TRUMP("6 NO TRUMP", 5),

    SEVEN_OF_SPADES("7 SPADES", 6),
    SEVEN_OF_CLUBS("7 CLUBS", 7),
    SEVEN_OF_DIAMONDS("7 DIAMOND", 8),
    SEVEN_OF_HEARTS("7 HEART", 9),
    SEVEN_NO_TRUMP("7 NO TRUMP", 10),

    EIGHT_OF_SPADES("8 SPADES", 11),
    EIGHT_OF_CLUBS("8 CLUBS", 12),
    EIGHT_OF_DIAMONDS("8 DIAMOND", 13),
    EIGHT_OF_HEARTS("8 HEART", 14),
    EIGHT_NO_TRUMP("8 NO TRUMP", 15),

    MIZER("MIZER", 16),

    NINE_OF_SPADES("9 SPADES", 17),
    NINE_OF_CLUBS("9 CLUBS", 18),
    NINE_OF_DIAMONDS("9 DIAMOND", 19),
    NINE_OF_HEARTS("9 HEART", 20),
    NINE_NO_TRUMP("9 NO TRUMP", 21),

    TEN_OF_SPADES("10 SPADES", 22),
    TEN_OF_CLUBS("10 CLUBS", 23),
    TEN_OF_DIAMONDS("10 DIAMOND", 24),
    TEN_OF_HEARTS("10 HEART", 25),
    TEN_NO_TRUMP("10 NO TRUMP", 26),
}

/**
 * Card Suit representation
 */
enum class Suit(val type: String, val index: Int) {
    UNKNOWN("unknown", -1),
    CLUBS("clubs", 0),
    DIAMONDS("diamonds", 1),
    HEARTS("hearts", 2),
    SPADES("spades", 3)
}

/**
 * Card Pip representation
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