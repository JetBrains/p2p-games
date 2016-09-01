package apps.games.serious.mafia.roles

import apps.games.serious.Card
import apps.games.serious.Pip
import apps.games.serious.Suit
import crypto.AES.AESEncryptor
import entity.User
import org.bouncycastle.math.ec.ECPoint

/**
 * Created by user on 8/10/16.
 */

abstract class PlayerRole {
    /**
     * Role of playerId
     */
    abstract val role: Role

    private lateinit var IV: ECPoint
    private var encryptor = AESEncryptor()
    private var comrades = mutableSetOf<User>()

    /**
     * If role is represented with multiple
     * instances they might want to communicate
     *
     * @return list of known comrades include self
     */
    open fun getComrades(): Set<User> {
        return comrades
    }

    /**
     * Assuming we share a secret with comrades - encrypt
     * a message for them
     *
     * @param msg - message to encrypt
     * @return String - encrypted message
     */
    fun encryptForComrades(msg: String): String {
        return encryptor.encrypt(msg)
    }

    /**
     * add user to the list of known comrades
     */
    fun registerComrade(user: User) {
        comrades.add(user)
    }

    /**
     * Assuming we share a secret with comrades - decrypt
     * a message for them
     *
     * @param msg - message to decrypt
     * @return String - decrypted message
     */
    fun decryptForComrades(msg: String): String {
        return encryptor.decrypt(msg)
    }

    /**
     * Get index of given user among commrades
     */
    fun getPlayerIndex(user: User): Int {
        return comrades.sortedBy { x -> x.name }.indexOf(user)
    }

    fun registerIV(IV: ECPoint) {
        this.IV = IV
        encryptor.init(IV.getEncoded(false))
    }

    /**
     * clear role for later reuser
     */
    @Synchronized open fun reset() {
        encryptor = AESEncryptor()
        comrades = mutableSetOf<User>()
    }
}

enum class Role(val id: Int, val playerRole: PlayerRole) {
    UNKNOWN(-1, UnknownRole()),
    MAFIA(1, MafiaRole()),
    DOCTOR(2, DoctorRole()),
    DETECTIVE(3, DetectiveRole()),
    INNOCENT(4, InnocentRole());

    fun getCard(index: Int): Card {
        return when (this) {
            Role.UNKNOWN -> Card(Suit.UNKNOWN, Pip.UNKNOWN)
            Role.MAFIA -> Card(Suit.SPADES, Pip.values()[Pip.KING.value - index])
            Role.DOCTOR -> Card(Suit.HEARTS, Pip.values()[Pip.QUEEN.value - index])
            Role.DETECTIVE -> Card(Suit.DIAMONDS, Pip.values()[Pip.KING.value - index])
            Role.INNOCENT -> Card(Suit.CLUBS, Pip.values()[Pip.TEN.value - index])
        }
    }

    companion object {
        fun reset() {
            for (role in Role.values()) {
                role.playerRole.reset()
            }
        }
    }
}