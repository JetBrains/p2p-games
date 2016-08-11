package crypto.random

import org.bouncycastle.jce.spec.ECParameterSpec
import org.bouncycastle.math.ec.ECPoint
import java.math.BigInteger
import java.security.SecureRandom
import java.util.*

/**
 * Created by user on 6/24/16.
 */

val secureRandom = SecureRandom()

/**
 * Generate crypto.random string
 * @param n - len of generated string
 */
fun randomString(n: Int): String {
    return BigInteger(6 * n, secureRandom).toString(32).substring(n)
}

/**
 * Generate a random integer in given range
 * @param n - max value(not included)
 */
fun randomInt(n: Int = Int.MAX_VALUE): Int {
    var res = BigInteger(32, secureRandom).toInt() % n
    if (res < 0) {
        res += n
    }
    return res
}

/**
 * Generate a random BigInteger
 * @bits - bit length of generated number
 */
fun randomBigInt(bits: Int): BigInteger {
    return BigInteger(bits, secureRandom)
}

/**
 * Generate a random BigInteger in giver Range
 * @param n - upper bound for generated value
 */
fun randomBigInt(n: BigInteger): BigInteger {
    val bits = n.bitLength()
    var res: BigInteger = BigInteger(bits, secureRandom)
    while (res >= n || res == BigInteger.ZERO) {
        res = BigInteger(bits, secureRandom)
    }
    return res
}

/**
 * Generate a random point on Elliptic curve
 * @param curveSpec - specificatoin of the curve
 */
fun randomECPoint(curveSpec: ECParameterSpec): ECPoint {
    val order = randomBigInt(curveSpec.n)
    return curveSpec.g.multiply(order)
}

/**
 * Create a random permutuation of [0..n-1]
 * @param n - max value(not included)
 */
fun randomPermutation(n: Int): List<Int> {
    val list = (0..n - 1).toList()
    Collections.shuffle(list)
    return list
}

/**
 * shuffle list
 * @param list - list to shuffle
 */
fun <T> shuffle(list: List<T>) {
    Collections.shuffle(list, secureRandom)
}

/**
 * create a shuffled copy of an Array
 * @param array - array to copy and shuffle
 */
fun <T> shuffleArray(array: Array<T>) {
    val shuffled = array.toList()
    shuffle(shuffled)
    for (i in 0..array.size - 1) {
        array[i] = shuffled[i]
    }
}