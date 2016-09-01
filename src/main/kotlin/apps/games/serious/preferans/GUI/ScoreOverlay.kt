package apps.games.serious.preferans.GUI

import apps.games.serious.TableGUI.Overlay
import apps.games.serious.preferans.PreferansScoreCounter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin

/**
 * Created by user on 7/26/16.
 */

class ScoreOverlay(val scoreCounter: PreferansScoreCounter, val me: Int) : Overlay() {
    lateinit var skin: Skin
    lateinit var table: com.badlogic.gdx.scenes.scene2d.ui.Table
    val bfont = BitmapFont()
    val texture = Texture(Gdx.files.internal("preferans/scoreboard.jpg"))
    val scoreField = Image(texture)
    //normal labels
    private val normalLabels = mutableListOf<Label>()
    private val rotatedLabels = mutableListOf<Label>()


    init {

        table = com.badlogic.gdx.scenes.scene2d.ui.Table()

        table.setPosition(150f * scaleX, 225f * scaleY)
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
        scoreField.scaleBy(0.6f)
        initLabels()
        stage.addListener(object : InputListener() {
            override fun keyDown(event: InputEvent?, keycode: Int): Boolean {
                if (keycode == Input.Keys.TAB) {
                    isVisible = true
                    return true
                }
                return false
            }

            override fun keyUp(event: InputEvent?, keycode: Int): Boolean {
                if (keycode == Input.Keys.TAB) {
                    isVisible = false
                    return true
                }
                return false
            }
        })
    }


    //names
    lateinit var myName: Label
    lateinit var leftName: Label
    lateinit var topName: Label

    //heaps
    lateinit var myHeap: Label
    lateinit var leftHeap: Label
    lateinit var topHeap: Label

    //bullet
    lateinit var myBullet: Label
    lateinit var leftBullet: Label
    lateinit var topBullet: Label

    //whists
    lateinit var myLeftWhist: Label
    lateinit var myTopWhist: Label
    lateinit var leftMeWhist: Label
    lateinit var leftTopWhist: Label
    lateinit var topLeftWhist: Label
    lateinit var topMeWhist: Label

    fun initLabels() {
        //names
        myName = Label("", skin)
        myName.setPosition(610f * scaleX, 500f * scaleY)
        normalLabels.add(myName)

        leftName = Label("", skin)
        leftName.setPosition(-545 * scaleY, 370 * scaleX)
        rotatedLabels.add(leftName)

        topName = Label("", skin)
        topName.setPosition(610f * scaleX, 530f * scaleY)
        normalLabels.add(topName)

        //heaps
        myHeap = Label("", skin)
        myHeap.setPosition(610f * scaleX, 432f * scaleY)
        normalLabels.add(myHeap)

        leftHeap = Label("", skin)
        leftHeap.setPosition(-525 * scaleY, 350 * scaleX)
        rotatedLabels.add(leftHeap)

        topHeap = Label("", skin)
        topHeap.setPosition(610f * scaleX, 620f * scaleY)
        normalLabels.add(topHeap)

        //bullets
        myBullet = Label("", skin)
        myBullet.setPosition(610f * scaleX, 395f * scaleY)
        normalLabels.add(myBullet)

        leftBullet = Label("", skin)
        leftBullet.setPosition(-525 * scaleY, 315 * scaleX)
        rotatedLabels.add(leftBullet)

        topBullet = Label("", skin)
        topBullet.setPosition(610f * scaleX, 655f * scaleY)
        normalLabels.add(topBullet)

        //whists
        myLeftWhist = Label("", skin)
        myLeftWhist.setPosition(420f * scaleX, 300f * scaleY)
        normalLabels.add(myLeftWhist)

        myTopWhist = Label("", skin)
        myTopWhist.setPosition(800f * scaleX, 300f * scaleY)
        normalLabels.add(myTopWhist)

        //
        leftTopWhist = Label("", skin)
        leftTopWhist.setPosition(-620 * scaleY, 230 * scaleX)
        rotatedLabels.add(leftTopWhist)

        leftMeWhist = Label("", skin)
        leftMeWhist.setPosition(-420 * scaleY, 230 * scaleX)
        rotatedLabels.add(leftMeWhist)
        //
        topMeWhist = Label("", skin)
        topMeWhist.setPosition(800f * scaleX, 750f * scaleY)
        normalLabels.add(topMeWhist)

        topLeftWhist = Label("", skin)
        topLeftWhist.setPosition(420f * scaleX, 750f * scaleY)
        normalLabels.add(topLeftWhist)
    }

    fun updateLabels() {
        val leftId = (me + 1) % scoreCounter.users.size
        val topId = (me + 2) % scoreCounter.users.size
        val myUser = scoreCounter.users[me]
        val leftUser = scoreCounter.users[leftId]
        val topUser = scoreCounter.users[topId]
        // Names
        myName.setText(myUser.name)
        leftName.setText(leftUser.name)
        topName.setText(topUser.name)


        //Heaps
        myHeap.setText(scoreCounter.heap[myUser].toString())
        leftHeap.setText(scoreCounter.heap[leftUser].toString())
        topHeap.setText(scoreCounter.heap[topUser].toString())

        //bullets
        myBullet.setText(scoreCounter.bullet[myUser].toString())
        leftBullet.setText(scoreCounter.bullet[leftUser].toString())
        topBullet.setText(scoreCounter.bullet[topUser].toString())

        //whists
        myLeftWhist.setText(scoreCounter.whists[myUser to leftUser].toString())
        myTopWhist.setText(scoreCounter.whists[myUser to topUser].toString())
        leftTopWhist.setText(scoreCounter.whists[leftUser to topUser].toString())
        leftMeWhist.setText(scoreCounter.whists[leftUser to myUser].toString())
        topMeWhist.setText(scoreCounter.whists[topUser to myUser].toString())
        topLeftWhist.setText(scoreCounter.whists[topUser to leftUser].toString())
    }

    fun drawLabels(batch: Batch) {
        updateLabels()
        batch.begin()
        normalLabels.forEach { x -> x.draw(batch, 1f) }
        batch.end()

        batch.transformMatrix.rotate(0f, 0f, 1f, -90f)
        batch.begin()
        rotatedLabels.forEach { x -> x.draw(batch, 1f) }
        batch.end()
        batch.transformMatrix.rotate(0f, 0f, 1f, 90f)


    }

    override fun render(cam: Camera) {
        if (isVisible) {
            stage.camera.projection.set(cam.projection)
            stage.batch.begin()
            stage.batch.draw(texture, 150f * scaleX, 225f * scaleY, 800f * scaleX, 600f * scaleY)
            //Draw playerId names
            stage.batch.end()
            drawLabels(stage.batch)

            stage.draw()
        }
    }
}