package apps.games.serious.preference.GUI

import apps.games.serious.preference.Pip
import apps.games.serious.preference.Suit
import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Game
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g3d.ModelBatch

/**
 * Created by user on 6/30/16.
 */
class PreferenceGame : Game() {
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
    fun dealPlayer(player: Int, cardID: Int){
        tableScreen.dealPlayer(player, getCardById(cardID))
    }

    /**
     * Deal a common card. For example
     * TALON in Prefenernce or cards
     * in Texas Holdem Poker
     */
    fun dealCommon(cardID: Int){
        tableScreen.dealCommon(getCardById(cardID))
    }

    /**
     * Show bidding overlay after all other actions are complete
     */
    fun showBiddingOverlay(){
        tableScreen.actionManager.addAfterLastComplete(BiddingOverlayAction(tableScreen.biddingOverlay, true))
    }

    fun hideBiddingOverlay(){
        tableScreen.actionManager.addAfterLastComplete(BiddingOverlayAction(tableScreen.biddingOverlay, false))
    }

    private fun getCardById(cardID: Int): Card{
        val card: Card
        if(cardID == -1){
            card = tableScreen.deck.getCard(Suit.UNKNOWN, Pip.UNKNOWN)
        }else{
            val suitId = cardID / 8
            var pipId: Int = (cardID % 8)
            if(Pip.TWO.index <= pipId){
                pipId += 5
            }
            println(pipId)
            card = tableScreen.deck.getCard(Suit.values().first { x -> x.index == suitId },
                    Pip.values().first{x -> x.index == pipId})
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
    val gameGUI = PreferenceGame()
    LwjglApplication(gameGUI, config)
    println("6 \u2660")
}