package apps.games.serious

import apps.chat.Chat
import apps.games.Game
import apps.games.GameInputException
import apps.games.GameManager
import apps.games.primitives.RandomNumberGame
import entity.ChatMessage
import entity.Group
import entity.User
import proto.GameMessageProto
import random.randomInt
import java.util.concurrent.Future

/**
 * Created by user on 6/27/16.
 */

class Lotto(chat: Chat, group: Group, gameID: String, val ticketSize: Int = 5, val maxValue:Int = 30) : Game(chat, group, gameID) {
    private enum class State{
        INIT,
        GENERATE_TICKET,
        RUNNING,
        END
    }

    private var state: State = State.INIT
    private var ticket: Ticket = generateTicket()
    val unused: MutableList<Int> = mutableListOf()
    init{
        for(i in 1..maxValue){
            unused.add(i)
        }
    }

    override fun evaluate(responses: List<GameMessageProto.GameStateMessage>): String {
        when(state){
            State.INIT -> {
                state = State.GENERATE_TICKET
                return ticket.getMD5()
            }
            State.GENERATE_TICKET -> {
                val hashes = responses.map { x -> x.value }
                if(hashes.distinct().size != hashes.size){
                    ticket = generateTicket()
                    return ticket.getMD5()
                }
                state = State.RUNNING

                //Agreement on rng seems to work. Now need to eval each step
                //Plus process game end messages
            }
            State.RUNNING -> {
                //TODO - think about better syntax
                val rngFuture = runSubGame()
                val index: Int
                try{
                    index = rngFuture.get().toInt() % unused.size
                }catch(e: Exception){
                    return ""
                }

                val x = unused.removeAt(index)
                ticket.mark(x)
                if(ticket.win()){
                    chat.sendMessage("I WON! My ticket is |$ticket|")
                    state = State.END
                }
            }
            State.END -> {}
        }
        return ""
    }

    override fun isFinished(): Boolean {
        return state == State.END
    }

    override fun getFinalMessage(): String {
        if(ticket.win()){
            return "I WON! + $ticket"
        }
        return ""
    }

    override fun getVerifier(): String? {
        if(!ticket.win()){
            return null
        }
        return ticket.toString()
    }

    override fun evaluateGameEnd(msg: GameMessageProto.GameEndMessage) {
        val validator = Ticket.getValidator(ticketSize, maxValue)
        if(validator(msg.verifier)){
            val ticket = Ticket.from(ticketSize, maxValue, msg.verifier)
            if(!verifyTicket(ticket)){
                chat.sendMessage("[${msg.user.name}] Cheated!!!")
            }
            chat.sendMessage("I agree, that [${msg.user.name}] won!!!")
        }
        state = State.END
    }

    //TODO - create by class(game factory). Move to Game class
    fun runSubGame(): Future<String> {
        return GameManager.initSubGame(RandomNumberGame(chat, group.clone(), subGameID(), 1, maxValue.toLong()))
    }

    private fun verifyTicket(ticket: Ticket): Boolean{
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

    private fun generateTicket(): Ticket{
        val validator = Ticket.getValidator(ticketSize, maxValue)
        val s = chat.getUserInput("Please generate ticket: type in five values non greater than 30", validator)
        return Ticket.from(ticketSize, maxValue, s)
    }
}
