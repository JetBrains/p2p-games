package apps.games.serious.Cheat.GUI

import apps.games.serious.*
import apps.games.serious.Cheat.BetCount
import apps.games.serious.Cheat.Choice
import apps.games.serious.Cheat.DeckSizes
import apps.games.serious.TableGUI.*
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g3d.ModelBatch
import java.util.concurrent.LinkedBlockingQueue


/**
 * Created by user on 6/30/16.
 */
class CheatGame(val me: Int, var deckSize: Int = 32, val N: Int) : GameView() {
    lateinit var font: BitmapFont
    lateinit var tableScreen: TableScreen
    lateinit private var deckSizeOverlay: ButtonOverlay<DeckSizes>
    lateinit private var choiceOverlay: ButtonOverlay<Choice>
    lateinit private var numberOverlay: ButtonOverlay<BetCount>
    lateinit private var pipOverlay: ButtonOverlay<Pip>
    private var deckSizeChanged: Boolean = false


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
                "Zoom camara:            midde mouse button\n" +
                "Toggle camera zoom: SPACE"
        tableScreen.controlsHint = s
        loaded = true
    }

    /**
     * Give a player with specified ID
     * a card with cardID from the deck
     */
    fun dealPlayer(player: Int, cardID: Int) {
        tableScreen.dealPlayer(player, getCardModelById(cardID))
    }

    /**
     * Give a player with specified ID
     * a card from the deck
     */
    fun dealPlayer(player: Int, card: Card) {
        tableScreen.dealPlayer(player, tableScreen.deck.getCardModel(card.suit, card.pip))
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
    fun pickCard(vararg allowedCardIds: Card): Int {
        var res: Int = -1
        while (res == -1) {
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
            res = getIdByCard(card, deckSize)
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

    /**
     * Animate card movement from players hand
     */
    fun animateCardPlay(player: Int) {
        tableScreen.animateUnknownCardPlay(player)
    }

    /**
     * Pick a card from players cardspace and
     * return it's relative position
     */
    fun pickPlayedCard(player: Int,
                       restrinction: (Int) -> (Boolean) = { x -> true }): Int {
        var res: Int = -1
        while (res == -1) {
            val queue = LinkedBlockingQueue<Int>(1)
            tableScreen.setSelector(object : CardSelector {
                override fun select(card: CardGUI) {
                    queue.add(tableScreen.getPositionInCardSpaceHand(player, card))
                }

                override fun canSelect(card: CardGUI): Boolean {
                    val f = tableScreen.playerCardSpaceSelectionFunction(player)(card)
                    return f && restrinction(tableScreen.getPositionInCardSpaceHand(player, card))
                }
            })

            res = queue.take()
            tableScreen.resetSelector()

        }

        return res
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
     * Init overlays for
     * 1)Game pips
     * 2)Number of cards played coinces
     * 3)pip pips
     * 4)Deck size pips
     */
    private fun initButtonOverlays() {
        val choiceNames = Choice.values().associate { x -> Pair(x, x.type) }
        choiceOverlay = ButtonOverlayFactory.create(Choice::class.java, names = choiceNames)
        choiceOverlay.create()


        val numberNames = BetCount.values().associate { x -> Pair(x, x.type) }
        val numberBreaks = listOf(BetCount.TWO)
        numberOverlay = ButtonOverlayFactory.create(BetCount::class.java,
                breaks = numberBreaks,
                names = numberNames)
        numberOverlay.create()

        initPipOverlay()


        val sizesNames = DeckSizes.values().associate { x -> Pair(x, x.type) }
        deckSizeOverlay = ButtonOverlayFactory.create(DeckSizes::class.java, names = sizesNames)
        deckSizeOverlay.create()
    }

    /**
     * Init pip overlay based on current deck size
     */
    private fun initPipOverlay() {
        val allowedPips = getPipsInDeck(deckSize)
        val pipNames = allowedPips.associate { x -> Pair(x, x.type) }
        val pipSkips = Pip.values().toMutableList()
        pipSkips.removeAll(allowedPips)
        val pipBreaks = mutableListOf<Pip>()
        val BUTTONS_IN_ROW = 4
        for (i in (BUTTONS_IN_ROW - 1)..(allowedPips.size - 1) step BUTTONS_IN_ROW) {
            pipBreaks.add(allowedPips[i])
        }
        try {
            pipOverlay = ButtonOverlayFactory.create(Pip::class.java,
                    breaks = pipBreaks,
                    skips = pipSkips,
                    names = pipNames)
        } catch (e: Exception) {
            println(e)
        }
        pipOverlay.create()
    }

    /**
     * Disable Choices from given list of bets
     * @param choices - Choices to disable
     */
    fun disableChoices(vararg choices: Choice) {
        for (choice in choices) {
            choiceOverlay.disableOption(choice)
        }
    }

    /**
     * Disable BetCounts from given list of bets
     * @param betCounts - BetCounts to disable
     */
    fun disableBetCounts(vararg betCounts: BetCount) {
        for (betCount in betCounts) {
            numberOverlay.disableOption(betCount)
        }
    }

    /**
     * Show DeckSize overlay after all other actions are complete
     */
    fun showDecksizeOverlay() {
        tableScreen.addOverlay(deckSizeOverlay)
        tableScreen.actionManager.addAfterLastComplete(
                OverlayVisibilityAction(deckSizeOverlay, true))
    }

    /**
     * Hide DeckSize overlay after all other actions are complete
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
    fun <R> registerDeckSizeCallback(callBack: (DeckSizes) -> (R),
                                     vararg sizes: DeckSizes) {
        for (size in sizes) {
            deckSizeOverlay.addCallback(size, callBack)
        }
    }

    /**
     * Add callback listener for buttons
     * corresponding to pick of Choice
     */
    fun <R> registerChoicesCallback(callBack: (Choice) -> (R),
                                    vararg choices: Choice) {
        for (choice in choices) {
            choiceOverlay.addCallback(choice, callBack)
        }
    }

    /**
     * Add callback listener for buttons
     * corresponding to pick of BetCount
     */
    fun <R> registerBetCountsCallback(callBack: (BetCount) -> (R),
                                      vararg betCounts: BetCount) {
        for (betCount in betCounts) {
            numberOverlay.addCallback(betCount, callBack)
        }
    }

    /**
     * Add callback listener for buttons
     * corresponding to pick of BetCount
     */
    fun <R> registerPipCallback(callBack: (Pip) -> (R), vararg pips: Pip) {
        for (pip in pips) {
            pipOverlay.addCallback(pip, callBack)
        }
    }


    /**
     * Show Choices overlay after all other actions are complete
     */
    fun showChoicesOverlay() {
        tableScreen.addOverlay(choiceOverlay)
        tableScreen.actionManager.addAfterLastComplete(
                OverlayVisibilityAction(choiceOverlay, true))
    }

    /**
     * Hide Choices overlay after all other actions are complete
     */
    fun hideChoicesOverlay() {
        tableScreen.removeOverlay(choiceOverlay)
        tableScreen.actionManager.addAfterLastComplete(
                OverlayVisibilityAction(choiceOverlay, false))
    }

    /**
     * Show Bet Count overlay after all other actions are complete
     */
    fun showBetCountOverlay() {
        tableScreen.addOverlay(numberOverlay)
        tableScreen.actionManager.addAfterLastComplete(
                OverlayVisibilityAction(numberOverlay, true))
    }

    /**
     * Hide Bet Count overlay after all other actions are complete
     */
    fun hideBetCountOverlay() {
        tableScreen.removeOverlay(numberOverlay)
        tableScreen.actionManager.addAfterLastComplete(
                OverlayVisibilityAction(numberOverlay, false))
    }

    /**
     * Show Pip overlay after all other actions are complete
     */
    fun showPipOverlay() {
        tableScreen.addOverlay(pipOverlay)
        tableScreen.actionManager.addAfterLastComplete(
                OverlayVisibilityAction(pipOverlay, true))
    }

    /**
     * Hide Pip overlay after all other actions are complete
     */
    fun hidePipOverlay() {
        tableScreen.removeOverlay(pipOverlay)
        tableScreen.actionManager.addAfterLastComplete(
                OverlayVisibilityAction(pipOverlay, false))
    }

    /**
     * Reveal a card played by a given user
     *
     * @param player - player whose card will be revealed
     * @param oldCardIndex - index of card to be played
     * from 0 to all cards in card space
     * @param newCard - new card to be played
     */
    fun revealAndHidePlayedCard(player: Int, oldCardIndex: Int, newCard: Int) {
        val action1 = tableScreen.revealCardInCardSpaceHand(player, oldCardIndex, getCardModelById(newCard))
        while (!action1.isComplete()) {
            Thread.sleep(150)
        }
        val action2 = tableScreen.revealCardInCardSpaceHand(player, oldCardIndex, getCardModelById(-1))
        while (!action2.isComplete()) {
            Thread.sleep(150)
        }
    }

    /**
     * Transfer card from player's cardspace
     * to the hand of another player
     * @param fromPlayer - id of player, whose cards to transfer
     * @param toPlayer - id of player, who will receive cards
     * @param index - index of played card in cardspace
     * @param newCard - a card that will be revealed instead
     * of card at index position
     */
    fun transferPlayedCardToPlayer(fromPlayer: Int,
                                   toPlayer: Int,
                                   index: Int,
                                   newCard: Int) {
        tableScreen.revealCardInCardSpaceHand(fromPlayer, index, getCardModelById(newCard))
        tableScreen.transferCardFromCardSpaceToPlayer(fromPlayer, toPlayer, index)

    }

    /**
     * Mark all Pips buttons as enabled
     */
    fun resetAllChoices() {
        for (choice in Choice.values()) {
            choiceOverlay.resetOption(choice)
        }
    }


    /**
     * Mark all DeckSizes buttons as enabled
     */
    fun resetAllSizes() {
        for (size in DeckSizes.values()) {
            deckSizeOverlay.resetOption(size)
        }
    }

    /**
     * Mark all bet count buttons as enabled
     */
    fun resetAllBetCounts() {
        for (betCount in BetCount.values()) {
            numberOverlay.resetOption(betCount)
        }
    }

    /**
     * Mark all pips buttons as enabled
     */
    fun resetAllPips() {
        for (pip in Pip.values()) {
            pipOverlay.resetOption(pip)
        }
    }

    /**
     * Update deck size inside of GUI
     */
    fun updateDeckSize(newDeckSize: Int) {
        deckSize = newDeckSize
        deckSizeChanged = true
    }


    override fun render() {
        if (deckSizeChanged) {
            initPipOverlay()
            tableScreen.updateDeckSize(deckSize)
            deckSizeChanged = false
        }
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
    val gameGUI = CheatGame(1, N = 5, deckSize = 52)
    LwjglApplication(gameGUI, config)
    Thread.sleep(2000)
    for (i in 0..51) {
        gameGUI.dealPlayer(i % 5, i)
    }
    val allowed = gameGUI.tableScreen.cards.map { x -> Card(x.suit, x.pip) }.toTypedArray()
    while (true) {
        gameGUI.pickCard(*allowed)
    }
}