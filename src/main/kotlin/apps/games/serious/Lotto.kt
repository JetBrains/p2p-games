package apps.games.serious

import apps.chat.Chat
import apps.games.Game
import apps.games.GameExecutionException
import apps.games.GameInputException
import apps.games.GameManager
import apps.games.primitives.RandomNumberGame
import entity.ChatMessage
import entity.Group
import entity.User
import proto.GameMessageProto
import random.randomInt
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future

/**
 * Created by user on 6/27/16.
 */

class Lotto(chat: Chat, group: Group, gameID: String, val ticketSize: Int = 5, val maxValue:Int = 30) : Game(chat, group, gameID) {
    override val name: String
        get() = "Lotto"

    /**
     * Lotto game has four states:
     * INIT - Game just started
     *
     * GENERATE_TICKET - all players
     * generate distinct Lotto tickets
     *
     * RUNNING - polling random numbers
     *
     * END - someone won
     */
    private enum class State{
        INIT,
        GENERATE_TICKET,
        RUNNING,
        END
    }

    private var state: State = State.INIT
    private var ticket: Ticket = generateTicket()
    private val initialTicketHashes: MutableMap<User, String> = mutableMapOf()
    val unused: MutableList<Int> = mutableListOf()
    init{
        for(i in 1..maxValue){
            unused.add(i)
        }
    }

    /**
     * Simple DFA describes Lotto game
     */
    @Synchronized override fun evaluate(responses: List<GameMessageProto.GameStateMessage>): String {
        when(state){
            State.INIT -> { //Game just started. Generate initial ticket
                state = State.GENERATE_TICKET
                return ticket.getMD5()
            }
            State.GENERATE_TICKET -> { // Generate
                val hashes = responses.map { x -> x.value }
                if(hashes.distinct().size != hashes.size){
                    ticket = generateTicket()
                    return ticket.getMD5()
                }
                for(msg in responses){
                    initialTicketHashes[User(msg.user)] = msg.value
                }
                state = State.RUNNING
            }
            State.RUNNING -> {
                val rngFuture = runSubGame(RandomNumberGame(chat, group.clone(), subGameID(), 1, maxValue.toLong()))
                val result: String
                try{
                    result = rngFuture.get()
                }catch(e: CancellationException){ // Task was cancelled - means that we need to stop. NOW!
                    state = State.END
                    return ""
                }catch(e: ExecutionException){
                    chat.showMessage(ChatMessage(chat, e.message?: "Something went wrong"))
                    throw GameExecutionException("Subgame failed")
                }
                val index: Int
                try{
                    index = result.toInt() % unused.size
                }catch(e: Exception){
                    throw GameExecutionException("Subgame returned unexpected result")
                }
                chat.showMessage(ChatMessage(chat, "Iteration complete"))
                val x = unused.removeAt(index)
                ticket.mark(x)
                if(ticket.win()){
                    chat.sendMessage(getFinalMessage())
                    state = State.END
                }

            }
            State.END -> {}
        }
        return ""
    }

    /**
     * Game either finished naturally or was cancelled
     */
    override fun isFinished(): Boolean {
        return state == State.END
    }

    /**
     * Don's say anything if didn't win
     */
    override fun getFinalMessage(): String {
        if(ticket.win()){
            return "I WON! My ticket is |$ticket|"
        }
        return ""
    }

    /**
     * proof that we won(if we have one)
     */
    override fun getVerifier(): String? {
        if(!ticket.win()){
            return null
        }
        return ticket.toString()
    }

    /**
     * remove user from game group - we don't
     * expect any messages from him anymore.
     * If he claimed, that he won - check it
     */
    override fun evaluateGameEnd(msg: GameMessageProto.GameEndMessage) {
        group.users.remove(User(msg.user))
        val validator = Ticket.getValidator(ticketSize, maxValue)
        if(validator(msg.verifier)){
            val ticket = Ticket.from(ticketSize, maxValue, msg.verifier)
            if(initialTicketHashes[User(msg.user)] != ticket.getMD5()){
                chat.sendMessage("[${msg.user.name}] Cheated!!! It was not his initial ticket")
            }
            if(!verifyTicket(ticket)){
                chat.sendMessage("[${msg.user.name}] Cheated!!! This ticket hasn't won yet")
            }
            chat.sendMessage("I agree, that [${msg.user.name}] won!!!")
        }
        cancelSubgames()
        synchronized(state){
            state = State.END
        }


    }

    /**
     * check if we won
     */
    fun win(): Boolean{
        return ticket.win()
    }

    /**
     * set Ticket to given value
     */
    fun setTicket(ticket: Ticket){
        this.ticket = ticket
    }

    /**
     * Given a ticket - verify, that it
     * 1)Was mentioned before game
     * 2)Contains only used numbers
     */
    fun verifyTicket(ticket: Ticket): Boolean{
        val validator = Ticket.getValidator(ticketSize, maxValue)
        if(!validator(ticket.toString())){
            return false
        }
        for(number in ticket.numbers){
            if(unused.contains(number)){
                return false
            }
        }
        return true
    }

    /**
     * get ticket from user
     */
    private fun generateTicket(): Ticket{
        val validator = Ticket.getValidator(ticketSize, maxValue)
        val s = chat.getUserInput("Please generate ticket: type in five values non greater than 30", validator)
        return Ticket.from(ticketSize, maxValue, s)
    }
}
