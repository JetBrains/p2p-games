package apps.games.serious.Cheat.GUI

import apps.games.serious.TableGUI.*
import apps.games.serious.getCardById32
import apps.games.serious.getId32ByCard
import apps.games.serious.preferans.*
import apps.games.serious.preferans.GUI.ScoreOverlay
import com.badlogic.gdx.Game
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g3d.ModelBatch
import entity.User
import java.util.concurrent.LinkedBlockingQueue


/**
 * Created by user on 6/30/16.
 */
class CheatGame(val me: Int) : GameView() {
    lateinit var font: BitmapFont
    lateinit var tableScreen: TableScreen
    lateinit private var deckSizeOverlay: ButtonOverlay<DeckSizes>


    var loaded = false
    override fun create() {
        batch = ModelBatch()
        font = BitmapFont()
        font.data.scale(2f)
        tableScreen = TableScreen(this)
        initButtonOverlays()
        setScreen(tableScreen)
        val s = "Switch 2D/3D:            C\n" +
                "Move:                         WASD \n" +
                "Strafe:                        Q, E \n" +
                "Select card:                left mouse\n" +
                "Play card:                   left mosue on selected card\n" +
                "Zoom camara:            midde mouse button\n"+
                "Toggle camera zoom: SPACE"
        tableScreen.controlsHint = s
        loaded = true
    }

    /**
     * Give a player with specified ID
     * a card from a 32 card deck
     */
    fun dealPlayer(player: Int, cardID: Int) {
        tableScreen.dealPlayer(player, getCardModelById(cardID))
    }

    /**
     * Deal a common card. For example
     * TALON in Prefenernce or cards
     * in Texas Holdem Poker
     */
    fun dealCommon(cardID: Int) {
        tableScreen.dealCommon(getCardModelById(cardID))
    }



    /**
     * Display hint for current step
     */
    fun showHint(hint: String) {
        synchronized(tableScreen.hint) {
            tableScreen.hint = hint
        }
    }

    /**
     * Get one of specified cards.
     * NB: This method wont work for selection
     * of UNKNOWN card.
     */
    fun pickCard(vararg allowedCardIds: Card): Int{
        val queue = LinkedBlockingQueue<CardGUI>(1)
        val allowedCards = allowedCardIds.map { x -> tableScreen.deck
                .getCardModel(x) }
        tableScreen.setSelector(object : CardSelector {
            override fun select(card: CardGUI){
                queue.add(card)
            }

            override fun canSelect(card: CardGUI): Boolean {
                return allowedCards.contains(card)
            }
        })

        val card = queue.take()
        tableScreen.resetSelector()
        val res =  getId32ByCard(card)
        if(res == -1){
            return pickCard(*allowedCardIds)
        }
        return res
    }

    /**
     * Move card from hand of one player to the hand of another
     * @param cardID - card to move(any unknown card is picked, if cardID = -1)
     * @param from - id of player, to take card from
     * from = -1 - means common hand is used
     * @param to - id of player, to give card to
     * to = -1 - means common hand is used
     */
    fun giveCard(cardID: Int, from: Int, to: Int, flip: Boolean = true){
        val card = getCardModelById(cardID)
        val fromHand = tableScreen.getHandById(from) ?: return
        val toHand = tableScreen.getHandById(to) ?: return
        tableScreen.moveCard(card, fromHand, toHand, flip)
    }

    /**
     * animate card played by user
     */
    fun playCard(cardID: Int){
        val card = getCardModelById(cardID)
        tableScreen.animateCardPlay(card)
    }

    fun revealPlayerCard(player: Int, cardID: Int){
        val card = getCardModelById(cardID)
        tableScreen.revealPlayerCard(player, card)
    }


    /**
     * In Preferans we have 32 card deck.
     * This function takes card ID (0 -> 32)
     * or -1 for UNKNOWN card
     * and translates it into corresponding
     * renderable object
     */
    private fun getCardModelById(cardID: Int): CardGUI {
        val card = getCardById32(cardID)
        return tableScreen.deck.getCardModel(card.suit, card.pip)
    }


    private enum class DeckSizes(val type: String,val size: Int){
        SMALL("36 CARD", 36),
        LARGE("52 CARD", 52)
    }

    /**
     * Show overlay, that allows players to pick deck size for game
     */
    private fun initButtonOverlays(){
        val sizesNames = DeckSizes.values().associate { x -> Pair(x, x.type) }
        deckSizeOverlay = ButtonOverlayFactory.create(DeckSizes::class.java, names = sizesNames)
        deckSizeOverlay.create()
    }

    /**
     * Show bidding overlay after all other actions are complete
     */
    fun showDecksizeOverlay() {
        tableScreen.addOverlay(deckSizeOverlay)
        tableScreen.actionManager.addAfterLastComplete(
                OverlayVisibilityAction(deckSizeOverlay, true))
    }

    /**
     * Hide bidding overlay after all other actions are complete
     */
    fun hideDecksizeOverlay() {
        tableScreen.removeOverlay(deckSizeOverlay)
        tableScreen.actionManager.addAfterLastComplete(
                OverlayVisibilityAction(deckSizeOverlay, false))
    }


    override fun render() {
        super.render()
    }


    override fun dispose() {
        batch.dispose()
        font.dispose()
    }

    fun clear() {
        tableScreen.clear()
    }
}

fun main(args: Array<String>) {
    val config = LwjglApplicationConfiguration()
    config.width = 1024
    config.height = 1024
    config.forceExit = false
    val gameGUI = CheatGame(1)
    LwjglApplication(gameGUI, config)
    Thread.sleep(2000)
    println("6 \u2660")
}