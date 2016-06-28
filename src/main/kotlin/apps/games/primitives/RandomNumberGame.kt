package apps.games.primitives

import apps.chat.Chat
import apps.games.Game
import entity.ChatMessage
import entity.Group
import entity.User
import org.apache.commons.codec.digest.DigestUtils
import proto.GameMessageProto
import random.randomInt
import random.randomString

/**
 * Created by user on 6/27/16.
 */

class RandomNumberGame(chat: Chat, group: Group,
                       gameID: String, val minValue: Long = Int.MIN_VALUE.toLong(),
                       val maxValue: Long = Int.MAX_VALUE.toLong()) : Game(chat, group, gameID) {


    private val offset = minValue
    private val n: Long = maxValue - minValue + 1

    private enum class State{
        INIT,
        GENERATE,
        VALIDATE,
        END
    }
        private var state: State = State.INIT
    private val myRandom: Int = randomInt()
    private var answer: Long = 0
    private val salt: String = randomString(100)
    private val hashes: MutableSet<String> = mutableSetOf()
    private var agreed: Boolean = true

    override fun evaluate(responses: List<GameMessageProto.GameStateMessage>): String {
        when(state){
            State.INIT -> {
                state = State.GENERATE
                return DigestUtils.md5Hex(myRandom.toString() + salt)
            }
            State.GENERATE -> {
                state = State.VALIDATE

                for(msg in responses){
                    chat.showMessage(ChatMessage(chat.chatId, User(msg.user), "Hash: ${msg.value}"))
                    hashes.add(msg.value)
                }
                return myRandom.toString() + " " + salt
            }
            State.VALIDATE -> {
                for(msg in responses){
                    val res = checkAnswer(msg.value)
                    if(res == null){
                        agreed = false
                        break
                    }
                    chat.showMessage(ChatMessage(chat.chatId, User(msg.user), "User data: ${msg.value}"))
                    answer += res
                    answer %= n
                    if(answer < 0){
                        answer += n
                    }
                    answer += offset
                }
                state = State.END
                return ""
            }
            State.END -> {
                return ""
            }
        }
    }

    override fun isFinished(): Boolean {
        return state == State.END
    }

    override fun getFinalMessage(): String{
        if(agreed){
            return "Everything is OK!\n Agreed on $answer"
        }else{
            return "Someone cheated"
        }
    }

    override fun getResult(): String {
        return answer.toString()
    }

    private fun checkAnswer(s: String): Int?{
        val split = s.split(" ")
        if(split.size != 2){
            return null
        }
        val tomd5: String = split.joinToString("")
        if(!hashes.contains(DigestUtils.md5Hex(tomd5))){
            return null
        }
        return split[0].toInt()
    }

}