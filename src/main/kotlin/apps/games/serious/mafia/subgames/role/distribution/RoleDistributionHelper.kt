package apps.games.serious.mafia.subgames.role.distribution

import apps.games.GameExecutionException
import apps.games.serious.mafia.roles.PlayerRole
import apps.games.serious.mafia.roles.Role
import apps.games.serious.mafia.subgames.role.generation.RoleDeck
import entity.User
import org.bouncycastle.math.ec.ECPoint
import java.math.BigInteger


/**
 * Created by user on 8/12/16.
 */

class RoleDistributionHelper(val roleDeck: RoleDeck, val users: Collection<User>){
    private val N = users.size
    private val deckSize = roleDeck.originalRoles.size

    private val roleKeys: Map<User, Array<BigInteger>>
    private val VKeys: Map<User, Array<BigInteger>>
    private val RKeys: Map<User, Array<BigInteger>>

    init {
        roleKeys = users.associate { x -> x to Array(deckSize, {i -> BigInteger.ZERO}) }
        VKeys = users.associate { x -> x to Array(deckSize, {i -> BigInteger.ZERO}) }
        RKeys = users.associate { x -> x to Array(deckSize, {i -> BigInteger.ZERO}) }
    }

    /**
     * Register users key for a role. decrypt a card in deck
     *
     * @param user - whose role to decrypt
     * @param position -  position of role to decrypt
     * @param key - users key for this role
     */
    fun registerRoleKey(user: User, position: Int, key: BigInteger){
        if(user !in roleKeys){
            throw GameExecutionException("Unknown user's key provided")
        }
        if(position >= N || position < 0){
            throw GameExecutionException("Key position out of range")
        }
        if(roleKeys[user]!![position] != BigInteger.ZERO && roleKeys[user]!![position] != key){
            throw GameExecutionException("Another key is already registered for that combination of user and position")
        }
        roleKeys[user]!![position] = key
        roleDeck.shuffledRoles.decryptCardWithKey(position, key)
    }

    /**
     * Register users key for a V card (secret associated with a card).
     * decrypt a V card in deck
     *
     * @param user - whose V to decrypt
     * @param position -  position of V to decrypt
     * @param key - users key for this V
     */
    fun registerVKey(user: User, position: Int, key: BigInteger){
        if(user !in VKeys){
            throw GameExecutionException("Unknown user's key provided")
        }
        if(position >= N || position < 0){
            throw GameExecutionException("Key position out of range")
        }
        if(VKeys[user]!![position] != BigInteger.ZERO && VKeys[user]!![position] != key){
            throw GameExecutionException("Another key is already registered for that combination of user and position")
        }
        VKeys[user]!![position] = key
        roleDeck.V.decryptCardWithKey(position, key)
    }

    /**
     * Register users key for a R key (secret user to encrypt given V).
     * decrypt a R key
     *
     * @param user - whose R to decrypt
     * @param position -  position of R to decrypt
     * @param key - users key for this R
     */
    fun registerRKey(user: User, position: Int, key: BigInteger){
        if(user !in RKeys){
            throw GameExecutionException("Unknown user's key provided")
        }
        if(position >= N || position < 0){
            throw GameExecutionException("Key position out of range")
        }
        if(RKeys[user]!![position] != BigInteger.ZERO && RKeys[user]!![position] != key){
            throw GameExecutionException("Another key is already registered for that combination of user and position")
        }
        RKeys[user]!![position] = key
        roleDeck.encryptedR[position] *= key.modInverse(roleDeck.V.ECParams.n)
        roleDeck.encryptedR[position] %= roleDeck.V.ECParams.n
    }

    /**
     * Given position in shuffled role deck get role
     * that corresponds to that point
     *
     * @param pos - position in shuffled role deck
     * @return - Role (UNKNOWN if role is still encrypted)
     * @throws GameExecutionException - if invalid argument provided
     */
    fun getRole(pos: Int): Role {
        if(pos < 0 || pos > N){
            throw GameExecutionException("Position is out of range")
        }
        var idx: Int = roleDeck.originalRoles.cards.indexOf(roleDeck.shuffledRoles.cards[pos])
        if(idx == -1){
            return Role.UNKNOWN
        }
        for((role, count) in roleDeck.roleCounts.toSortedMap()){
            idx -= count
            if(idx < 0){
                return role
            }
        }
        throw GameExecutionException("Something went wring")
    }

    /**
     * check if card at given position can be decrypted
     * (all keys are registered)
     *
     * @param pos - position of card to check
     */
    private fun canDecryptCard(pos: Int): Boolean{
        return roleKeys.all { x -> x.value[pos] != BigInteger.ZERO } &&
                VKeys.all { x -> x.value[pos] != BigInteger.ZERO } &&
                RKeys.all { x -> x.value[pos] != BigInteger.ZERO }
    }

    fun getDecryptedVCard(pos: Int): ECPoint {
        if(!canDecryptCard(pos)){
            throw GameExecutionException("Card is not ready for decryption yet")
        }
        return roleDeck.V.cards[pos].multiply(roleDeck.encryptedR[pos].modInverse(roleDeck.V.ECParams.n))
    }

}