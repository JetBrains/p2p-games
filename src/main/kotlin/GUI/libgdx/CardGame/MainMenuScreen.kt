package GUI.libgdx.CardGame

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera

/**
 * Created by user on 7/1/16.
 */

class MainMenuScreen(val game: CardGame): Screen {
    val camera = OrthographicCamera()
    init{
        camera.setToOrtho(false, 800f, 480f)
    }


    override fun render(delta: Float) {


    }

    override fun resume() {}

    override fun hide() {}

    override fun resize(width: Int, height: Int) {}

    override fun pause() {}

    override fun show() {}

    override fun dispose() {}

}