package apps.games.serious.preference.GUI

import apps.games.serious.preference.Bet
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.scenes.scene2d.*
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener

/**
 * Created by user on 7/7/16.
 */


class BiddingOverlay {
    lateinit var skin: Skin
    lateinit var stage: Stage
    lateinit var batch: SpriteBatch
    lateinit var table: com.badlogic.gdx.scenes.scene2d.ui.Table
    private val buttons = mutableListOf<Button>()

    fun create() {
        batch = SpriteBatch()
        stage = Stage()
        table = com.badlogic.gdx.scenes.scene2d.ui.Table()

        table.setPosition(500f, 500f)
        // A skin can be loaded via JSON or defined programmatically, either is fine. Using a skin is optional but strongly
        // recommended solely for the convenience of getting a texture, region, etc as a drawable, tinted drawable, etc.
        skin = Skin()
        // Generate a 1x1 white texture and store it in the skin named "white".
        val pixmap = Pixmap(100, 100, Pixmap.Format.RGBA8888)
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

        val breaks = listOf(Bet.PASS, Bet.SIX_NO_TRUMP, Bet.SEVEN_NO_TRUMP, Bet.EIGHT_NO_TRUMP, Bet.MIZER, Bet.NINE_NO_TRUMP)
        for(value in Bet.values()){
            val button = TextButton(value.type, textButtonStyle)
            button.addListener(ListenerFactory.create(value, {-> buttons.forEach { x -> x.isDisabled = true }}))
            buttons.add(button)
            table.add(button).pad(15f)

            if(value in breaks){
                table.row()
            }
        }

        stage.addActor(table)
    }


    fun render(cam: Camera){
        stage.camera.projection.set(cam.projection)
        stage.draw()
    }

    fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    companion object ListenerFactory{
        fun create(bet: Bet,callback: () -> Unit): EventListener{
            return object: ChangeListener(){
                override fun changed(event: ChangeEvent?, actor: Actor?) {
                    callback()
                    println(bet.type)
                }
            }
        }
    }
}
