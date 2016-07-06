package apps.games.primitives

import crypto.random.randomPermutuation
import crypto.random.shuffleArray
import org.bouncycastle.jce.spec.ECParameterSpec
import org.bouncycastle.math.ec.ECPoint
import java.math.BigInteger
import java.util.*

/**
 * Created by user on 7/5/16.
 */


class Deck(val ECParams: ECParameterSpec,val size: Int = 52){
    val cards = Array<ECPoint>(size, { i -> ECParams.g})

    /**
     * check if this deck contains specified
     * card
     * @param card - card to find
     */
    fun contains(card: ECPoint): Boolean{
        return cards.contains(card)
    }

    /**
     * Shuffle this deck with
     * random permutuation
     */
    fun shuffle(){
        shuffleArray(cards)
    }

    /**
     * Encrypt all cards with specified key
     * @param key
     */
    fun encrypt(key: BigInteger){
        for(i in 0..size-1){
            cards[i] = cards[i].multiply(key)
        }
    }

    /**
     * Decrypt all cards assuming they were
     * encrypted with given key
     */
    fun decrypt(key: BigInteger){
        val inv = key.modInverse(ECParams.n)
        for(i in 0..size-1){
            cards[i] = cards[i].multiply(inv)
        }
    }


    /**
     * encrypt each card with it's own
     * key provided by keys
     * @param keys - Collection of keys
     * to encrypt cards
     */
    fun enctyptSeparate(keys: Collection<BigInteger>){
        if(keys.size < size){
            throw IndexOutOfBoundsException("Insufficient number of keys provided")
        }
        for(i in 0..size-1){
            cards[i] = cards[i].multiply(keys.elementAt(i))
        }
    }

    /**
     * deccrypt each card with it's own
     * key provided by keys
     * @param keys - Collection of keys
     * to decrypt cards
     */
    fun dectyptSeparate(keys: Collection<BigInteger>){
        if(keys.size < size){
            throw IndexOutOfBoundsException("Insufficient number of keys provided")
        }
        for(i in 0..size-1){
            cards[i] = cards[i].multiply(keys.elementAt(i).modInverse(ECParams.n))
        }
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

class EncryptedDeck(val deck: Deck, val keys: List<BigInteger>)