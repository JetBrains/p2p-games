package apps.games.serious

import apps.table.gui.CardGUI


data class Card(val suit: Suit, val pip: Pip)

private val MAX_DECK_SIZE = 54
/**
 * Created by user on 7/18/16.d
 */

/**
 * Given Id of cardID in n card deck -
 * return an object representing it's
 * Suit and Pip.
 * @param cardID - ID of card to find
 * @param n - deck size. n/4 hightest
 * cards in each pip
 * @return Card - Suit and Pip
 */
fun getCardById(cardID: Int, n: Int): Card {
    if (n > MAX_DECK_SIZE) {
        throw IllegalArgumentException("Can not process deck with more that 52 cards")
    }
    val card: Card
    if (cardID == -1) {
        card = Card(Suit.UNKNOWN, Pip.UNKNOWN)
    } else {
        //All count Pips, that are not UNKNOWN
        val totalPips = Pip.values().size - 1
        //All count Suits, that are not UNKNOWN
        val totalSuits = Suit.values().size - 1
        val nPips = n / totalSuits
        val gapSize = totalPips - nPips
        val suitId = cardID / nPips
        var pipId: Int = (cardID % nPips)
        if (Pip.TWO.index <= pipId) {
            pipId += gapSize
        }
        card = Card(
                Suit.values().first { x -> x.index == suitId },
                Pip.values().first { x -> x.index == pipId })
    }
    return card
}

/**
 * This function takes cardID
 * and translates it into corresponding
 * Card Id (-1 -> n). -1 - for UNKNOWN
 * @param card - card to translate
 * @param n - deck size
 */
fun getIdByCard(card: Card, n: Int): Int {
    if (n > MAX_DECK_SIZE) {
        throw IllegalArgumentException("Can not process deck with more that 52 cards")
    }
    if (card.suit == Suit.UNKNOWN) {
        return -1
    }
    val suitID: Int = card.suit.index
    var pipID: Int = card.pip.index
    //All count Pips, that are not UNKNOWN
    val totalPips = Pip.values().size - 1
    //All count Suits, that are not UNKNOWN
    val totalSuits = Suit.values().size - 1
    val nPips = n / totalSuits
    val gapSize = totalPips - nPips

    if (pipID - gapSize >= Pip.TWO.index) {
        pipID -= gapSize
    }
    return nPips * suitID + pipID
}


/**
 * Get cardGUI object and translate it
 * into card ID
 */
fun getIdByCard(card: CardGUI, n: Int): Int {
    return getIdByCard(Card(card.suit, card.pip), n)
}


/**
 * Get list of all Pips that appear in
 * n-card deck
 */
fun getPipsInDeck(n: Int): List<Pip> {
    //All count Pips, that are not UNKNOWN
    val totalPips = Pip.values().size - 1
    //All count Suits, that are not UNKNOWN
    val totalSuits = Suit.values().size - 1
    val nPips = n / totalSuits
    val gapSize = totalPips - nPips

    val res = mutableListOf<Pip>()
    for (pip in Pip.values().sortedBy { x -> x.value }) {
        if (pip == Pip.UNKNOWN) {
            continue
        }
        if (pip == Pip.ACE) {
            res.add(pip)
            continue
        }
        if (pip.index - Pip.TWO.index >= gapSize) {
            res.add(pip)
        }
    }
    return res
}

/**
 * Find max cardID in collection, considering
 * trump suit and enforcedSuit
 */
fun maxWithTrump(cards: Collection<Card>, trump: Suit = Suit.UNKNOWN,
                 enforcedSuit: Suit = Suit.UNKNOWN): Card? {
    val trumpPlays = cards.filter { x -> x.suit == trump }
    val maxValue: Int = Pip.values().maxBy { x -> x.value }!!.value + 1

    val maxTrump = trumpPlays.maxBy { x -> if (x.pip == Pip.ACE) maxValue else x.pip.value }
    if (maxTrump != null) {
        return maxTrump
    }
    return cards.filter { x -> x.suit == enforcedSuit }.maxBy { v ->
        if (v.pip == Pip.ACE)
            maxValue else v.pip.value
    }

}

