package apps.games.serious.preferans.GUI

import apps.games.serious.preferans.Pip

import apps.games.serious.preferans.Suit
import com.badlogic.gdx.*
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Vector3
/**
 * Created by user on 7/1/16.
 */


class TableScreen(val game: PreferansGame) : InputAdapter(), Screen {
    companion object DefaultSelector: CardSelector

    private val atlas = TextureAtlas(Gdx.files.internal("cards/carddeck.atlas"))
    private val cam3d = PerspectiveCamera()
    private val camController = FirstPersonCameraController(cam3d)
    private val cam2d = OrthographicCamera()
    private var zoomed = false
    private val cam2dZoom = OrthographicCamera()
    private val zoomAspectRatio = 5f/3f

    private var is2dMode: Boolean = true

    private val table: Table = Table(3, 10)

    private val environment = Environment()

    private var selecting: CardGUI? = null
    val deck = CardDeck(atlas, 2)
    val cards: CardBatch
    var actionManager = ActionManager()
    val topDeck: CardGUI
    var showDeck = true
    val overlays = mutableSetOf<Overlay<*>>()
    lateinit var spriteBatch: SpriteBatch
    lateinit var font: BitmapFont

    private var selector: CardSelector = DefaultSelector

    var hint: String = ""

    private val DEAL_SPEED = 1f

    init {
        spriteBatch = SpriteBatch()
        font = BitmapFont()
        font.color = Color.RED

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
        topDeck = deck.getCardModel(Suit.UNKNOWN, Pip.UNKNOWN)
        topDeck.position.set(table.deckPosition)
        topDeck.angle = 180f
        topDeck.update()
        cards.add(topDeck)

        //Init Envirenment
        environment.set(
                ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f,
                        1f))
        environment.add(
                DirectionalLight().set(0.8f, 0.8f, 0.8f, -.4f, -.4f, -.4f))


        //Init Camera
        camController.setVelocity(10f)
        updateInputProcessor()

    }


    /**
     * Render everything. Also in charge of spawning cards on timer
     */
    override fun render(delta: Float) {
        val delta = Math.min(1 / 30f, Gdx.graphics.deltaTime)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        Gdx.gl20.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        camController.update()

        actionManager.update(delta)

        game.batch.begin(getCam())
        game.batch.render(cards, environment)
        game.batch.render(table.tableTop, environment)
        game.batch.end()

        spriteBatch.begin()
        font.draw(spriteBatch, hint, 100f, 100f)
        spriteBatch.end()
        for(overlay in overlays){
            overlay.render(getCam())
        }
        if(zoomed){
            //TODO - no overlapping
            val height = Gdx.graphics.height/4
            Gdx.gl20.glViewport(0, Gdx.graphics.height - height,
                                (height*zoomAspectRatio).toInt(),height)
            game.batch.begin(cam2dZoom)
            game.batch.render(cards, environment)
            game.batch.render(table.tableTop, environment)
            game.batch.end()
        }

    }

    /**
     * Deal specified card to a specified player
     * @param player - receives the card
     * @param card - card to give
     */
    @Synchronized fun dealPlayer(player: Player, card: CardGUI) {
        card.position.set(table.deckPosition)
        val handPos = player.hand.nextCardPosition()
        val angle = player.getAngle()
        player.hand.cards.add(card)
        card.angle = 180f
        cards.add(card)

        Gdx.app.log("Spawn", card.suit.type + " - " + card.pip)


        actionManager.addAfterLastReady(
                CardGUI.animate(card, handPos.x, handPos.y, handPos.z, 0f,
                                DEAL_SPEED, angle))
    }

    /**
     * Deal specified card to a player with a give playerId
     * @param player - id of the player that receives the card
     * @param card - card to give
     */
    @Synchronized fun dealPlayer(player: Int, card: CardGUI) {
        dealPlayer(table.players[player], card)
    }


    /**
     * Deal a card common to all players(e.g.
     * TALON in Preferans, or cards in texas holdem poker)
     */
    @Synchronized fun dealCommon(card: CardGUI) {
        card.position.set(table.deckPosition)
        val handPos = table.commonHand.nextCardPosition()
        val angle = table.getMainPlayer().getAngle()
        table.commonHand.cards.add(card)
        card.angle = 180f
        cards.add(card)
        actionManager.addAfterLastReady(
                CardGUI.animate(card, handPos.x, handPos.y, handPos.z, 180f,
                                DEAL_SPEED, angle))
    }

    //TODO - mark cards dealt to self as revealed\

    /**
     * Reveal card in common hand
     */
    @Synchronized fun revealCommonCard(card: CardGUI) {
        val revealCallback = {
            val oldCard = table.commonHand.replaceUnknownCard(card)
            if (oldCard != null) {
                //TODO - flawless transition
                card.angle = 180f
                val action = CardGUI.animate(card, card.position.x,
                                             card.position.y, card.position.z, card.angle + 180f, 1f)
                actionManager.add(action)
                cards.remove(oldCard)
                //spawn card facing table
                cards.add(card)
            }
        }
        actionManager.addAfterLastComplete(DelayedAction(0.15f, revealCallback))
    }

    /**
     * reveal playerID card and execute actions after it
     */
    @Synchronized fun revealPlayerCard(playerID: Int, card: CardGUI){
        val hand = table.players[playerID].hand
        val oldCard = hand.replaceUnknownCard(card)
        if (oldCard != null) {
            //TODO - flawless transition
            card.angle = 180f
            val action = CardGUI.animate(card, card.position.x,
                                         card.position.y, card.position.z,
                                         card.angle + 180f, 1f, card.rotation)
            actionManager.add(action)

            cards.remove(oldCard)
            //spawn card facing table
            cards.add(card)
        }



    }

    /**
     * Remove all cards, that are not in player hands from table
     */
    @Synchronized fun clearTable(){
        val action = DelayedAction(0.15f, {
            val it = cards.iterator()
            while(it.hasNext){
                val next = it.next()
                if(table.getPlayerWithCard(next) == null){
                    it.remove()
                }
            }
        })
        actionManager.addAfterLastComplete(action)
    }
    /**
     * Return hand with given ID.
     * @param handID - id of hand to find
     * handID = -1 - means common hand
     */
    fun getHandById(handID: Int): Hand?{
        if(handID == -1){
            return table.commonHand
        }
        if(handID >= 0 && handID < table.playersCount){
            return table.players[handID].hand
        }
        return null
    }

    /**
     * Move card from one hand to another
     */
    @Synchronized fun moveCard(card: CardGUI, from: Hand, to: Hand, flip:
                               Boolean = false){
        from.cards.remove(card)
        val pos = to.nextCardPosition()
        val toAngle: Float = card.angle + (if (flip) 180f else 0f)
        val action = CardGUI.animate(card, pos.x, pos.y, pos.z, toAngle, 1f,
                                     to.getAngle() - from.getAngle())
        actionManager.addAfterLastComplete(action)
        to.cards.add(card)
    }

    /**
     * Hide deck from the table
     */
    @Synchronized fun hideDeck() {
        if (showDeck) {
            val pos = Vector3(topDeck.position).add(0f, 0f, -1f)
            actionManager.addAfterLastComplete(
                    CardGUI.animate(topDeck, pos.x, pos.y, pos.z, 0f, 1f, 0f))
            showDeck = true
        }

    }

    /**
     * Show deck on the table
     */
    @Synchronized fun showDeck() {
        if (!showDeck) {
            val pos = Vector3(topDeck.position).add(0f, 0f, 1f)
            actionManager.addAfterLastComplete(
                    CardGUI.animate(topDeck, pos.x, pos.y, pos.z, 0f, 1f, 0f))
            showDeck = false
        }

    }

    /**
     * Add overlay
     */
    @Synchronized fun addOverlay(overlay: Overlay<*>){
        overlays.add(overlay)
        updateInputProcessor()
    }

    /**
     * Remove overlay
     */
    @Synchronized fun removeOverlay(overlay: Overlay<*>){
        overlays.remove(overlay)
        updateInputProcessor()
    }

    override fun resize(width: Int, height: Int) {
        for(overlay in overlays){
            overlay.resize(width, height)
        }
        cam3d.viewportWidth = width.toFloat()
        cam3d.viewportHeight = height.toFloat()
        cam3d.position.set(0f, -12f, 12f) //experimental constants
        cam3d.lookAt(0f, -8f, 0f)
        cam2d.position.set(0f, 0f, 11f)
        cam2d.lookAt(0f, 0f, 0f)
        cam2d.viewportWidth = 20f
        cam2d.viewportHeight = 20f
        cam3d.update()
        cam2d.update()

        cam2dZoom.position.set(0f, 0f, 5f)
        cam2dZoom.lookAt(0f, 0f, 1f)
        cam2dZoom.viewportWidth = 10f*zoomAspectRatio
        cam2dZoom.viewportHeight = 10f
        cam2dZoom.zoom = 0.25f

    }

    /**
     * animate card played for holders hand
     * @param User - holder of ther card
     */
    private var deltaZ = 0.01f
    fun animateCardPlay(card: CardGUI){
        val player = table.getPlayerWithCard(card) ?: return
        player.hand.cards.remove(card)
        val pos = player.getCardspace()

        actionManager.addAfterLastComplete(
                CardGUI.animate(card, pos.x, pos.y, deltaZ, card.angle, 1f,
                                card.rotation, doNotRotate = true))
        deltaZ += 0.001f
    }

    override fun touchUp(screenX: Int,
            screenY: Int,
            pointer: Int,
            button: Int): Boolean {
        if(button == Input.Buttons.MIDDLE){
            zoomed = false
            return true
        }
        if (selecting != null) {
            if (selecting == getObject(screenX, screenY)) {
                setSelected(selecting!!)
            } else {
                setSelecting(getObject(screenX, screenY))
            }

        } else {
            setSelecting(getObject(screenX, screenY))
        }
        return selecting != null
    }

    override fun touchDragged(screenX: Int,
                                       screenY: Int,
                                       pointer: Int): Boolean {
            if(zoomed){
                val pos = cam2d.unproject(Vector3(screenX.toFloat(), screenY
                        .toFloat(), 0f))
                cam2dZoom.position.set(pos)
                cam2dZoom.lookAt(pos.x, pos.y, 0f)
                cam2dZoom.update()
                return true
            }
            return false
    }

    override fun touchDown(screenX: Int,
                           screenY: Int,
                           pointer: Int,
                           button: Int): Boolean {
        if(button == Input.Buttons.MIDDLE){
            val pos = cam2d.unproject(Vector3(screenX.toFloat(), screenY
                    .toFloat(), 0f))
            cam2dZoom.position.set(pos)
            cam2dZoom.lookAt(pos.x, pos.y, 0f)
            cam2dZoom.update()
            zoomed = true
            return true
        }
        return false
    }

    /**
     * Get current camera - 2d or 3d view
     */
    private fun getCam(): Camera {
        if (is2dMode) {
            return cam2d
        } else {
            return cam3d
        }
    }

    /**
     * Second click on card
     */
    private fun setSelected(selecting: CardGUI) {
        if(!selector.canSelect(selecting)){
            return
        }
        selector.select(selecting)
        val player = table.getPlayerWithCard(selecting) ?: return
        player.hand.removeCard(selecting, actionManager)
        val pos = player.getCardspace()
        actionManager.add(CardGUI.animate(selecting, pos.x, pos.y, deltaZ, 0f,
                                          1f, selecting.rotation))
        deltaZ += 0.001f
    }

    /**
     * First click on card
     */
    private fun setSelecting(newSelecting: CardGUI?) {
        if (selecting != null) {
            (selecting as CardGUI).isSelected = false
        }

        selecting = newSelecting
        if (newSelecting != null) {
            newSelecting.isSelected = true
        }


    }

    /**
     * Simple implementation to pick pointed card
     */
    fun getObject(screenX: Int, screenY: Int): CardGUI? {
        val ray = getCam().getPickRay(screenX.toFloat(), screenY.toFloat())

        var result: CardGUI? = null
        var distance = -1f

        for (instance in cards) {
            val dist2 = ray.origin.dst2(instance.position)
            if (distance >= 0f && dist2 > distance)
                continue

            if (Intersector.intersectRaySphere(ray, instance.position,
                    instance.radius, null)) {
                result = instance
                distance = dist2
            }
        }

        return result
    }

    fun updateInputProcessor(){
        val overlayProcessor = InputMultiplexer(*overlays.map { x -> x.stage }
                .toTypedArray())
        Gdx.input.inputProcessor = InputMultiplexer(this, overlayProcessor,
                                                    camController)
    }

    @Synchronized fun setSelector(cardSelector: CardSelector){
        selector = cardSelector
    }

    @Synchronized fun resetSelector(){
        selector = DefaultSelector
    }

    override fun keyUp(keycode: Int): Boolean {
        if (keycode == Input.Keys.C) {
            is2dMode = !is2dMode
            return true
        }
        return false
    }

    override fun keyDown(keycode: Int): Boolean {
        return super.keyDown(keycode)
    }

    override fun show() {
    }

    override fun pause() {
    }

    override fun hide() {
    }

    override fun resume() {
    }

    override fun dispose() {
        atlas.dispose()
        cards.dispose()
    }

    fun clear(){
        cards.clear()
        table.clear()
        deck.reset()
    }
}
