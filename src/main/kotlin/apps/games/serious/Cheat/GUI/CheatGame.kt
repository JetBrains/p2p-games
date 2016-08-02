package apps.games.serious.Cheat.GUI

import apps.games.serious.Card
import apps.games.serious.Cheat.DeckSizes
import apps.games.serious.TableGUI.*
import apps.games.serious.getCardById
import apps.games.serious.getIdByCard
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g3d.ModelBatch
import java.util.concurrent.LinkedBlockingQueue


/**
 * Created by user on 6/30/16.
 */
class CheatGame(val me: Int, var DECK_SIZE: Int = 32, val N: Int) : GameView() {
    lateinit var font: BitmapFont
    lateinit var tableScreen: TableScreen
    lateinit private var deckSizeOverlay: ButtonOverlay<DeckSizes>


    var loaded = false
    override fun create() {
        batch = ModelBatch()
        font = BitmapFont()
        font.data.scale(2f)
        tableScreen = TableScreen(this, N)
        initButtonOverlays()
        setScreen(tableScreen)
        val s = "Switch 2D/3D:            C\n" +
                "Move:                         WASD \n" +
                "Strafe:                        Q, E \n" +
                "Select cardID:                left mouse\n" +
                "Play cardID:                   left mosue on selected cardID\n" +
                "Zoom camara:            midde mouse button\n"+
                "Toggle camera zoom: SPACE"
        tableScreen.controlsHint = s
        loaded = true
    }

    /**
     * Give a player with specified ID
     * a cardID from a 32 cardID deck
     */
    fun dealPlayer(player: Int, cardID: Int) {
        tableScreen.dealPlayer(player, getCardModelById(cardID))
    }

    fun dealPlayer(player: Int, card: Card){
        tableScreen.dealPlayer(player, tableScreen.deck.getCardModel(card.suit, card.pip))
    }

    /**
     * Deal a common cardID. For example
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
     * of UNKNOWN cardID.
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
        val res =  getIdByCard(card, DECK_SIZE)
        if(res == -1){
            return pickCard(*allowedCardIds)
        }
        return res
    }

    /**
     * Move cardID from hand of one player to the hand of another
     * @param cardID - cardID to move(any unknown cardID is picked, if cardID = -1)
     * @param from - id of player, to take cardID from
     * from = -1 - means common hand is used
     * @param to - id of player, to give cardID to
     * to = -1 - means common hand is used
     */
    fun giveCard(cardID: Int, from: Int, to: Int, flip: Boolean = true){
        val card = getCardModelById(cardID)
        val fromHand = tableScreen.getHandById(from) ?: return
        val toHand = tableScreen.getHandById(to) ?: return
        tableScreen.moveCard(card, fromHand, toHand, flip)
    }

    /**
     * animate cardID played by user
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
     * In Preferans we have 32 cardID deck.
     * This function takes cardID ID (0 -> 32)
     * or -1 for UNKNOWN cardID
     * and translates it into corresponding
     * renderable object
     */
    private fun getCardModelById(cardID: Int): CardGUI {
        val card = getCardById(cardID, DECK_SIZE)
        return tableScreen.deck.getCardModel(card.suit, card.pip)
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

    /**
     * Add callback listener for buttons
     * corresponding to pick of Deck Size
     */
    fun <R> registerDeckSizeCallback(callBack: (DeckSizes) -> (R), vararg sizes: DeckSizes) {
        for (size in sizes) {
            deckSizeOverlay.addCallback(size, callBack)
        }
    }

    fun updateDeckSize(newDeckSize: Int){
        DECK_SIZE  = newDeckSize
        tableScreen.updateDeckSize(DECK_SIZE)
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
    val gameGUI = CheatGame(1, N=3)
    LwjglApplication(gameGUI, config)
    Thread.sleep(2000)
    println("6 \u2660")
}