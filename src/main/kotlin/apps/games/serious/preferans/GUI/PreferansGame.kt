package apps.games.serious.preferans.GUI

import apps.games.serious.preferans.Bet
import apps.games.serious.preferans.Pip
import apps.games.serious.preferans.Suit
import com.badlogic.gdx.Game
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g3d.ModelBatch
import entity.User

/**
 * Created by user on 6/30/16.
 */
class preferansGame : Game() {
    lateinit var batch: ModelBatch
    lateinit var font: BitmapFont
    lateinit var tableScreen: TableScreen
    var loaded = false
    override fun create() {
        batch = ModelBatch()
        font = BitmapFont()
        font.data.scale(2f)
        tableScreen = TableScreen(this)
        setScreen(tableScreen)
        loaded = true
    }

    /**
     * Give a player with specified ID
     * a card from a 32 card deck
     */
    fun dealPlayer(player: Int, cardID: Int) {
        tableScreen.dealPlayer(player, getCardById(cardID))
    }

    /**
     * Deal a common card. For example
     * TALON in Prefenernce or cards
     * in Texas Holdem Poker
     */
    fun dealCommon(cardID: Int) {
        tableScreen.dealCommon(getCardById(cardID))
    }

    /**
     * Show bidding overlay after all other actions are complete
     */
    fun showBiddingOverlay() {
        tableScreen.actionManager.addAfterLastComplete(
                BiddingOverlayVisibilityAction(tableScreen.biddingOverlay,
                        true))
    }

    /**
     * Show bidding overlay after all other actions are complete
     */
    fun hideBiddingOverlay() {
        tableScreen.actionManager.addAfterLastComplete(
                BiddingOverlayVisibilityAction(tableScreen.biddingOverlay,
                        false))
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
            tableScreen.biddingOverlay.markBet(bet,
                    *(betMap[bet]!!.toTypedArray()))
        }
    }

    /**
     * Mark all bet buttons as enabled
     */
    fun resetAllBets() {
        for (bet in Bet.values()) {
            tableScreen.biddingOverlay.resetBet(bet)
        }
    }

    /**
     * Mark all bet buttons as disabled
     */
    fun disableAllBets() {
        for (bet in Bet.values()) {
            tableScreen.biddingOverlay.disableBet(bet)
        }
    }

    /**
     * Reset bets from given list of bets
     * @param bets - Bets to enable
     */
    fun resetBets(vararg bets: Bet) {
        for (bet in bets) {
            tableScreen.biddingOverlay.resetBet(bet)
        }
    }

    /**
     * Enable bets from given list of bets
     * @param bets - Bets to enable
     */
    fun enableBets(vararg bets: Bet) {
        for (bet in bets) {
            tableScreen.biddingOverlay.enableBet(bet)
        }
    }


    /**
     * Disable bets from given list of bets
     * @param bets - Bets to disable
     */
    fun disableBets(vararg bets: Bet) {
        for (bet in bets) {
            tableScreen.biddingOverlay.disableBet(bet)
        }
    }

    /**
     * Add callback listener for buttons
     * corresponding to provided bets
     */
    fun <R> registerCallback(callBack: (Bet) -> (R), vararg bets: Bet) {
        for (bet in bets) {
            tableScreen.biddingOverlay.addCallback(bet, callBack)
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
        tableScreen.revealCommonCard(getCardById(cardID))
    }

    /**
     * In preferans we have 32 card deck.
     * This function takes card ID (0 -> 32)
     * or -1 for UNKNOWN card
     * and translates it into corresponding
     * renderable object
     */
    private fun getCardById(cardID: Int): Card {
        val card: Card
        if (cardID == -1) {
            card = tableScreen.deck.getCard(Suit.UNKNOWN, Pip.UNKNOWN)
        } else {
            val suitId = cardID / 8
            var pipId: Int = (cardID % 8)
            if (Pip.TWO.index <= pipId) {
                pipId += 5
            }
            println(pipId)
            card = tableScreen.deck.getCard(
                    Suit.values().first { x -> x.index == suitId },
                    Pip.values().first { x -> x.index == pipId })
        }
        return card
    }

    override fun render() {
        super.render()
    }


    override fun dispose() {
        batch.dispose()
        font.dispose()
    }
}

fun main(args: Array<String>) {
    val config = LwjglApplicationConfiguration()
    config.width = 1024
    config.height = 1024
    config.forceExit = false
    val gameGUI = preferansGame()
    LwjglApplication(gameGUI, config)
    println("6 \u2660")
}