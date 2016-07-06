package apps.games.serious.preference.GUI

import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Pool
import com.badlogic.gdx.utils.Array

/**
 * Created by user on 7/1/16.
 */

/**
 * Class represents card translation
 * Moves cards on table rotating them
 * around Z axis - toAngle/From angle
 * around Y axis - around Y axis
 * (toRatation degrees)
 */
class CardAction(var parent: CardActions) {
    lateinit var card: Card
    val fromPosition = Vector3()
    var fromAngle: Float = 0.toFloat()
    val toPosition = Vector3()
    var toAngle: Float = 0.toFloat()
    var speed: Float = 0.toFloat()
    var alpha: Float = 0.toFloat()
    var toRotation: Float = 0.toFloat()

    fun reset(card: Card) {
        this.card = card
        fromPosition.set(card.position)
        fromAngle = card.angle
        alpha = 0f
    }

    fun update(delta: Float) {
        alpha += delta * speed
        if (alpha >= 1f) {
            alpha = 1f
            parent.actionComplete(this)
        }
        card.position.set(fromPosition).lerp(toPosition, alpha)
        card.angle = fromAngle + alpha * (toAngle - fromAngle)
        card.rotation = toRotation*alpha
        card.update()
    }
}

/***
 * Card action manager. Keeps all
 * cards movements
 */
class CardActions {
    internal var actionPool: Pool<CardAction> = object : Pool<CardAction>() {
        override fun newObject(): CardAction {
            return CardAction(this@CardActions)
        }
    }
    internal var actions = Array<CardAction>()

    fun actionComplete(action: CardAction) {
        actions.removeValue(action, true)
        actionPool.free(action)
    }

    fun update(delta: Float) {
        for (action in actions) {
            action.update(delta)
        }
    }

    fun animate(card: Card, x: Float, y: Float, z: Float, angle: Float, speed: Float, rotation: Float = 0f) {
        val action: CardAction = actionPool.obtain()
        action.reset(card)
        action.toPosition.set(x, y, z)
        action.toAngle = angle
        action.speed = speed
        action.toRotation = rotation
        actions.add(action)
    }
}