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
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Intersector

/**
 * Created by user on 7/1/16.
 */


class CardGameScreen(val game: CardGame): InputAdapter(), Screen {
    private val atlas = TextureAtlas(Gdx.files.internal("cards/carddeck.atlas"))

    //TODO - mb perspective camera
    private val cam = PerspectiveCamera()
    private val camController = FirstPersonCameraController(cam)
    private val tableTopModel: Model
    private val tableTop: ModelInstance
    private val environment = Environment()

    private var selected = -1
    private var selecting = -1
    private val selectionMaterial: Material = Material()
    private val originalMaterial: Material = Material()

    val deck = CardDeck(atlas, 2)
    val cards: CardBatch
    var actions = CardActions()

    val MINIMUM_VIEWPORT_SIZE = 5f

    init{
        //Init cards
        val material = Material(
                TextureAttribute.createDiffuse(atlas.textures.first()),
                BlendingAttribute(false, 1f),
                FloatAttribute.createAlphaTest(0.5f))
        cards = CardBatch(material)

        //Init selectionMaterial
        selectionMaterial.set(ColorAttribute.createDiffuse(Color.ORANGE))


        val card = deck.getCard(Suit.SPADES, Pip.KING)
        card.position.set(3.5f, -2.5f, 0.01f)
        card.angle = 180f
        card.update()
        cards.add(card)

        //Init table
        val builder = ModelBuilder()
        builder.begin()
        builder.node().id = "top"
        builder.part("top", GL20.GL_TRIANGLES, (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(),
                    Material(ColorAttribute.createDiffuse(Color(0x63750A)))).box(0f, 0f, -0.5f, 20f, 20f, 1f)
        tableTopModel = builder.end()
        tableTop = ModelInstance(tableTopModel)

        //Init Envirenment
        environment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f))
        environment.add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -.4f, -.4f, -.4f))

        //Init Camera
        Gdx.input.inputProcessor = InputMultiplexer(this, camController)

    }


    private var spawnTimer = -1f
    private var toSpawn: Int = 6

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
                    spawn()
                    toSpawn --
                }
            }

        }


        actions.update(delta)

        game.batch.begin(cam)
        game.batch.render(cards, environment)
        game.batch.render(tableTop, environment)
        game.batch.end()

    }




    override fun resize(width: Int, height: Int) {
        cam.viewportWidth = width.toFloat()
        cam.viewportHeight = height.toFloat()
        cam.position.set(0f, -13f, 13f) //experimental constants
        cam.lookAt(0f, 0f, 0f)
        cam.update()
        cam.update()
    }


    //TODO REMOVE - testing purposes only
    var pipIdx = -1
    var suitIdx = 0
    var spawnX = -0.5f
    var spawnY = 0f
    var spawnZ = 0f
    fun spawn() {
        if (++pipIdx >= Pip.values().size) {
            pipIdx = 0
            suitIdx = (suitIdx + 1) % Suit.values().size
        }
        val suit = Suit.values()[suitIdx]
        val pip = Pip.values()[pipIdx]
        Gdx.app.log("Spawn", suit.type + " - " + pip)
        val card = deck.getCard(suit, pip)
        card.position.set(3.5f, -2.5f, 0.01f)
        card.angle = 180f
        if (!cards.contains(card))
            cards.add(card)
        spawnX += 0.5f
        if (spawnX > 6f) {
            spawnX = 0f
            spawnY = (spawnY + 0.5f) % 2f
        }
        spawnZ += 0.01f
        actions.animate(card, -3.5f + spawnX, 2.5f - spawnY, 0.01f + spawnZ, 0f, 1f)
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        selecting = getObject(screenX, screenY)
        return selecting >= 0
    }

    // TODO
    // 1) Game object system instead of Cards
    // 2) Move cards on drag & Drop
    // 3) Table model
    // 4) Give cards to players
    fun getObject(screenX: Int, screenY: Int): Int {
        val ray = cam.getPickRay(screenX.toFloat(), screenY.toFloat())

        var result: Card?
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

        return 42
    }

    override fun show() { }

    override fun pause() { }

    override fun hide() { }

    override fun resume() { }

    override fun dispose() {
        atlas.dispose()
        cards.dispose()
        tableTopModel.dispose()
    }


}