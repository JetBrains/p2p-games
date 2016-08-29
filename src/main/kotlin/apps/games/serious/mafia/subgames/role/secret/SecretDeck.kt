package apps.games.serious.mafia.subgames.role.secret

import apps.games.GameExecutionException
import apps.games.primitives.Deck
import com.sun.org.apache.xpath.internal.operations.Bool
import crypto.RSA.ECParams
import entity.User
import org.apache.commons.codec.digest.DigestUtils
import org.bouncycastle.math.ec.ECPoint
import java.math.BigInteger

/**
 * Created by user on 8/25/16.
 */

/**
 * @param secrets - describes pack secrets
 * @param ids - ordered list of encrypted ID's
 */
data class SecretDeck(val secrets: Deck, val ids: Deck,
                      val SKeys: Collection<BigInteger>,
                      val SKeyHashes: Map<User, String>){
    val size = secrets.size

    /**
     * Get secret part for encoded user id
     *
     * @param id - id*g(point on EC), encrypted with detective k
     */
    fun getSecretForId(id: ECPoint): ECPoint{
        if(!ids.contains(id)){
            throw GameExecutionException("Secret for unknown user requested")
        }
        return secrets.cards[ids.cards.indexOf(id)]
    }

    /**
     * Get secret part for encoded user id
     *
     * @param id - id, encrypted with detective k
     */
    fun getSecretForId(id: BigInteger): ECPoint{
        return getSecretForId(ECParams.g.multiply(id))
    }
}

class SecretSharingVerifier(val users: Collection<User>, val secrets: Deck){
    private val SKeys: Map<User, Array<BigInteger>>
    private val N = users.size
    init {
        SKeys = users.associate { x -> x to Array(N*N, {i -> BigInteger.ZERO}) }
    }

    /**
     * Register users key for a V card (secret associated with a card).
     * decrypt a V card in deck
     *
     * @param user - whose V to decrypt
     * @param position -  position of V to decrypt
     * @param key - users key for this V
     */
    fun registerSKey(user: User, position: Int, key: BigInteger){
        if(user !in SKeys){
            throw GameExecutionException("Unknown user's key provided")
        }
        if(position >= N*N || position < 0){
            throw GameExecutionException("Key position $position out of range")
        }
        if(SKeys[user]!![position] != BigInteger.ZERO && SKeys[user]!![position] != key){
            throw GameExecutionException("Another key is already registered for that combination of user and position")
        }
        SKeys[user]!![position] = key
        secrets.decryptCardWithKey(position, key)
    }

    fun cardIsDecrypted(pos: Int): Boolean{
        return SKeys.all { x -> x.value[pos] != BigInteger.ZERO }
    }

    /**
     * Verify key hashes //todo
     */
    fun verifySKeys(SKeyHashes: Map<User, String>): Boolean{
        if (!(0..N*N-1).all { x -> cardIsDecrypted(x) }){
            return false
        }
        return SKeys.all { x -> DigestUtils.sha256Hex(x.value.joinToString(" ")) == SKeyHashes[x.key] }
    }

}