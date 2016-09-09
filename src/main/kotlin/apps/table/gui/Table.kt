package apps.table.gui

import apps.games.GameExecutionException
import apps.games.serious.Pip
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable
import crypto.random.randomInt

/**
 * Created by user on 7/4/16.
 */


class Table(val playersCount: Int, var handSize: Int) : Disposable {
    private val tableTopModel: Model
    val tableTop: ModelInstance
    val deckPosition = Vector3(0.5f, -0.5f, 0.1f)
    val MAX_PLAYERS = 8
    val playerSpots: Array<Player>
    val players: Array<Player>
    //cards, that belong to noone, but still on the table
    val commonHand: Hand

    init {
        val angle = 360f / MAX_PLAYERS
        playerSpots = Array(MAX_PLAYERS, { i ->
            Player(Vector3(-1.2f, -8.3f, 0f), Vector3(0f, 1f, 0f), handSize, 0,
                    this)
        })
        for (i in 0..MAX_PLAYERS - 1) {
            val initPos = Vector3(0f, -8.3f, 0f)
            val initVec = Vector3(0f, 1f, 0f)
            playerSpots[i] = Player(initPos.rotate(Vector3.Z, -angle * i),
                    initVec.rotate(Vector3.Z, -angle * i), handSize, i, this)
        }
        players = Array(playersCount,
                { i -> playerSpots[i * (MAX_PLAYERS / playersCount)] })
        commonHand = Hand(Vector3(-1f, -1f, 0f), 6, getMainPlayer().direction,
                6f)
    }

    init {
        val loader = ObjLoader()
        tableTopModel = loader.loadModel(
                Gdx.files.internal("models/poker+table_octagon_base.obj"))
        tableTop = ModelInstance(tableTopModel)
        tableTop.transform.scale(0.4f, 0.4f, 0.4f)
        tableTop.transform.rotate(1f, 0f, 0f, 90f)
        tableTop.transform.rotate(0f, 1f, 0f, 22.5f)
        tableTop.transform.translate(0f, -1.5f, 0f)
    }

    /**
     * Update max Hand Size
     */
    fun updateHandSize(n: Int) {
        handSize = Math.min(n, 13) // Hard to see what is going on if more than 13
        for (player in players) {
            player.hand.MAX_HAND_SIZE = handSize
        }
    }

    /**
     * Given a card gui find a playerId,
     * whose cardspace this card currently
     * belongs
     *
     * @param card - CardGUI to find
     * @return Player - playerId, who holds this
     * card or null, if no such playerId exists
     */
    fun getPlayerWithCard(card: CardGUI): Player? {
        for (player in players) {
            if (player.hand.cards.contains(card)) {
                return player
            }
        }
        return null
    }

    /**
     * get playerId at the head of the table
     */
    fun getMainPlayer(): Player {
        return players[0]
    }

    /**
     * clear and reset the table
     */
    fun clear() {
        commonHand.clear()
        for (player in players) {
            player.hand.clear()
        }
    }

    override fun dispose() {
        tableTopModel.dispose()
    }
}

class Player(val position: Vector3, val direction: Vector3, handSize: Int, val Id: Int, val talbe: Table) {
    val hand = Hand(position, handSize, direction)

    val cardSpaceHand: Hand = Hand(Vector3(position).scl(0.7f), 4, direction, 4.5f)

    var name: String = "Anonymous"

    val nickPosition: Vector3 = Vector3(position).scl(1.22f).add(0f, 0f, 1f)

    /**
     * get angle between playerId and center of the table
     */
    fun getAngle(): Float {
        return MathUtils.radDeg * MathUtils.atan2(-direction.x, direction.y)
    }

    /**
     * get place, that selected cardID sould be put in
     */
    fun getCardspace(): Vector3 {
        return cardSpaceHand.nextCardPosition()
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Player

        if (Id != other.Id) return false

        return true
    }

    override fun hashCode(): Int {
        return Id
    }
}

class Hand(val position: Vector3, var MAX_HAND_SIZE: Int = 6, val direction: Vector3, val MAX_HAND_WIDTH: Float = 3f) {
    val cards = mutableListOf<CardGUI>()
    val size: Int
        get() = cards.size

    /**
     * @see Player#getAngle()
     */
    fun getAngle(): Float {
        return MathUtils.radDeg * MathUtils.atan2(-direction.x, direction.y)
    }

    /**
     * find first not revealed cardID and return an action
     * revealing it
     *
     * @param newCard - new cardID to be shown in place of revealed
     * @return UNKNOWN cardID that now need's to be removed from table
     */
    @Synchronized fun replaceUnknownCard(newCard: CardGUI): CardGUI? {
        for (i in size - 1 downTo 0) {
            if (!cards[i].isRevealed) {
                val res = cards[i]
                cards[i] = newCard
                newCard.position.set(res.position)
                newCard.rotation = res.rotation

                return res
            }
        }
        return null
    }

    /**
     * Replace card at given index with another card
     * @param index - position of card to be replaces
     * @param newCard - card to be inserted
     *
     * @return old card to be removed
     */
    @Synchronized fun replaceCardByIndex(index: Int,
                                         newCard: CardGUI): CardGUI {
        val res = cards[index]
        cards[index] = newCard
        newCard.position.set(res.position)
        newCard.rotation = res.rotation
        return res
    }

    /**
     * get random unknown card from players hand
     */
    fun randomUnknownCard(): CardGUI {
        val idx = randomInt(size)
        for (i in 0..size - 1) {
            if (cards[idx + i].pip == Pip.UNKNOWN) {
                return cards[idx + i]
            }
        }
        throw GameExecutionException("Player has no unknown cards")
    }

    /**
     * position for next cardID to put into this hand
     */
    fun nextCardPosition(): Vector3 {
        return getCardPositionById(size)
    }

    /**
     * get position of n-th cardID in hand
     */
    fun getCardPositionById(n: Int): Vector3 {
        val result = Vector3(position)
        //get perpendicular vector in Z=0 plane
        val normal = Vector3(direction).crs(0f, 0f, 1f).nor()
        val y = n / MAX_HAND_SIZE
        val x = n % MAX_HAND_SIZE
        val stepX = MAX_HAND_WIDTH / MAX_HAND_SIZE
        result.add(Vector3(position).nor().scl(0.5f * y))
        result.add(normal.scl(stepX * x - 1.4f))
        result.z += (n + 1) * 0.01f
        return result
    }

    /**
     * clear hand info for reusage
     */
    @Synchronized fun clear() {
        for (card in cards) {
            card.reset()
        }
        cards.clear()
    }

    /**
     * remove cardID from hand.
     */
    @Synchronized fun removeCard(card: CardGUI, actionManager: ActionManager) {
        var index: Int = -1
        for (i in 0..size - 1) {
            if (cards[i] === card) {
                index = i
                break
            }
        }
        if (index == -1) {
            index = cards.indexOf(card)
            if (index == -1) {
                return
            }
        }
        for (i in index + 1..size - 1) {
            val pos = getCardPositionById(i - 1)
            actionManager.add(
                    CardGUI.animate(cards[i], pos.x, pos.y, pos.z, cards[i]
                            .angle, 2f, cards[i].rotation))
        }
        cards.removeAt(index)
    }
}