package apps.games.serious.mafia.GUI

import apps.games.serious.TableGUI.Overlay
import apps.games.serious.mafia.MafiaLogger
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.Align

/**
 * Created by user on 8/27/16.
 */

class LogOverlay(val logger: MafiaLogger) : Overlay() {
    lateinit var skin: Skin
    lateinit var table: com.badlogic.gdx.scenes.scene2d.ui.Table
    val bfont = BitmapFont()
    val texture = Texture(Gdx.files.internal("mafia/log.png"))
    val background = Image(texture)
    //normal labels
    private val dayLabel: Label
    private val nightLabel: Label
    private var currentDayOffset: Int = 0


    init {

        val generator = FreeTypeFontGenerator(Gdx.files.internal("fonts/hand.ttf"))

        val parameter = FreeTypeFontGenerator.FreeTypeFontParameter()
        parameter.size = 20
        //TODO - remove caps in logger

        val bfont = generator.generateFont(parameter) // font size 12 pixels
        generator.dispose()

        table = com.badlogic.gdx.scenes.scene2d.ui.Table()

        table.setPosition(550f * scaleX, 460f * scaleY, Align.topLeft)
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

        skin.add("default", bfont)
        val style = Label.LabelStyle(bfont, Color.BLACK)
        skin.add("default", style)
        background.scaleBy(0.6f)
        dayLabel = Label("", skin)
        nightLabel = Label("", skin)
        dayLabel.setWrap(true)
        nightLabel.setWrap(true)
        dayLabel.setAlignment(Align.topLeft)
        nightLabel.setAlignment(Align.topLeft)
        table.add(dayLabel).width(300f * scaleX).height(600 * scaleY).padRight(60 * scaleX).align(Align.top)
        table.add(nightLabel).width(300f * scaleX).height(600 * scaleY).align(Align.top)
        stage.addListener(object : InputListener() {
            override fun keyUp(event: InputEvent?, keycode: Int): Boolean {
                when (keycode) {
                    Input.Keys.TAB -> {
                        isVisible = !isVisible
                        return true
                    }
                    Input.Keys.LEFT -> {
                        currentDayOffset++
                        return true
                    }
                    Input.Keys.RIGHT -> {
                        currentDayOffset--
                        return true
                    }
                    else -> return false
                }
            }
        })
    }

    fun updateLabels() {
        dayLabel.setText(logger.getDayLog(currentDayOffset))
        nightLabel.setText(logger.getNightLog(currentDayOffset))
    }


    override fun render(cam: Camera) {
        if (isVisible) {
            stage.camera.projection.set(cam.projection)
            stage.batch.begin()
            stage.batch.draw(texture, 150f * scaleX, 225f * scaleY, 800f * scaleX, 600f * scaleY)
            updateLabels()
            table.draw(stage.batch, 1f)
            stage.batch.end()
            stage.draw()
        }
    }
}
