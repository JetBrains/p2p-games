package apps.games.serious.Cheat.logger


import apps.games.GameExecutionException
import apps.games.serious.ShuffledDeck


/**
 * Created by user on 7/15/16.
 */


class CheatGameLogger(val N: Int) {
    private val pastLogs = mutableListOf<RoundLogger>()
    private var currentLogger: RoundLogger? = null
    val log: RoundLogger
        get() {
            if (currentLogger == null) {
                throw GameExecutionException("Acessing logger before initialization")
            }
            return currentLogger as RoundLogger
        }

    fun newRound(deckSize: Int, shuffledDeck: ShuffledDeck) {
        if (currentLogger != null) {
            pastLogs.add(currentLogger!!.clone())
        }
        currentLogger = RoundLogger(N, deckSize, shuffledDeck)
    }

    /**
     * format complete game log
     */
    fun formatLog(): String {
        return pastLogs.map { x -> x.formatLog() }.joinToString("=================")
    }


}
