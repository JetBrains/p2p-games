package apps.games.primitives.protocols

import apps.chat.Chat
import apps.games.Game
import apps.games.primitives.Deck
import entity.Group
import org.bouncycastle.math.ec.ECCurve
import org.bouncycastle.math.ec.ECPoint
import proto.GameMessageProto

/**
 * Created by user on 7/4/16.
 */


class CardShuffleGame(chat: Chat, group: Group, gameID: String, val ecCurve: ECCurve) : Game<Deck>(chat, group, gameID) {
    override fun getResult(): Deck {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val name: String
        get() = "Card Shuffle"

    private enum class State{
        INIT,
        GENERATE,
        VALIDATE,
        END
    }
    private var state: State = State.INIT


    override fun isFinished(): Boolean {
        return state == State.END
    }
    override fun evaluate(responses: List<GameMessageProto.GameStateMessage>): String {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}