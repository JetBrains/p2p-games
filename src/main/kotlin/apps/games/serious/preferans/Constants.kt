package apps.games.serious.preferans

/**
 * Created by user on 7/7/16.
 */


enum class Bet(var type: String, var value: Int, var trump: Suit,
               val contract: Int, val bullet: Int, val penalty: Int,
               val whistBounty: Int, val whistPenalty: Int, val whistNorm: Int) {
    UNKNOWN("UNKNOWN", -1, Suit.UNKNOWN, -1, -1, -1, -1, -1, -1),
    PASS("PASS", 0, Suit.UNKNOWN, -1, -1, -1, -1, -1, -1),

    SIX_OF_SPADES("6 SPADES", 1, Suit.SPADES, 6, 2, 4, 4, 2, 4),
    SIX_OF_CLUBS("6 CLUBS", 2, Suit.CLUBS, 6, 2, 4, 4, 2, 4),
    SIX_OF_DIAMONDS("6 DIAMOND", 3, Suit.DIAMONDS, 6, 2, 4, 4, 2, 4),
    SIX_OF_HEARTS("6 HEART", 4, Suit.HEARTS, 6, 2, 4, 4, 2, 4),
    SIX_NO_TRUMP("6 NO TRUMP", 5, Suit.UNKNOWN, 6, 2, 4, 4, 2, 4),

    SEVEN_OF_SPADES("7 SPADES", 6, Suit.SPADES, 7, 4, 8, 8, 4, 2),
    SEVEN_OF_CLUBS("7 CLUBS", 7, Suit.CLUBS, 7, 4, 8, 8, 4, 2),
    SEVEN_OF_DIAMONDS("7 DIAMOND", 8, Suit.DIAMONDS, 7, 4, 8, 8, 4, 2),
    SEVEN_OF_HEARTS("7 HEART", 9, Suit.HEARTS, 7, 4, 8, 8, 4, 2),
    SEVEN_NO_TRUMP("7 NO TRUMP", 10, Suit.UNKNOWN, 7, 4, 8, 8, 4, 2),

    EIGHT_OF_SPADES("8 SPADES", 11, Suit.SPADES, 8, 6, 12, 12, 6, 1),
    EIGHT_OF_CLUBS("8 CLUBS", 12, Suit.CLUBS, 8, 6, 12, 12, 6, 1),
    EIGHT_OF_DIAMONDS("8 DIAMOND", 13, Suit.DIAMONDS, 8, 6, 12, 12, 6, 1),
    EIGHT_OF_HEARTS("8 HEART", 14, Suit.HEARTS, 8, 6, 12, 12, 6, 1),
    EIGHT_NO_TRUMP("8 NO TRUMP", 15, Suit.UNKNOWN, 8, 6, 12, 12, 6, 1),

    MIZER("MIZER", 16, Suit.UNKNOWN, 0, 10, 20, -1, -1, -1),

    NINE_OF_SPADES("9 SPADES", 17, Suit.SPADES, 9, 8, 16, 16, 8, 1),
    NINE_OF_CLUBS("9 CLUBS", 18, Suit.CLUBS, 9, 8, 16, 16, 8, 1),
    NINE_OF_DIAMONDS("9 DIAMOND", 19, Suit.DIAMONDS, 9, 8, 16, 16, 8, 1),
    NINE_OF_HEARTS("9 HEART", 20, Suit.HEARTS, 9, 8, 16, 16, 8, 1),
    NINE_NO_TRUMP("9 NO TRUMP", 21, Suit.UNKNOWN, 9, 8, 16, 16, 8, 1),

    TEN_OF_SPADES("10 SPADES", 22, Suit.SPADES, 10, 10, 20, 20, 10, 1),
    TEN_OF_CLUBS("10 CLUBS", 23, Suit.CLUBS, 10, 10, 20, 20, 10, 1),
    TEN_OF_DIAMONDS("10 DIAMOND", 24, Suit.DIAMONDS, 10, 10, 20, 20, 10, 1),
    TEN_OF_HEARTS("10 HEART", 25, Suit.HEARTS, 10, 10, 20, 20, 10, 1),
    TEN_NO_TRUMP("10 NO TRUMP", 26, Suit.UNKNOWN, 10, 10, 20, 20, 10, 1),
}


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