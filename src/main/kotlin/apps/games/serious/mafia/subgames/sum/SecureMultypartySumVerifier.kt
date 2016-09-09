package apps.games.serious.mafia.subgames.sum

import crypto.rsa.RSAKeyManager
import entity.User
import java.math.BigInteger

/**
 * Created by user on 8/25/16.
 *
 * class allows to verify user input to sms
 */

data class SMSEntry(val sender: User, val receiver: User, val msg: String)

class SecureMultypartySumVerifier {
    private lateinit var finalSum: BigInteger
    private val partialSum = mutableMapOf<User, BigInteger>()
    private val messages = mutableListOf<SMSEntry>()
    /**
     * register final finalSum, that was obtained from the game
     *
     * @param sum - final sum that was obtained in SMS
     */
    fun registerFinalSum(sum: BigInteger) {
        finalSum = sum
    }

    /**
     * Register partial finalSum, that should be returned by given user
     *
     * @param user - user whose partial sum to register
     * @param sum - partial sum obtained by that user
     */
    fun registerPartialSum(user: User, sum: BigInteger) {
        partialSum[user] = sum
    }

    /**
     * Register message that was sent
     *
     * @param sender - User, who sent message
     * @param receiver - User, who received message
     * @param msg - encrypted message contents
     */
    fun registerMessage(sender: User, receiver: User, msg: String) {
        messages.add(SMSEntry(sender, receiver, msg))
    }

    /**
     * Given an RSAKeyManager, that holds private keys
     * of all users - restore message history and check, that
     * all sums are correct
     *
     * @param RSAKeyManager - key manager with all private keys registered
     * @return whether verification was successful
     */
    fun verifySums(keyManager: RSAKeyManager): Boolean {
        if (!verifyTotalSum()) {
            return false
        }
        for ((user, sum) in partialSum) {
            val userPart = messages.filter { x -> x.receiver == user }
            var realSum: BigInteger = BigInteger.ZERO
            for (entry in userPart) {
                val split = keyManager.decodeForUser(user, entry.msg).split(" ")
                realSum += BigInteger(split[1])
            }
            if (realSum != partialSum[user]) {
                return false
            }
        }
        return true
    }

    /**
     * Given an RSAKeyManager, that holds private keys
     * of all users - restore original inputs
     *
     * @param RSAKeyManager - key manager with all private keys registered
     * @return Map<User, BigIntger> - map user to their input
     */
    fun getInputs(keyManager: RSAKeyManager): Map<User, BigInteger> {
        val res = mutableMapOf<User, BigInteger>()
        for (user in partialSum.keys) {
            val userPart = messages.filter { x -> x.sender == user }
            var tmp: BigInteger = BigInteger.ZERO
            for (entry in userPart) {
                val split = keyManager.decodeForUser(entry.receiver, entry.msg).split(" ")
                tmp += BigInteger(split[1])
            }
            res[user] = tmp
        }
        return res
    }

    /**
     * check, that partial sums sum up to total sum
     */
    private fun verifyTotalSum(): Boolean {
        return partialSum.values.fold(BigInteger.ZERO, { s: BigInteger, v: BigInteger -> s + v }) == finalSum
    }
}