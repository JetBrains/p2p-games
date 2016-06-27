package apps.games.serious

import apps.chat.Chat
import apps.games.Game
import apps.games.GameInputException
import apps.games.GameManager
import entity.Group
import proto.GameMessageProto
import random.randomInt

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
                val rngFuture = GameManager.initSubGame(group, chat, gameID + "N")
                val x: String = rngFuture.get()
                print(x)
                //Agreement on rng seems to work. Now need to eval each step
                //Plus process game end messages
            }
            State.RUNNING -> {

            }
            State.END -> {

            }
        }
        return ""
    }

    override fun isFinished(): Boolean {
        return state == State.END
    }

    override fun getFinalMessage(): String {
        return super.getFinalMessage()
    }

    override fun getResult(): String {
        return super.getResult()
    }

    override fun getInitialMessage(): String {
        return super.getInitialMessage()
    }

    private fun generateTicket(): Ticket{
        val validator = Ticket.getValidator(ticketSize, maxValue)
        val s = chat.getUserInput("Please generate ticket: type in five values non greater than 30", validator)
        return Ticket.from(ticketSize, maxValue, s)
    }
}
