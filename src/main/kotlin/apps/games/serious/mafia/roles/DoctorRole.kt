package apps.games.serious.mafia.roles

import java.math.BigInteger

/**
 * Created by user on 8/24/16.
 */
class DoctorRole : PlayerRole() {
    override val role: Role
        get() = Role.DOCTOR

    private var currentTarget: BigInteger = BigInteger.ZERO

    /**
     * Register, that last target of this doctor was user
     * with given id
     */
    fun registerTarget(id: BigInteger) {
        currentTarget = id
    }

    companion object {
        val TIMEOUT: Long = 5
    }
}