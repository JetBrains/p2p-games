package GUI.libgdx.CardGame

import com.badlogic.gdx.*
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Intersector

/**
 * Created by user on 7/1/16.
 */


class CardGameScreen(val game: CardGame): InputAdapter(), Screen {
    private val atlas = TextureAtlas(Gdx.files.internal("cards/carddeck.atlas"))

    private val cam3d = PerspectiveCamera()
    private val camController = FirstPersonCameraController(cam3d)
    private val cam2d = OrthographicCamera()
    private var is2dMode: Boolean = true

    private val table: Table = Table(3, 10)

    private val environment = Environment()

    private var selecting: Card? = null

    val deck = CardDeck(atlas, 2)
    val cards: CardBatch
    var actions = CardActions()

    init{
        //Init cards
        val material = Material(
                TextureAttribute.createDiffuse(atlas.textures.first()),
                BlendingAttribute(false, 1f),
                FloatAttribute.createAlphaTest(0.5f))
        val selectionMaterial = Material(BlendingAttribute(0.4f))
        selectionMaterial.set(ColorAttribute.createDiffuse(Color.ORANGE))

        cards = CardBatch(material, selectionMaterial)

        //Init selectionMaterial
        selectionMaterial.set(ColorAttribute.createDiffuse(Color.ORANGE))


        //init deck placement
        val card = deck.getCard(Suit.UNKNOWN, Pip.UNKNOWN)
        card.position.set(table.deckPosition)
        card.angle = 180f
        card.update()
        cards.add(card)

        //Init Envirenment
        environment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f))
        environment.add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -.4f, -.4f, -.4f))

        //Init Camera
        camController.setVelocity(10f)
        Gdx.input.inputProcessor = InputMultiplexer(this, camController)

    }


    private var spawnTimer = -1f
    private var playerId = 0
    private var toSpawn: Int = table.players[playerId].hand.MAX_HAND_SIZE

    /**
     * Render everything. Also in charge of spawning cards on timer
     */
    override fun render(delta: Float) {
        val delta = Math.min(1 / 30f, Gdx.graphics.deltaTime)

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        camController.update()
        if(toSpawn > 0){
            if (spawnTimer < 0) {
                if (Gdx.input.justTouched())
                    spawnTimer = 1f
            } else{
                spawnTimer -= delta
                if(spawnTimer < 0){
                    spawnTimer = 0.25f
                    giveCard(table.players[playerId])
                    toSpawn --
                }
            }

        }else{
            playerId ++
            if(playerId < table.players.size){
                toSpawn = table.players[playerId].hand.MAX_HAND_SIZE
            }
        }


        actions.update(delta)

        game.batch.begin(getCam())
        game.batch.render(cards, environment)
        game.batch.render(table.tableTop, environment)
        game.batch.end()

    }




    override fun resize(width: Int, height: Int) {
        cam3d.viewportWidth = width.toFloat()
        cam3d.viewportHeight = height.toFloat()
        cam3d.position.set(0f, -12f, 12f) //experimental constants
        cam3d.lookAt(0f, -8f, 0f)
        cam2d.position.set(0f, 0f, 11f)
        cam2d.lookAt(0f, 0f, 0f)
        cam2d.viewportWidth = 20f
        cam2d.viewportHeight = 20f
        cam3d.lookAt(0f, 0f, 0f)
        cam3d.update()
        cam2d.update()
    }


    //TODO REMOVE - testing purposes only
    var pipIdx = 0
    var suitIdx = 1
    var spawnZ = 0f

    /**
     * Spawn a card from the deck and give it to player
     */
    fun giveCard(player: Player) {
        //sample code
        if (++pipIdx >= Pip.values().size) {
            pipIdx = 0
            suitIdx = (suitIdx + 1) % Suit.values().size
        }
        val suit = Suit.values()[suitIdx]
        val pip = Pip.values()[pipIdx]
        Gdx.app.log("Spawn", suit.type + " - " + pip)

        //spawn card from deck
        val card = deck.getCard(suit, pip)
        card.position.set(table.deckPosition)
        card.angle = 180f
        cards.add(card)
        spawnZ += 0.01f
        if(spawnZ*100 > player.hand.MAX_HAND_SIZE){
            spawnZ = 0.01f
        }
        val handPos = player.hand.nextCardPosition()
        val angle = player.getAngle()
        actions.animate(card, handPos.x, handPos.y, handPos.z + spawnZ, 0f, 1f, angle)
        player.hand.size ++
    }


    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (selecting != null) {
            if(selecting == getObject(screenX, screenY)){
                setSelected(selecting!!)
            }else{
                setSelecting(getObject(screenX, screenY))
            }
            return true
        }else{
            setSelecting(getObject(screenX, screenY))
            return selecting != null
        }
    }

    /**
     * Second click on card
     */
    private fun setSelected(selecting: Card) {
        val pos = table.getMainPlayer().getCardspace()
        actions.animate(selecting, pos.x, pos.y, 0.01f, 180f, 1f, 0f)
    }

    /**
     * First click on card
     */
    private fun setSelecting(newSelecting: Card?){
        if(newSelecting == null){
            return
        }

        if(selecting != null){
            (selecting as Card).isSelected = false
        }
        newSelecting.isSelected = true
        selecting = newSelecting
    }

    /**
     * Get current camera - 2d or 3d view
     */
    private fun getCam(): Camera{
        if(is2dMode){
            return cam2d
        }else{
            return cam3d
        }
    }

    /**
     * Simple implementation to pick pointed card
     */
    fun getObject(screenX: Int, screenY: Int): Card? {
        val ray = getCam().getPickRay(screenX.toFloat(), screenY.toFloat())

        var result: Card? = null
        var distance = -1f

        for (instance in cards) {
             val dist2 = ray.origin.dst2(instance.position)
            if (distance >= 0f && dist2 > distance)
                continue

            if (Intersector.intersectRaySphere(ray, instance.position, instance.radius, null)) {
                result = instance
                distance = dist2
            }
        }

        return result
    }

    override fun keyUp(keycode: Int): Boolean {
        if(keycode == Input.Keys.C){
            is2dMode = !is2dMode
            return true
        }
        return false
    }

    override fun show() { }

    override fun pause() { }

    override fun hide() { }

    override fun resume() { }

    override fun dispose() {
        atlas.dispose()
        cards.dispose()
    }


}