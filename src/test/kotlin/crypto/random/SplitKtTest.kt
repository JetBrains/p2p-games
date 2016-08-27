package crypto.random

import crypto.RSA.ECParams
import org.junit.Assert.*
import org.junit.Test
import java.math.BigInteger

/**
 * Created by user on 8/25/16.
 */
class SplitKtTest{
    val SPLIT_COUNT = 50

    /**
     * Test, that split function works correct
     * on splits of small numbers
     */
    @Test
    fun testSplitZerosSmall(){
        val n = BigInteger.valueOf(100)
        val tests = 100
        for(i in 0..tests){
            val split = crypto.random.split(n, SPLIT_COUNT, true)
            var sum: BigInteger = BigInteger.ZERO
            for(v in split){
                sum += v
            }
            assertEquals(n, sum)
            assertEquals(SPLIT_COUNT, split.size)
        }
    }

    /**
     * Test, that split function works correct
     * on splits of large numbers
     */
    @Test
    fun testSplitZerosLarge(){
        val n = randomBigInt(ECParams.n)
        val tests = 100
        for(i in 0..tests){
            val split = crypto.random.split(n, SPLIT_COUNT, true)
            var sum: BigInteger = BigInteger.ZERO
            for(v in split){
                sum += v
            }
            assertEquals(n, sum)
            assertEquals(SPLIT_COUNT, split.size)
        }
    }


    /**
     * Test, that split function works correct
     * on splits of small numbers, and produces no zeros
     */
    @Test
    fun testSplitNoZerosSmall(){
        val n = BigInteger.valueOf(100)
        val tests = 100
        for(i in 0..tests){
            val split = crypto.random.split(n, SPLIT_COUNT, false)
            var sum: BigInteger = BigInteger.ZERO
            for(v in split){
                sum += v
            }
            assertEquals(n, sum)
            assertFalse(split.contains(BigInteger.ZERO))
            assertEquals(SPLIT_COUNT, split.size)
        }
    }


    /**
     * Test, that split function works correct
     * on splits of large numbers, and produces no zeros
     */
    @Test
    fun testSplitNoZerosLarge(){
        val n = randomBigInt(ECParams.n)
        val tests = 100
        for(i in 0..tests){
            val split = crypto.random.split(n, SPLIT_COUNT, true)
            var sum: BigInteger = BigInteger.ZERO
            for(v in split){
                sum += v
            }
            assertEquals(n, sum)
            assertFalse(split.contains(BigInteger.ZERO))
            assertEquals(SPLIT_COUNT, split.size)
        }
    }
}