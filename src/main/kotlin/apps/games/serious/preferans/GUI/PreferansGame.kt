package apps.games.serious.preferans.GUI

import apps.games.serious.preferans.*
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
class PreferansGame : Game() {
    lateinit var batch: ModelBatch
    lateinit var font: BitmapFont
    lateinit var tableScreen: TableScreen
    lateinit var biddingOverlay: Overlay<Bet>
    lateinit var whistingOverlay: Overlay<Whists>

    var loaded = false
    override fun create() {
        batch = ModelBatch()
        font = BitmapFont()
        font.data.scale(2f)
        tableScreen = TableScreen(this)
        initOverlays()
        setScreen(tableScreen)
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
     * Show bidding overlay after all other actions are complete
     */
    fun showBiddingOverlay() {
        tableScreen.addOverlay(biddingOverlay)
        tableScreen.actionManager.addAfterLastComplete(
                OverlayVisibilityAction(biddingOverlay, true))
    }

    /**
     * Hide bidding overlay after all other actions are complete
     */
    fun hideBiddingOverlay() {
        tableScreen.removeOverlay(biddingOverlay)
        tableScreen.actionManager.addAfterLastComplete(
                OverlayVisibilityAction(biddingOverlay, false))
    }

    /**
     * Show whisting overlay after all other actions are complete
     */
    fun showWhistingOverlay() {
        tableScreen.addOverlay(whistingOverlay)
        tableScreen.actionManager.addAfterLastComplete(
                OverlayVisibilityAction(whistingOverlay, true))
    }

    /**
     * Hide whisting overlay after all other actions are complete
     */
    fun hideWhistingOverlay() {
        tableScreen.removeOverlay(whistingOverlay)
        tableScreen.actionManager.addAfterLastComplete(
                OverlayVisibilityAction(whistingOverlay, false))
    }

    /**
     * display user bets - mark them on bidding overlay
     * @param bets -vararg Pair<User, Bet> - pairs
     * describing User's Bet
     */
    fun markBets(vararg bets: Pair<User, Bet>) {
        val betMap = mutableMapOf<Bet, MutableSet<User>>()
        for (bet in bets) {
            if (betMap[bet.second] == null) {
                betMap[bet.second] = mutableSetOf()
            }
            betMap[bet.second]?.add(bet.first)
        }
        for (bet in betMap.keys) {
            val s = betMap[bet]!!.map { x -> x.name}.joinToString(" + \n")
            biddingOverlay.markOption(bet, s)
        }
    }

    /**
     * display user whists - mark them on bidding overlay
     * @param whists -vararg Pair<User, Whist> - pairs
     * describing User's whists
     */
    fun markWhists(vararg whists: Pair<User, Whists>) {
        val whistMap = mutableMapOf<Whists, MutableSet<User>>()
        for (whist in whists) {
            if (whistMap[whist.second] == null) {
                whistMap[whist.second] = mutableSetOf()
            }
            whistMap[whist.second]?.add(whist.first)
        }
        for (whist in whistMap.keys) {
            val s = whistMap[whist]!!.map { x -> x.name}.joinToString(" + \n")
            whistingOverlay.markOption(whist, s)
        }
    }



    /**
     * Mark all bet buttons as enabled
     */
    fun resetAllBets() {
        for (bet in Bet.values()) {
            biddingOverlay.resetOption(bet)
        }
    }

    /**
     * Mark all bet buttons as disabled
     */
    fun disableAllBets() {
        for (bet in Bet.values()) {
            biddingOverlay.disableOption(bet)
        }
    }

    /**
     * Mark all whist buttons as enabled
     */
    fun resetAllWhists() {
        for (whist in Whists.values()) {
            whistingOverlay.resetOption(whist)
        }
    }

    /**
     * Mark all whist buttons as disabled
     */
    fun disableAllWhists() {
        for (whist in Whists.values()) {
            whistingOverlay.disableOption(whist)
        }
    }

    /**
     * Reset bets from given list of bets
     * @param bets - Bets to enable
     */
    fun resetBets(vararg bets: Bet) {
        for (bet in bets) {
            biddingOverlay.resetOption(bet)
        }
    }

    /**
     * Enable bets from given list of bets
     * @param bets - Bets to enable
     */
    fun enableBets(vararg bets: Bet) {
        for (bet in bets) {
            biddingOverlay.enableOption(bet)
        }
    }


    /**
     * Disable bets from given list of bets
     * @param bets - Bets to disable
     */
    fun disableBets(vararg bets: Bet) {
        for (bet in bets) {
            biddingOverlay.disableOption(bet)
        }
    }

    /**
     * Enable whists from given list of whists
     * @param whists - Whists to enable
     */
    fun enableWhists(vararg whists: Whists) {
        for (whist in whists) {
            whistingOverlay.enableOption(whist)
        }
    }


    /**
     * Disable whists from given list of whists
     * @param whists - Whists to disable
     */
    fun disableWhists(vararg whists: Whists) {
        for (whist in whists) {
            whistingOverlay.disableOption(whist)
        }
    }

    /**
     * Add callback listener for buttons
     * corresponding to provided bets
     */
    fun <R> registerBiddingCallback(callBack: (Bet) -> (R), vararg bets: Bet) {
        for (bet in bets) {
            biddingOverlay.addCallback(bet, callBack)
        }
    }

    /**
     * Add callback listener for buttons
     * corresponding to whisting
     */
    fun <R> registerWhistingCallback(callBack: (Whists) -> (R), vararg whists: Whists) {
        for (whist in whists) {
            whistingOverlay.addCallback(whist, callBack)
        }
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
     * Reveal talon card
     */
    fun revealTalonCard(cardID: Int) {
        tableScreen.revealCommonCard(getCardModelById(cardID))
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
        tableScreen.setSelector(object : CardSelector{
            override fun select(card: CardGUI){
                queue.add(card)
            }

            override fun canSelect(card: CardGUI): Boolean {
                return allowedCards.contains(card)
            }
        })

        val card = queue.take()
        tableScreen.resetSelector()
        val res =  getIdByCard(card)
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


    /**
     * In Preferans we have 32 card deck.
     * This function takes card
     * and translates it into corresponding
     * CardGUI Id (-1 -> 32). -1 - for UNKNOWN
     */
    private fun getIdByCard(card: CardGUI): Int {
        if(card.suit == Suit.UNKNOWN){
            return -1
        }
        val suitID: Int = card.suit.index
        var pipID: Int = card.pip.index
        if(pipID >= 6){
            pipID -= 5
        }
        return 8*suitID + pipID
    }

    private fun initOverlays(){
        val betBreaks  = listOf(Bet.PASS, Bet.SIX_NO_TRUMP, Bet.SEVEN_NO_TRUMP,
                                Bet.EIGHT_NO_TRUMP, Bet.MIZER, Bet.NINE_NO_TRUMP)
        val betSkips = listOf(Bet.UNKNOWN)
        val betNames = Bet.values().associate { x -> Pair(x, x.type) }
        biddingOverlay = OverlayFactory.create(Bet::class.java, betBreaks,
                                               betSkips, betNames)
        biddingOverlay.create()

        val whistBreaks = listOf(Whists.WHIST_HALF)
        val whistIgnore = listOf(Whists.UNKNOWN)
        val whistNames = Whists.values().associate { x -> Pair(x, x.type) }
        whistingOverlay = OverlayFactory.create(Whists::class.java,
                                                whistBreaks, whistIgnore, whistNames)
        whistingOverlay.create()

    }



    override fun render() {
        super.render()
    }


    override fun dispose() {
        batch.dispose()
        font.dispose()
    }

    fun clear() {
        tableScreen.cards.clear()
    }
}

fun main(args: Array<String>) {
    val config = LwjglApplicationConfiguration()
    config.width = 1024
    config.height = 1024
    config.forceExit = false
    val gameGUI = PreferansGame()
    LwjglApplication(gameGUI, config)
    println("6 \u2660")
}