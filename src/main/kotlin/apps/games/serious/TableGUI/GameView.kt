package apps.games.serious.TableGUI

import com.badlogic.gdx.Game
import com.badlogic.gdx.graphics.g3d.ModelBatch

/**
 * Created by user on 8/1/16.
 */
abstract class GameView : Game() {
    lateinit var batch: ModelBatch
}