package apps.games.serious.Cheat


/**
 * Created by user on 8/1/16.
 */



enum class DeckSizes(val type: String,val size: Int){
    DEBUG("DEBUG", 8),
    SMALL("36 CARD", 36),
    LARGE("52 CARD", 52)
}

enum class BetCount(val size: Int){
    ONE(1),
    TWO(2),
    THREE(3),
    FOUR(4);

    val type = name
}

enum class Choice(val type: String){
    ADD("ADD CARDS"),
    CHECK_TRUE("BELIEVE"),
    CHECK_FALSE("CHEAT")
}