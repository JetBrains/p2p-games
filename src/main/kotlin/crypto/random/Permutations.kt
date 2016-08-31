package crypto.random

/**
 * Created by user on 8/24/16.
 */

/**
 * Shuffle arary with permutation
 * with given permutation
 *
 * @param collection - collection to shuffle
 * @param permutation - List of indicies in permutuation. TODO - move to bouncycastle permutations
 */
fun <T> shuffle(collection: Collection<T>, permutation: List<Int>): List<T> {
    val size = collection.size
    if (size != permutation.size) {
        throw IllegalArgumentException("Permutation size doesn't match deck size")
    }
    val res = mutableListOf<T>()
    for (x in permutation) {
        res.add(collection.elementAt(x))
    }
    return res
}

/**
 * Given a collection shuffle it in blocks of given size
 * @param collection - collection to shuffle
 * @param permutation - shuffle permutation
 * @param blockSize - size of shuffle block
 */
fun <T> shuffle(collection: Collection<T>,
                permutation: List<Int>,
                blockSize: Int): List<T> {
    val tmp = mutableListOf<List<T>>()
    var t = mutableListOf<T>()
    for (element in collection) {
        t.add(element)
        if (t.size >= blockSize) {
            tmp.add(t)
            t = mutableListOf<T>()
        }
    }
    if (t.isNotEmpty()) {
        tmp.add(t)
    }
    val res = shuffle(tmp, permutation)
    return res.flatten()
}
