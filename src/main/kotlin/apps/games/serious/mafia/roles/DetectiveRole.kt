package apps.games.serious.mafia.roles

import apps.games.GameExecutionException
import entity.User
import java.math.BigInteger

/**
 * Created by user on 8/24/16.
 */
class DetectiveRole : PlayerRole() {
    override val role: Role
        get() = Role.DETECTIVE
    private val Ks = mutableMapOf<User, BigInteger>()

    /**
     * Detective knowns keys [K_i] that were used in secret
     * generation
     */
    fun registerUserK(user: User, k: BigInteger){
        Ks[user] = k
    }

    /**
     * Get User's K value used to
     * conseal his identitty in secret sharing
     */
    fun getUserK(user: User): BigInteger{
        if(user !in Ks){
            throw GameExecutionException("Trying to get K for unknown user")
        }
        return Ks[user]!!
    }
}