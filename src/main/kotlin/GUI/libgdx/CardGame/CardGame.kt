package GUI.libgdx.CardGame

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Game
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g3d.ModelBatch

/**
 * Created by user on 6/30/16.
 */
class CardGame: Game() {
    lateinit var batch: ModelBatch
    lateinit var font: BitmapFont

    override fun create() {
        batch = ModelBatch()
        font = BitmapFont()
        font.data.scale(2f)

        setScreen(CardGameScreen(this))
    }

    override fun render() {
        super.render()
    }

    override fun dispose() {
        batch.dispose()
        font.dispose()
    }
}