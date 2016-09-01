package apps.games.serious.mafia.roles

import apps.games.GameExecutionException
import crypto.RSA.ECParams
import crypto.RSA.RSAKeyManager
import entity.User
import org.bouncycastle.math.ec.ECPoint
import java.math.BigInteger

/**
 * Created by user on 8/24/16.
 */
class DetectiveRole : PlayerRole() {
    override val role: Role
        get() = Role.DETECTIVE
    private val Ks = mutableMapOf<User, BigInteger>()
    private val targets = mutableMapOf<User, Boolean>()
    private val keyManager = RSAKeyManager()
    private lateinit var currentTargetId: BigInteger
    private lateinit var currentTargetUser: User
    /**
     * Detective knowns keys [K_i] that were used in secret
     * generation
     */
    fun registerUserK(user: User, k: BigInteger) {
        Ks[user] = k
    }

    /**
     * Get User's K value used to
     * conseal his identitty in secret sharing
     */
    fun getUserK(user: User): BigInteger {
        if (user !in Ks) {
            throw GameExecutionException("Trying to get K for unknown user")
        }
        return Ks[user]!!
    }

    /**
     * Register, that last target of this doctor was user
     * with given id
     */
    fun registerTarget(user: User, id: BigInteger) {
        currentTargetId = id
        currentTargetUser = user
    }

    fun registerTargetResult(id: BigInteger, isMafia: Boolean) {
        if (id != currentTargetId) {
            throw GameExecutionException("Someone interfered with detective investigation")
        }
        targets[currentTargetUser] = isMafia
    }

    /**
     * get public key for role transmition
     */
    fun getPublicExponent(): BigInteger {
        return keyManager.getExponent()
    }

    /**
     * get modulus for role transmition
     */
    fun getModulus(): BigInteger {
        return keyManager.getModulus()
    }

    /**
     * decode secret part (EC point) encoded with parameters
     * obtained from [getPublicExponent] and [getModulus]
     */
    fun decodeSecretPart(msg: String): ECPoint {
        try {
            return ECParams.curve.decodePoint(keyManager.decodeBytes(msg))
        } catch (e: Exception) {
            throw GameExecutionException("Someone tried to interfere with detectiveRSA")
        }
    }

    /**
     * given real distribution of roles check,
     * that all results obtained from out checks were
     * correct
     */
    fun verifyChecks(roles: Map<User, Role>): Boolean {
        return targets.all { x -> x.value == (roles[x.key] == Role.MAFIA) }
    }

    /**
     * reset evrything in parrent +
     * create new RSA key pair
     */
    override fun reset() {
        super.reset()
        keyManager.reset()
    }

    companion object {
        val TIMEOUT: Long = 1

        val KEY_LENGTH = 1024
    }
}