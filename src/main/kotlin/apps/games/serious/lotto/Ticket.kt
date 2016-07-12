package apps.games.serious.lotto

import apps.games.GameInputException

/**
 * Created by user on 6/27/16.
 */

class Ticket(val ticketSize: Int) {
    /**
     * List of ticket values
     */
    val numbers: MutableSet<Int> = mutableSetOf()
    private val marked: MutableSet<Int> = mutableSetOf()

    /**
     * GUI if ticket contains give number
     * @param n - number to search
     */
    fun contains(n: Int): Boolean {
        return numbers.contains(n)
    }

    /**
     * If ticket contains a number
     * store it as marked
     * @param n - number to mark
     */
    fun mark(n: Int): Boolean {
        val res = contains(n)
        if (res) {
            marked.add(n)
        }
        return res
    }

    /**
     * check, whether game condition
     * is satisfied
     */
    fun win(): Boolean {
        return marked == numbers
    }

    fun getSHA256(): String {
        return org.apache.commons.codec.digest.DigestUtils.sha256Hex(toString())
    }

    override fun toString(): String {
        return numbers.sorted().joinToString(" ")
    }

    /**
     * Factory for creating tickets
     */
    companion object Factory {
        /**
         * create crypto.random ticket
         * @param ticketSize - size of ticket
         * @param maxValue - Maximum value of number in ticket
         */
        fun randomTicket(ticketSize: Int,
                maxValue: Int): apps.games.serious.lotto.Ticket {
            val res = apps.games.serious.lotto.Ticket(ticketSize)
            while (res.numbers.size < ticketSize) {
                res.numbers.add(crypto.random.randomInt(maxValue))
            }
            return res
        }

        /**
         * create ticket from given list numbers
         * @param ticketSize - size of ticket
         * @param maxValue - Maximum value of number in ticket
         * @param values - List of numbers in ticket
         * @throws GameInputException - if provided list values
         * is inconsistent with other restrictions
         */
        fun buildTicket(ticketSize: Int,
                maxValue: Int,
                values: List<Int>): apps.games.serious.lotto.Ticket {
            val res = apps.games.serious.lotto.Ticket(ticketSize)
            for (value in values) {
                if (value > maxValue) {
                    throw apps.games.GameInputException(
                            "Malformed game user input. Out of bound value found")
                }
                res.numbers.add(value)
            }
            if (res.numbers.size != ticketSize) {
                throw apps.games.GameInputException(
                        "Malformed game user input. Ticked length doesn't correspond to game rules")
            }
            return res
        }

        /**
         * create ticket from given string
         * @param ticketSize - size of ticket
         * @param maxValue - Maximum value of number in ticket
         * @param s - string representation of ticket
         * @throws GameInputException - if provided list values
         * is inconsistent with other restrictions
         */
        fun from(ticketSize: Int,
                maxValue: Int,
                s: String): apps.games.serious.lotto.Ticket {
            if (!(apps.games.serious.lotto.Ticket.Factory.getValidator(
                    ticketSize, maxValue))(s)) {
                throw GameInputException("String $s is not a valid ticket")
            }
            val split: List<String> = s.split(" ")
            return buildTicket(ticketSize, maxValue,
                    split.map { x -> x.toInt() })
        }

        /**
         * create validator, that takes string and says if it is
         * a valid ticket
         * @param ticketSize - size of ticket
         * @param maxValue - Maximum value of number in ticket
         * @return (String)->(Boolean) - validator function
         */
        fun getValidator(ticketSize: Int,
                maxValue: Int): (String) -> (Boolean) {
            fun checker(s: String): Boolean {
                val split: List<String> = s.split(" ")
                if (split.size != ticketSize) {
                    return false
                }
                val was = mutableListOf<Int>()
                for (part in split) {
                    try {
                        val x = part.toInt()
                        if (was.contains(x) || x > maxValue) {
                            return false
                        }
                        was.add(x)
                    } catch(e: NumberFormatException) {
                        return false
                    }
                }
                return true
            }
            return ::checker
        }
    }
}