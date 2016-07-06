package apps.games.serious.preference.GUI

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Game
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

    fun dealCard(player: Int, cardID: Int){
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

        tableScreen.dealPlayer(player, card)
    }

    override fun render() {
        super.render()
    }


    override fun dispose() {
        batch.dispose()
        font.dispose()
    }
}