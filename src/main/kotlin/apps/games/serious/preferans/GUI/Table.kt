package apps.games.serious.preferans.GUI

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable

/**
 * Created by user on 7/4/16.
 */


class Table(val playersCount: Int, val handSize: Int) : Disposable {
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

    fun getPlayerWithCard(card: CardGUI): Player?{
        for(player in players){
            if(player.hand.cards.contains(card)){
                return player
            }
        }
        return null
    }

    fun getMainPlayer(): Player {
        return players[0]
    }


    override fun dispose() {
        tableTopModel.dispose()
    }
}

class Player(val position: Vector3, val direction: Vector3, handSize: Int, val Id: Int, val talbe: Table) {
    val hand = Hand(position, handSize, direction)

    /**
     * get angle between player and center of the table
     */
    fun getAngle(): Float {
        return MathUtils.radDeg * MathUtils.atan2(-direction.x, direction.y)
    }

    /**
     * get place, that selected card sould be put in
     */
    fun getCardspace(): Vector3 {
        return Vector3(position).add(Vector3(direction).scl(3.5f))
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

class Hand(val position: Vector3, val MAX_HAND_SIZE: Int = 6, val direction: Vector3, val MAX_HAND_WIDTH: Float = 3f) {
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
     * find first not revealed card and return an action
     * revealing it
     *
     * @param newCard - new card to be shown in place of revealed
     * @return UNKNOWN card that now need's to be removed from table
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
     * position for next card to put into this hand
     */
    fun nextCardPosition(): Vector3 {
        return getCardPositionById(size)
    }

    /**
     * get position of n-th card in hand
     */
    fun getCardPositionById(n: Int): Vector3 {
        val result = Vector3(position)
        //get perpendicular vector in Z=0 plane
        val normal = Vector3(direction).crs(0f, 0f, 1f).nor()
        val step = MAX_HAND_WIDTH / MAX_HAND_SIZE
        result.add(normal.scl(step * n - 1.4f))
        result.z += (cards.size + 1) * 0.01f
        return result
    }

    /**
     * remove card from hand.
     */
    @Synchronized fun removeCard(card: CardGUI, actionManager: ActionManager){
        val index = cards.indexOf(card)
        if(index == -1){
            return
        }
        for(i in index+1..size-1){
            val pos = cards[i-1].position
            actionManager.add(
                    CardGUI.animate(cards[i], pos.x, pos.y, pos.z, cards[i]
                    .angle, 2f))
        }
        cards.removeAt(index)
    }
}