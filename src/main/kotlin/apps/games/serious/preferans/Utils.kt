package apps.games.serious.preferans

import apps.games.GameExecutionException


/**
 * Created by user on 7/18/16.d
 */
/**
 * Given Id of card in 32 card deck -
 * return an object representing it's
 * Suit and Pip
 * @param cardID - card to find
 * @return Card - Suit and Pip
 */
fun getCardById32(cardID: Int): Card {
    val card: Card
    if (cardID == -1) {
        card = Card(Suit.UNKNOWN, Pip.UNKNOWN)
    } else {
        val suitId = cardID / 8
        var pipId: Int = (cardID % 8)
        if (Pip.TWO.index <= pipId) {
            pipId += 5
        }
        card = Card(
                Suit.values().first { x -> x.index == suitId },
                Pip.values().first { x -> x.index == pipId })
    }
    return card
}

/**
 * Find max card in collection, considering
 * trump suit and enforcedSuit
 */
fun maxWithTrump(cards: Collection<Card>, trump: Suit = Suit.UNKNOWN,
                 enforcedSuit: Suit = Suit.UNKNOWN): Card?{
    val trumpPlays = cards.filter {x -> x.suit == trump}
    val maxValue: Int = Pip.values().maxBy { x -> x.value}!!.value + 1

    val maxTrump = trumpPlays.maxBy { x -> if(x.pip == Pip.ACE) maxValue else x.pip.value}
    if (maxTrump != null) {
        return maxTrump
    }
    return cards.filter { x -> x.suit == enforcedSuit }.maxBy { v -> if(v.pip == Pip.ACE)
                                                                maxValue else v.pip.value }

}

