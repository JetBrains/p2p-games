package crypto.random

import java.math.BigDecimal
import java.math.BigInteger

/**
 * Created by user on 8/25/16.
 */

/**
 * Split number into random parts
 *
 * @param n - number to split
 * @param parts - number of split parts
 * @param zeros - whether zero value split are allowed
 *
 * @return List<BigInteger> - resulting splits
 */
fun split(n: BigInteger, parts: Int, zeros: Boolean = false): List<BigInteger> {
    val v = BigDecimal(n)
    if (parts <= 1 || (!zeros && n < BigInteger.valueOf(parts.toLong()))) {
        throw IllegalArgumentException("invalid number of split parts")
    }
    val points = Array(parts, { i -> BigDecimal(secureRandom.nextDouble()) })
    points.sort()
    val result = mutableListOf<BigInteger>()
    var s: BigInteger = BigInteger.ZERO
    for (i in 0..parts - 2) {
        val tmp = (v * points[i]).toBigInteger() - s
        if (tmp == BigInteger.ZERO && !zeros) {
            result.add(BigInteger.ONE)
        } else {
            result.add(tmp)
        }
        s += result[i]
    }
    val tmp = n - s
    if (tmp == BigInteger.ZERO && !zeros) {
        for (i in 0..parts - 3) {
            if (result[i + 1] - result[i] > BigInteger.ONE) {
                result[i + 1] -= BigInteger.ONE
                result.add(BigInteger.ONE)
                break
            }
        }
    } else {
        result.add(tmp)
    }
    return result
}