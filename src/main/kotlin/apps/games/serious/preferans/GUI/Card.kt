package apps.games.serious.preferans.GUI

import apps.games.serious.preferans.Pip
import apps.games.serious.preferans.Suit
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3

/**
 * Created by user on 7/1/16.
 */


/**
 * We need this to get meshes of cards
 */
private fun convert(front: FloatArray, back: FloatArray): FloatArray {
    return floatArrayOf(back[Batch.X2], back[Batch.Y2], 0f, 0f, 0f, 1f,
            back[Batch.U2], back[Batch.V2],
            back[Batch.X1], back[Batch.Y1], 0f, 0f, 0f, 1f, back[Batch.U1],
            back[Batch.V1],
            back[Batch.X4], back[Batch.Y4], 0f, 0f, 0f, 1f, back[Batch.U4],
            back[Batch.V4],
            back[Batch.X3], back[Batch.Y3], 0f, 0f, 0f, 1f, back[Batch.U3],
            back[Batch.V3],
            front[Batch.X1], front[Batch.Y1], 0f, 0f, 0f, -1f, front[Batch.U1],
            front[Batch.V1],
            front[Batch.X2], front[Batch.Y2], 0f, 0f, 0f, -1f, front[Batch.U2],
            front[Batch.V2],
            front[Batch.X3], front[Batch.Y3], 0f, 0f, 0f, -1f, front[Batch.U3],
            front[Batch.V3],
            front[Batch.X4], front[Batch.Y4], 0f, 0f, 0f, -1f, front[Batch.U4],
            front[Batch.V4])
}

/**
 * Card GUI representation
 */
class Card(val suit: Suit, val pip: Pip, front: Sprite, back: Sprite) {
    val CARD_WIDTH = 1f
    val CARD_HEIGHT = CARD_WIDTH * 277f / 200f
    val radius = CARD_WIDTH / 2f
    val verticies: FloatArray
    val indices: ShortArray = shortArrayOf(0, 1, 2, 0, 2, 3, 4, 5, 6, 4, 6, 7)
    val transform = Matrix4()
    val position = Vector3()
    var angle: Float = 0f
    var rotation: Float = 0f
    var isSelected: Boolean = false
    var isRevealed: Boolean = false

    init {
        front.setSize(CARD_WIDTH, CARD_HEIGHT)
        back.setSize(CARD_WIDTH, CARD_HEIGHT)
        front.setPosition(-0.5f * front.width, -0.5f * front.height)
        back.setPosition(-0.5f * back.width, -0.5f * back.height)

        verticies = convert(front.vertices, back.vertices)
    }

    fun update() {
        val z = position.z + 0.5f * Math.abs(MathUtils.sinDeg(angle))
        transform.setToRotation(Vector3.Y, angle)
        transform.rotate(Vector3.Z, rotation)
        transform.trn(position.x, position.y, z)
    }


    companion object {
        //TODO - Maybe cache actions on cards
        fun animate(card: Card,
                x: Float,
                y: Float,
                z: Float,
                angle: Float,
                speed: Float,
                rotation: Float = 0f,
                delay: Float = 0.1f): CardAction {
            val action = CardAction(delay)
            action.reset(card)
            action.toPosition.set(x, y, z)
            action.toAngle = angle
            action.speed = speed
            action.toRotation = rotation
            return action
        }
    }


}

/**
 * Class represents card translation
 * Moves cards on table rotating them
 * around Z axis - toAngle/From angle
 * around Y axis - around Y axis
 * (toRatation degrees)
 */
class CardAction(delay: Float) : Action(delay) {
    lateinit var card: Card
    val fromPosition = Vector3()
    var fromAngle: Float = 0.toFloat()
    val toPosition = Vector3()
    var toAngle: Float = 0.toFloat()
    var speed: Float = 0.toFloat()
    var alpha: Float = 0.toFloat()
    var toRotation: Float = 0.toFloat()
    var finished: Boolean = false

    fun reset(card: Card) {
        this.card = card
        fromPosition.set(card.position)
        fromAngle = card.angle
        alpha = 0f
    }


    override fun execute(delta: Float) {
        alpha += delta * speed
        if (alpha >= 1f) {
            alpha = 1f
            finished = true
        }
        card.position.set(fromPosition).lerp(toPosition, alpha)
        card.angle = fromAngle + alpha * (toAngle - fromAngle)
        card.rotation = toRotation * alpha
        card.update()
    }

    override fun isComplete(): Boolean {
        return finished
    }
}

/**
 * Holds all cards in deck. Doesn't load
 * cards twice(except UNKNOWN cards)
 */
class CardDeck(val atlas: TextureAtlas, val backIndex: Int) {
    val cards: Array<Array<Card>> = Array(Suit.values().size,
            { i -> arrayOf<Card>() })
    val back = atlas.createSprite("back", backIndex)

    init {
        for (suit in Suit.values()) {
            if (suit == Suit.UNKNOWN) {
                continue
            }
            val cardList = mutableListOf<Card>()
            for (pip in Pip.values()) {
                if (pip == Pip.UNKNOWN) {
                    continue
                }
                val front = atlas.createSprite(suit.type, pip.value)
                cardList.add(Card(suit, pip, back, front))
            }
            cards[suit.index] = arrayOf(*cardList.toTypedArray())
        }
    }

    fun getCard(suit: Suit, pip: Pip): Card {
        if (suit == Suit.UNKNOWN || pip == Pip.UNKNOWN) {
            return Card(Suit.UNKNOWN, Pip.UNKNOWN, back,
                    atlas.createSprite("back", (backIndex + 1) % 4))
        }
        cards[suit.index][pip.index].isRevealed = true
        return cards[suit.index][pip.index]
    }
}
