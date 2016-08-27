package apps.games.serious.TableGUI

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener

/**
 * Created by user on 7/14/16.
 */


abstract class Overlay {
    var isVisible = false
    val stage: Stage

    init {
        stage = Stage()
    }

    private val baseX = 1024f
    private val baseY = 1024f

    val scaleX = Gdx.graphics.width / baseX
    val scaleY = Gdx.graphics.height / baseY
    open fun render(cam: Camera) {
        if (isVisible) {
            stage.camera.projection.set(cam.projection)
            stage.draw()
        }
    }

    fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }
}

abstract class ButtonOverlay<T> : Overlay() {
    lateinit var skin: Skin

    lateinit var table: com.badlogic.gdx.scenes.scene2d.ui.Table
    val buttons = mutableMapOf<T, TextButton>()
    val textButtonStyle: TextButton.TextButtonStyle

    init {


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
        val bfont = BitmapFont()
        skin.add("default", bfont)

        // Configure a TextButtonStyle and name it "default". Skin resources are stored by type, so this doesn't overwrite the font.
        textButtonStyle = TextButton.TextButtonStyle()
        textButtonStyle.up = skin.newDrawable("white", Color.DARK_GRAY)
        textButtonStyle.down = skin.newDrawable("white", Color.DARK_GRAY)
        textButtonStyle.checked = skin.newDrawable("white", Color.BLUE)
        textButtonStyle.over = skin.newDrawable("white", Color.LIGHT_GRAY)

        textButtonStyle.font = skin.getFont("default")

        skin.add("default", textButtonStyle)
        stage.addActor(table)
    }

    /**
     * Create layout for bidding overlay
     */
    abstract fun create()


    /**
     * Display that this Option is already chosen, and add text to it
     */
    fun markOption(option: T, message: String) {
        val button = buttons[option] ?: return
        button.isChecked = true
        button.setText(message)
    }

    /**
     * Disable button corresponding to given option
     */
    fun disableOption(option: T) {
        val button = buttons[option] ?: return
        button.isDisabled = true
    }

    /**
     * Enable button corresponding to given Option
     */

    fun enableOption(option: T) {
        val button = buttons[option] ?: return
        button.isDisabled = false
    }

    /**
     * Enable button corresponding to given Option
     * and set button text to original value
     */
    open fun resetOption(option: T) {
        val button = buttons[option] ?: return
        button.isChecked = false
        button.setText(option.toString())
        button.isDisabled = false
    }

    /**
     * Add callback Listener for this button
     */
    fun <R> addCallback(option: T, callback: (T) -> (R)) {
        val button = buttons[option] ?: return
        button.clearListeners()
        button.addListener(ListenerFactory.create(option, button, { x: T ->
            if (isVisible) {
                callback(x)
            }
        }))

    }


    companion object ListenerFactory {
        fun <R, T> create(option: T,
                                    betButton: Button,
                                    callback: (T) -> (R)): EventListener {
            return object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    if (!betButton.isDisabled) {
                        callback(option)
                        betButton.isChecked = true
                        betButton.isDisabled = true
                    }
                }
            }
        }
    }
}

class ButtonOverlayFactory {
    companion object {
        fun <T : Enum<T>> create(clazz: Class<T>,
                                 breaks: Collection<T> = listOf(),
                                 skips: Collection<T> = listOf(),
                                 names: Map<T, String> = mapOf()): ButtonOverlay<T> {
            return object : ButtonOverlay<T>() {
                override fun create() {
                    for (value in clazz.enumConstants) {
                        if (value in skips) {
                            continue
                        }
                        val name = names[value] ?: value.name
                        val button = TextButton(name, textButtonStyle)
                        buttons[value] = button
                        table.add(button).pad(10f)

                        if (value in breaks) {
                            table.row()
                        }
                    }
                }

                override fun resetOption(option: T) {
                    val button = buttons[option] ?: return
                    button.isChecked = false
                    val name = names[option] ?: option.name
                    button.setText(name)
                    button.isDisabled = false

                }
            }
        }

        fun <T> create(values: Collection<T>,
                       breaks: Collection<T>,
                       names: Map<T, String>): ButtonOverlay<T> {
            return object : ButtonOverlay<T>() {
                override fun create() {
                    for(value in values){
                        val name = names[value]
                        val button = TextButton(name, textButtonStyle)
                        buttons[value] = button
                        table.add(button).pad(10f)
                        if (value in breaks) {
                            table.row()
                        }
                    }
                }
            }
        }
    }
}

class OverlayVisibilityAction(val overlay: ButtonOverlay<*>, val show: Boolean,
                              delay: Float = 0.10f) : Action(delay) {
    internal var finished = false
    override fun execute(delta: Float) {
        overlay.isVisible = show
        overlay.resize(Gdx.graphics.width, Gdx.graphics.height)
        finished = true
    }

    override fun isComplete(): Boolean {
        return finished
    }
}