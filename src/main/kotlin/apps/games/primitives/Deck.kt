package apps.games.primitives

import crypto.random.shuffleArray
import entity.User
import org.bouncycastle.jce.spec.ECParameterSpec
import org.bouncycastle.math.ec.ECPoint
import org.bouncycastle.pqc.math.linearalgebra.Permutation
import java.math.BigInteger
import java.util.*

/**
 * Created by user on 7/5/16.
 */


class Deck(val ECParams: ECParameterSpec, val size: Int = 52) : Cloneable {
    val cards = Array<ECPoint>(size, { i -> ECParams.g })

    /**
     * check if this deck contains specified
     * cardID
     * @param card - cardID to find
     */
    fun contains(card: ECPoint): Boolean {
        return cards.contains(card)
    }

    /**
     * Shuffle this deck with
     * random permutation
     */
    fun shuffle() {
        shuffleArray(cards)
    }

    /**
     * Shuffle this deck
     * with given permutation
     *
     * @param permutation - List of indiciers in permutuation. TODO - move to bouncycastle permutations
     */
    fun shuffle(permutation: List<Int>){
        if(size != permutation.size){
            throw IllegalArgumentException("Permutation size doesn't match deck size")
        }
        val res = mutableListOf<ECPoint>()
        for(x in permutation){
            res.add(cards[x])
        }
        for (i in 0..size-1){
            cards[i] = res[i]
        }
    }

    /**
     * Encrypt all cards with specified key
     * @param key
     */
    fun encrypt(key: BigInteger) {
        for (i in 0..size - 1) {
            cards[i] = cards[i].multiply(key)
        }
    }

    /**
     * Decrypt all cards assuming they were
     * encrypted with given key
     */
    fun decrypt(key: BigInteger) {
        val inv = key.modInverse(ECParams.n)
        for (i in 0..size - 1) {
            cards[i] = cards[i].multiply(inv)
        }
    }


    /**
     * encrypt each cardID with it's own
     * key provided by keys
     * @param keys - Collection of keys
     * to encrypt cards
     */
    fun enctyptSeparate(keys: Collection<BigInteger>) {
        if (keys.size < size) {
            throw IndexOutOfBoundsException(
                    "Insufficient number of keys provided")
        }
        for (i in 0..size - 1) {
            cards[i] = cards[i].multiply(keys.elementAt(i))
        }
    }

    /**
     * deccrypt each cardID with it's own
     * key provided by keys
     * @param keys - Collection of keys
     * to decrypt cards
     */
    fun decryptSeparate(keys: Collection<BigInteger>) {
        if (keys.size < size) {
            throw IndexOutOfBoundsException(
                    "Insufficient number of keys provided")
        }
        for (i in 0..size - 1) {
            cards[i] = cards[i].multiply(
                    keys.elementAt(i).modInverse(ECParams.n))
        }
    }

    /**
     * encrypt a single cardID with give key
     * @param n - id of cardID to decrypt
     * @param key - encryption key
     */
    fun encryptCardWithKey(n: Int, key: BigInteger) {
        cards[n] = cards[n].multiply(key)
    }

    /**
     * Decrypt a single cardID with give key
     * @param n - id of cardID to decrypt
     * @param key - decryption key
     */
    fun decryptCardWithKey(n: Int, key: BigInteger) {
        cards[n] = cards[n].multiply(key.modInverse(ECParams.n))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Deck

        if (!Arrays.equals(cards, other.cards)) return false

        return true
    }

    override fun hashCode(): Int {
        return Arrays.hashCode(cards)
    }

    override fun toString(): String {
        return "Deck(cards=${Arrays.toString(cards)})"
    }

    override public fun clone(): Deck {
        val res = Deck(ECParams, size)
        for (i in 0..size - 1) {
            res.cards[i] = cards[i]
        }
        return res
    }
}

data class EncryptedDeck(val deck: Deck, val keys: List<BigInteger>,
                         val hashes: Map<User, String>)