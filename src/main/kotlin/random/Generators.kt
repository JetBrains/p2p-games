package random

import java.math.BigInteger
import java.security.SecureRandom

/**
 * Created by user on 6/24/16.
 */

private val secureRandom = SecureRandom()

/**
 * Generate random string
 * @param n - len of generated string
 */
fun randomString(n: Int): String{
    return BigInteger(6*n, secureRandom).toString(32).substring(n)
}