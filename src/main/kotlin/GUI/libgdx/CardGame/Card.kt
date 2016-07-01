package GUI.libgdx.CardGame

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Pool

/**
 * Created by user on 7/1/16.
 */

enum class Suit(val type: String, val index: Int){
    CLUBS("clubs", 0),
    DIAMONDS("diamonds", 1),
    HEARTS("hearts", 2),
    SPADES("spades", 3)
}

enum class Pip(val value: Int){
    ACE(1),
    TWO(2),
    THREE(3),
    FOUR(4),
    FIVE(5),
    SIX(6),
    SEVEN(7),
    EIGHT(8),
    NINE(9),
    TEN(10),
    JACK(11),
    QUEEN(12),
    KING(13);

    val index = value - 1
}

private fun convert(front: FloatArray, back: FloatArray): FloatArray {
    return floatArrayOf(back[Batch.X2], back[Batch.Y2], 0f, 0f, 0f, 1f, back[Batch.U2], back[Batch.V2],
            back[Batch.X1], back[Batch.Y1], 0f, 0f, 0f, 1f, back[Batch.U1], back[Batch.V1],
            back[Batch.X4], back[Batch.Y4], 0f, 0f, 0f, 1f, back[Batch.U4], back[Batch.V4],
            back[Batch.X3], back[Batch.Y3], 0f, 0f, 0f, 1f, back[Batch.U3], back[Batch.V3],
            front[Batch.X1], front[Batch.Y1], 0f, 0f, 0f, -1f, front[Batch.U1], front[Batch.V1],
            front[Batch.X2], front[Batch.Y2], 0f, 0f, 0f, -1f, front[Batch.U2], front[Batch.V2],
            front[Batch.X3], front[Batch.Y3], 0f, 0f, 0f, -1f, front[Batch.U3], front[Batch.V3],
            front[Batch.X4], front[Batch.Y4], 0f, 0f, 0f, -1f, front[Batch.U4], front[Batch.V4])
}

class Card(val suit: Suit, val pip: Pip, front: Sprite, back: Sprite){
    val CARD_WIDTH = 1f
    val CARD_HEIGHT = CARD_WIDTH * 277f / 200f
    val radius = Vector2(CARD_HEIGHT * 0.5f, CARD_WIDTH * 0.5f).len()
    val verticies: FloatArray
    val indices: ShortArray = shortArrayOf(0, 1, 2, 0, 2, 3, 4, 5, 6, 4, 6, 7)
    val transform = Matrix4()
    val position = Vector3()
    var angle: Float = 0f

    init {
        front.setSize(CARD_WIDTH, CARD_HEIGHT)
        back.setSize(CARD_WIDTH, CARD_HEIGHT)
        front.setPosition(-0.5f * front.width, -0.5f * front.height)
        back.setPosition(-0.5f * back.width, -0.5f * back.height)

        verticies = convert(front.vertices, back.vertices)
    }

    fun update(){
        val z = position.z + 0.5f * Math.abs(MathUtils.sinDeg(angle))
        transform.setToRotation(Vector3.Y, angle)
        transform.trn(position.x, position.y, z)
    }

}

class CardDeck(atlas: TextureAtlas, backIndex: Int) {
    val cards: Array<Array<Card>> = Array(Suit.values().size, { i -> arrayOf<Card>()})

    init {
        for (suit in Suit.values()) {
            val cardList = mutableListOf<Card>()
            for (pip in Pip.values()) {
                val front = atlas.createSprite(suit.type, pip.value)
                val back = atlas.createSprite("back", backIndex)
                cardList.add(Card(suit, pip, back, front))
            }
            cards[suit.index] = arrayOf(*cardList.toTypedArray())
        }
    }

    fun getCard(suit: Suit, pip: Pip): Card {
        return cards[suit.index][pip.index]
    }
}
