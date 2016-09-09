package apps.games.serious.mafia.gui

import apps.table.gui.Overlay
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextArea
import com.badlogic.gdx.scenes.scene2d.ui.TextField

/**
 * Created by user on 8/30/16.
 */

class MafiaInputOverlay(maxLength: Int) : Overlay() {
    lateinit var skin: Skin
    lateinit var table: com.badlogic.gdx.scenes.scene2d.ui.Table

    val texture = Texture(Gdx.files.internal("mafia/note.png"))
    val background = Image(texture)
    //normal labels
    private val inputField: TextArea
    private var shiftPressed: Boolean = false
    private var callback: (String) -> (Unit) = { x -> Unit }

    init {

        val generator = FreeTypeFontGenerator(Gdx.files.internal("fonts/typewriter.ttf"))

        val parameter = FreeTypeFontGenerator.FreeTypeFontParameter()
        parameter.size = 25

        val bfont = generator.generateFont(parameter) // font size 12 pixels
        generator.dispose()

        table = com.badlogic.gdx.scenes.scene2d.ui.Table()

        table.setPosition(500f * scaleX, 500f * scaleY)
        // A skin can be loaded via JSON or defined programmatically, either is fine. Using a skin is optional but strongly
        // recommended solely for the convenience of getting a texture, region, etc as a drawable, tinted drawable, etc.
        skin = Skin()
        // Generate a 1x1 white texture and store it in the skin named "white".
        val pixmap = Pixmap(80, 80, Pixmap.Format.RGBA8888)
        pixmap.setColor(Color.GREEN)
        pixmap.fill()

        skin.add("white", Texture(pixmap))

        // Store the default libgdx font under the name "default".
        bfont.color = Color.BLACK
        //bfont.data.scale(0.2f)
        skin.add("default", bfont)
        val style = TextField.TextFieldStyle()
        style.font = bfont
        style.fontColor = Color.BLACK
        skin.add("default", style)
        background.scaleBy(0.6f)
        background.height = 300f * scaleY
        background.width = 200f * scaleX
        inputField = TextArea("TestTEST", skin)
        inputField.setPrefRows(7f)
        inputField.maxLength = maxLength
        table.add(inputField).width(300f * scaleX)
        stage.addActor(table)
        inputField.addListener(object : InputListener() {
            override fun keyUp(event: InputEvent?, keycode: Int): Boolean {
                if (keycode == Input.Keys.SHIFT_RIGHT || keycode == Input.Keys.SHIFT_LEFT) {
                    shiftPressed = false
                    return true
                }
                return false
            }

            override fun keyDown(event: InputEvent?, keycode: Int): Boolean {
                when (keycode) {
                    Input.Keys.SHIFT_LEFT -> {
                        shiftPressed = true
                        return true
                    }
                    Input.Keys.SHIFT_RIGHT -> {
                        shiftPressed = true
                        return true
                    }
                    Input.Keys.ENTER -> {
                        if (shiftPressed) {
                            finishedTyping(inputField.text)
                        }
                        return true
                    }
                    else -> {
                        return false
                    }
                }
            }
        })
    }

    /**
     * what to to, when end of message input is tiggered
     *
     * @param s - message to send
     */
    private fun finishedTyping(s: String) {
        callback(s)
    }

    /**
     * set focus on input
     */
    fun newMessage() {
        inputField.text = ""
        stage.keyboardFocus = inputField
    }

    /**
     * register callback to be executed after end of input
     */
    fun <T> registerCallback(callback: (String) -> (T)) {
        this.callback = { s: String ->
            if (isVisible) {
                callback(s)
            }
        }
    }

    override fun render(cam: Camera) {
        if (isVisible) {
            stage.camera.projection.set(cam.projection)
            stage.batch.begin()
            stage.batch.draw(texture, 280f * scaleX, 325f * scaleY, 450f * scaleX, 320f * scaleY)
            table.draw(stage.batch, 1f)

            stage.batch.end()
            stage.draw()
        }
    }
}