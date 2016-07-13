package apps.games.serious.preferans.GUI

import apps.games.serious.preferans.Bet
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import entity.User

/**
 * Created by user on 7/7/16.
 */


class BiddingOverlay {
    lateinit var skin: Skin
    lateinit var stage: Stage
    lateinit var batch: SpriteBatch
    lateinit var table: com.badlogic.gdx.scenes.scene2d.ui.Table
    private val buttons = mutableMapOf<Bet, TextButton>()
    var isVisible = false

    /**
     * Create layout for bidding overlay
     */
    fun create() {
        batch = SpriteBatch()
        stage = Stage()
        table = com.badlogic.gdx.scenes.scene2d.ui.Table()

        table.setPosition(500f, 500f)
        // A skin can be loaded via JSON or defined programmatically, either is fine. Using a skin is optional but strongly
        // recommended solely for the convenience of getting a texture, region, etc as a drawable, tinted drawable, etc.
        skin = Skin()
        // Generate a 1x1 white texture and store it in the skin named "white".
        val pixmap = Pixmap(80, 80, Pixmap.Format.RGBA8888)
        pixmap.setColor(Color.GREEN)
        pixmap.fill()

        skin.add("white", Texture(pixmap))

        // Store the default libgdx font under the name "default".
        val bfont = BitmapFont()
        skin.add("default", bfont)

        // Configure a TextButtonStyle and name it "default". Skin resources are stored by type, so this doesn't overwrite the font.
        val textButtonStyle = TextButton.TextButtonStyle()
        textButtonStyle.up = skin.newDrawable("white", Color.DARK_GRAY)
        textButtonStyle.down = skin.newDrawable("white", Color.DARK_GRAY)
        textButtonStyle.checked = skin.newDrawable("white", Color.BLUE)
        textButtonStyle.over = skin.newDrawable("white", Color.LIGHT_GRAY)

        textButtonStyle.font = skin.getFont("default")

        skin.add("default", textButtonStyle)

        val breaks = listOf(Bet.PASS, Bet.SIX_NO_TRUMP, Bet.SEVEN_NO_TRUMP,
                Bet.EIGHT_NO_TRUMP, Bet.MIZER, Bet.NINE_NO_TRUMP)
        val skips = listOf(Bet.UNKNOWN)
        for (value in Bet.values()) {
            if (value in skips) {
                continue
            }
            val button = TextButton(value.type, textButtonStyle)
            buttons[value] = button
            table.add(button).pad(10f)

            if (value in breaks) {
                table.row()
            }
        }

        stage.addActor(table)
    }

    /**
     * Display that this Bet is claimed by user
     */
    fun markBet(bet: Bet, vararg users: User) {
        val button = buttons[bet] ?: return
        button.isChecked = true
        button.setText(users.map { x -> x.name }.joinToString(" + \n"))
    }

    /**
     * Disable button corresponding to given Bet
     */
    fun disableBet(bet: Bet) {
        val button = buttons[bet] ?: return
        button.isDisabled = true
    }

    /**
     * Enable button corresponding to given Bet
     */

    fun enableBet(bet: Bet) {
        val button = buttons[bet] ?: return
        button.isDisabled = false
    }

    /**
     * Enable button corresponding to given Bet
     * and set button text to original value
     */

    fun resetBet(bet: Bet) {
        val button = buttons[bet] ?: return
        button.isChecked = false
        button.setText(bet.type)
        button.isDisabled = false
    }

    /**
     * Add callback Listener for this button
     */
    fun <R> addCallback(bet: Bet, callback: (Bet) -> (R)) {
        val button = buttons[bet] ?: return
        button.addListener(ListenerFactory.create(bet, button, callback))
    }


    fun render(cam: Camera) {
        if (isVisible) {
            stage.camera.projection.set(cam.projection)
            stage.draw()
        }
    }

    fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    companion object ListenerFactory {
        fun <R> create(bet: Bet,
                betButton: Button,
                callback: (Bet) -> (R)): EventListener {
            return object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    if (!betButton.isDisabled) {
                        callback(bet)
                        betButton.isChecked = true
                        betButton.isDisabled = true
                        println(bet.type)
                    }
                }
            }
        }
    }
}

class BiddingOverlayVisibilityAction(val overlay: BiddingOverlay, val show: Boolean, delay: Float = 0.10f) : Action(
        delay) {
    internal var finished = false
    override fun execute(delta: Float) {
        overlay.isVisible = show
        finished = true
    }

    override fun isComplete(): Boolean {
        return finished
    }
}


