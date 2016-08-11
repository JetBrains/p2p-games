package apps.games.serious.mafia.Roles

import entity.User

/**
 * Created by user on 8/10/16.
 */

abstract class PlayerRole{
    /**
     * Role of player
     */
    abstract val role: Role

    /**
     * If role is represented with multiple
     * instances they might want to communicate
     *
     * @return list of known comrades include self
     */
    open fun getComrades(): Collection<User>{
        throw UnsupportedOperationException()
    }

    /**
     * Assuming we share a secret with comrades - encrypt
     * a message for them
     *
     * @param msg - message to encrypt
     * @return String - encrypted message
     */
    open fun encryptForComrades(msg: String): String{
        throw UnsupportedOperationException()
    }
}

enum class Role(val id: Int){
    UNKNOWN(-1),
    INNOCENT(0),
    MAFIA(1),
    DOCTOR(2),
    DETECTIVE(3)
}