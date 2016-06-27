package apps.games.primitives

import apps.chat.Chat
import apps.games.Game
import entity.Group
import org.apache.commons.codec.digest.DigestUtils
import proto.GameMessageProto
import random.randomInt
import random.randomString

/**
 * Created by user on 6/27/16.
 */

class RandomNumberGame(chat: Chat, group: Group, gameID: String) : Game(chat, group, gameID) {
    private enum class State{
        INIT,
        GENERATE,
        VALIDATE,
        END
    }
    private var state: State = State.INIT
    private val myRandom: Int = randomInt()
    private var answer: Int = 0
    private val salt: String = randomString(10)
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
                    answer += res
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