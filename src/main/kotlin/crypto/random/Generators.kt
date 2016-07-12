package crypto.random

import org.bouncycastle.jce.spec.ECParameterSpec
import org.bouncycastle.math.ec.ECPoint
import java.math.BigInteger
import java.security.SecureRandom
import java.util.*

/**
 * Created by user on 6/24/16.
 */

private val secureRandom = SecureRandom()

/**
 * Generate crypto.random string
 * @param n - len of generated string
 */
fun randomString(n: Int): String {
    return BigInteger(6 * n, secureRandom).toString(32).substring(n)
}

fun randomInt(n: Int = Int.MAX_VALUE): Int {
    var res = BigInteger(32, secureRandom).toInt() % n
    if (res < 0) {
        res += n
    }
    return res
}

fun randomBigInt(bits: Int): BigInteger {
    return BigInteger(bits, secureRandom)
}

fun randomBigInt(n: BigInteger): BigInteger {
    val bits = n.bitLength()
    var res: BigInteger = BigInteger(bits, secureRandom)
    while (res >= n || res == BigInteger.ZERO) {
        res = BigInteger(bits, secureRandom)
    }
    return res
}

fun randomECPoint(curveSpec: ECParameterSpec): ECPoint {
    val order = randomBigInt(curveSpec.n)
    return curveSpec.g.multiply(order)
}

fun randomPermutuation(n: Int): List<Int> {
    val list = (0..n - 1).toList()
    Collections.shuffle(list)
    return list
}

fun <T> shuffle(list: List<T>) {
    Collections.shuffle(list, secureRandom)
}

fun <T> shuffleArray(array: Array<T>) {
    val shuffled = array.toList()
    shuffle(shuffled)
    for (i in 0..array.size - 1) {
        array[i] = shuffled[i]
    }
}