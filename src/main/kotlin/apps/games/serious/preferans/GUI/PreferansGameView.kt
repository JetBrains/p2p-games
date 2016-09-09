package apps.games.serious.preferans.gui

import Settings
import apps.games.serious.Card
import apps.games.serious.getCardById
import apps.games.serious.getIdByCard
import apps.games.serious.preferans.Bet
import apps.games.serious.preferans.PreferansScoreCounter
import apps.games.serious.preferans.Whists
import apps.table.gui.*
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g3d.ModelBatch
import entity.User
import java.util.concurrent.LinkedBlockingQueue

/**
 * Created by user on 6/30/16.
 */
class PreferansGameView(val scoreCounter: PreferansScoreCounter, val me: Int, val deckSize: Int = 32) : GameView() {
    lateinit var font: BitmapFont
    lateinit var tableScreen: TableScreen
    lateinit var biddingOverlay: ButtonOverlay<Bet>
    lateinit var whistingOverlay: ButtonOverlay<Whists>
    lateinit var scoreOverlay: ScoreOverlay

    var loaded = false
    override fun create() {
        batch = ModelBatch()
        font = BitmapFont()
        font.data.scale(2f)
        tableScreen = TableScreen(this)
        initButtonOverlays()
        initScoreOverlay(scoreCounter, me)
        setScreen(tableScreen)
        val s = "Switch 2D/3D:            C\n" +
                "Move:                         WASD \n" +
                "Strafe:                        Q, E \n" +
                "Select cardID:                left mouse\n" +
                "Play cardID:                   left mosue on selected cardID\n" +
                "Zoom camara:            midde mouse button\n" +
                "Toggle camera zoom: SPACE"
        tableScreen.controlsHint = s
        loaded = true
    }

    /**
     * Give a playerId with specified ID
     * a cardID from a 32 cardID deck
     */
    fun dealPlayer(player: Int, cardID: Int) {
        tableScreen.dealPlayer(player, getCardModelById(cardID))
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
            val s = betMap[bet]!!.map { x -> x.name }.joinToString(" + \n")
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
            val s = whistMap[whist]!!.map { x -> x.name }.joinToString(" + \n")
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
    fun <R> registerWhistingCallback(callBack: (Whists) -> (R),
                                     vararg whists: Whists) {
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
     * Reveal talon cardID
     */
    fun revealTalonCard(cardID: Int) {
        tableScreen.revealCommonCard(getCardModelById(cardID))
    }

    /**
     * Get one of specified cards.
     * NB: This method wont work for selection
     * of UNKNOWN cardID.
     */
    fun pickCard(vararg allowedCardIds: Card): Int {
        val queue = LinkedBlockingQueue<CardGUI>(1)
        val allowedCards = allowedCardIds.map { x ->
            tableScreen.deck
                    .getCardModel(x)
        }
        tableScreen.setSelector(object : CardSelector {
            override fun select(card: CardGUI) {
                queue.add(card)
            }

            override fun canSelect(card: CardGUI): Boolean {
                return allowedCards.contains(card)
            }
        })

        val card = queue.take()
        tableScreen.resetSelector()
        val res = getIdByCard(card, deckSize)
        if (res == -1) {
            return pickCard(*allowedCardIds)
        }
        return res
    }

    /**
     * Move cardID from hand of one playerId to the hand of another
     * @param cardID - cardID to move(any unknown cardID is picked, if cardID = -1)
     * @param from - id of playerId, to take cardID from
     * from = -1 - means common hand is used
     * @param to - id of playerId, to give cardID to
     * to = -1 - means common hand is used
     */
    fun giveCard(cardID: Int, from: Int, to: Int, flip: Boolean = true) {
        val card = getCardModelById(cardID)
        val fromHand = tableScreen.getHandById(from) ?: return
        val toHand = tableScreen.getHandById(to) ?: return
        tableScreen.moveCard(card, fromHand, toHand, flip)
    }

    /**
     * animate cardID played by user
     */
    fun playCard(cardID: Int) {
        val card = getCardModelById(cardID)
        tableScreen.animateCardPlay(card)
    }

    fun revealPlayerCard(player: Int, cardID: Int) {
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
        val card = getCardById(cardID, deckSize)
        return tableScreen.deck.getCardModel(card.suit, card.pip)
    }

    /**
     * update name of player with given ID
     *
     * @param player - table id of name to register
     * @param name - new name
     */
    fun updatePlayerName(player: Int, name: String) {
        tableScreen.updatePlayerName(player, name)
    }

    /**
     * init overlays, that require pressing button onscreen
     */
    private fun initButtonOverlays() {
        val betBreaks = listOf(Bet.PASS, Bet.SIX_NO_TRUMP, Bet.SEVEN_NO_TRUMP,
                Bet.EIGHT_NO_TRUMP, Bet.MIZER, Bet.NINE_NO_TRUMP)
        val betSkips = listOf(Bet.UNKNOWN)
        val betNames = Bet.values().associate { x -> Pair(x, x.type) }
        biddingOverlay = ButtonOverlayFactory.create(Bet::class.java, betBreaks,
                betSkips, betNames)
        biddingOverlay.create()

        val whistBreaks = listOf(Whists.WHIST_HALF)
        val whistIgnore = listOf(Whists.UNKNOWN)
        val whistNames = Whists.values().associate { x -> Pair(x, x.type) }
        whistingOverlay = ButtonOverlayFactory.create(Whists::class.java,
                whistBreaks, whistIgnore, whistNames)
        whistingOverlay.create()
    }

    fun initScoreOverlay(scoreCounter: PreferansScoreCounter, playerID: Int) {
        scoreOverlay = ScoreOverlay(scoreCounter, playerID)
        tableScreen.addOverlay(scoreOverlay)
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
    val gameGUI = PreferansGameView(PreferansScoreCounter(listOf(User(Settings.hostAddress, "sfsefse"))), 1)
    LwjglApplication(gameGUI, config)
    Thread.sleep(2000)
    println("6 \u2660")
}