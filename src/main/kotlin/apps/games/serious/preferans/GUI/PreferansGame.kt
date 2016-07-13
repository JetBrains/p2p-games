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
import java.util.concurrent.LinkedBlockingQueue

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
     * Get one of specified cards.
     * NB: Thhis method wont work for selection
     * of UNKNOWN card.
     */
    fun pickCard(vararg allowedCardIds: Int): Int?{
        if(allowedCardIds.contains(-1)){
            return null
        }
        val queue = LinkedBlockingQueue<Card>(1)
        val allowedCards = allowedCardIds.map { x -> getCardById(x) }
        tableScreen.setSelector(object : CardSelector{
            override fun select(card: Card){
                queue.add(card)
            }

            override fun canSelect(card: Card): Boolean {
                return allowedCards.contains(card)
            }
        })

        val card = queue.take()
        tableScreen.resetSelector()
        return getIdByCard(card)
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
        val card = getCardById(cardID)
        val fromHand = tableScreen.getHandById(from) ?: return
        val toHand = tableScreen.getHandById(to) ?: return
        tableScreen.moveCard(card, fromHand, toHand, flip)
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

    /**
     * In preferans we have 32 card deck.
     * This function takes card
     * and translates it into corresponding
     * Card Id (-1 -> 32). -1 - for UNKNOWN
     */
    private fun getIdByCard(card: Card): Int {
        if(card.suit == Suit.UNKNOWN){
            return -1
        }
        val suitID: Int = card.suit.index
        var pipID: Int = card.pip.index
        if(pipID >= 7){
            pipID -= 6
        }
        return 8*suitID + pipID
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