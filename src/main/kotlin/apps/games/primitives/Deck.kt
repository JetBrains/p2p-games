package apps.games.primitives

import org.bouncycastle.jce.spec.ECParameterSpec
import org.bouncycastle.math.ec.ECPoint
import java.util.*

/**
 * Created by user on 7/5/16.
 */


class Deck(ECParams: ECParameterSpec, maxDeckSize: Int = 52){
    private val cards = Array<ECPoint>(maxDeckSize, {i -> ECParams.g})

    fun contains(card: ECPoint): Boolean{
        return cards.contains(card)
    }

    fun set(pos: Int, card: ECPoint){
        cards[pos] = card
    }

    override fun equals(other: Any?): Boolean{
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Deck

        if (!Arrays.equals(cards, other.cards)) return false

        return true
    }

    override fun hashCode(): Int{
        return Arrays.hashCode(cards)
    }

    override fun toString(): String{
        return "Deck(cards=${Arrays.toString(cards)})"
    }


}