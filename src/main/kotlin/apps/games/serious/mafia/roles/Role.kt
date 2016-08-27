package apps.games.serious.mafia.roles

import apps.games.serious.Pip
import apps.games.serious.Suit
import crypto.AES.AESEncryptor
import entity.User
import org.bouncycastle.math.ec.ECPoint

/**
 * Created by user on 8/10/16.
 */

abstract class PlayerRole{
    /**
     * Role of player
     */
    abstract val role: Role

    private lateinit var IV: ECPoint
    private val encryptor = AESEncryptor()
    private val comrades = mutableSetOf<User>()

    /**
     * If role is represented with multiple
     * instances they might want to communicate
     *
     * @return list of known comrades include self
     */
    open fun getComrades(): Set<User>{
        return comrades
    }

    /**
     * Assuming we share a secret with comrades - encrypt
     * a message for them
     *
     * @param msg - message to encrypt
     * @return String - encrypted message
     */
    fun encryptForComrades(msg: String): String{
        return encryptor.encrypt(msg)
    }

    /**
     * add user to the list of known comrades
     */
    fun registerComrade(user: User){
        comrades.add(user)
    }

    /**
     * Assuming we share a secret with comrades - decrypt
     * a message for them
     *
     * @param msg - message to decrypt
     * @return String - decrypted message
     */
    fun decryptForComrades(msg: String): String{
        return encryptor.decrypt(msg)
    }

    fun registerIV(IV: ECPoint){
        this.IV = IV
        encryptor.init(IV.getEncoded(false))
    }
}

enum class Role(val id: Int, val pip: Pip, val suit: Suit){
    UNKNOWN(-1, Pip.UNKNOWN, Suit.UNKNOWN),
    MAFIA(1, Pip.ACE, Suit.SPADES),
    DOCTOR(2, Pip.QUEEN, Suit.HEARTS),
    DETECTIVE(3, Pip.ACE, Suit.DIAMONDS),
    INNOCENT(4, Pip.SIX, Suit.CLUBS);

    fun createPLayerRole(): PlayerRole{
        return when(this){
            Role.UNKNOWN -> UnknownRole()
            Role.MAFIA -> MafiaRole()
            Role.DOCTOR -> DoctorRole()
            Role.DETECTIVE -> DetectiveRole()
            Role.INNOCENT -> InnocentRole()
        }
    }
}