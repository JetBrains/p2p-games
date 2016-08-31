package apps.games.serious.mafia.roles

import java.math.BigInteger

/**
 * Created by user on 8/24/16.
 */
class MafiaRole : PlayerRole() {
    override val role: Role
        get() = Role.MAFIA

    private lateinit var currentTarget: BigInteger

    /**
     * Register, that last target of this mafia was user
     * with given id
     */
    fun registerTarget(id: BigInteger){
        currentTarget = id
    }

    /**
     * check, that current target pick is consistent with previous picks
     */
    fun verifyTarget(id: BigInteger): Boolean{
        return currentTarget == id
    }


    companion object{
        val MESSAGE_INPUT_TIMEOUT: Long = 10
        val TARGET_CHOICE_TIMEOUT: Long = 20
    }
}