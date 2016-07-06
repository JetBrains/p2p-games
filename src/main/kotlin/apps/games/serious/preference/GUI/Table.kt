package apps.games.serious.preference.GUI

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable
import org.lwjgl.util.vector.Vector4f
import java.text.FieldPosition

/**
 * Created by user on 7/4/16.
 */


class Table(val playersCount: Int, val handSize: Int): Disposable{
    private val tableTopModel: Model
    val tableTop: ModelInstance
    val deckPosition = Vector3(0.5f, -0.5f, 0.1f)
    val MAX_PLAYERS = 8
    val playerSpots: Array<Player>
    val players: Array<Player>

    init{
        val angle = 360f / MAX_PLAYERS
        playerSpots = Array(MAX_PLAYERS, {i -> Player(Vector3(-1.2f, -8.3f, 0f), Vector3(0f, 1f, 0f), handSize, 0, this)})
        for(i in 0..MAX_PLAYERS-1){
            val initPos = Vector3(0f, -8.3f, 0f)
            val initVec = Vector3(0f, 1f, 0f)
            playerSpots[i] = Player(initPos.rotate(Vector3.Z, - angle*i), initVec.rotate(Vector3.Z, - angle*i), handSize, i, this)
        }
        players = Array(playersCount, {i -> playerSpots[i*(MAX_PLAYERS/playersCount)]})
    }

    init {
        val loader = ObjLoader()
        tableTopModel = loader.loadModel(Gdx.files.internal("models/poker+table_octagon_base.obj"))
        tableTop = ModelInstance(tableTopModel)
        tableTop.transform.scale(0.4f, 0.4f, 0.4f)
        tableTop.transform.rotate(1f, 0f, 0f, 90f)
        tableTop.transform.rotate(0f, 1f, 0f, 22.5f)
        tableTop.transform.translate(0f, -1.5f, 0f)
    }

    fun getMainPlayer(): Player{
        return players[0]
    }


    override fun dispose() {
        tableTopModel.dispose()
    }
}

class Player(val position: Vector3, val direction: Vector3, handSize: Int,  val Id: Int, val talbe: Table){
    val hand = Hand(position, handSize, this)

    /**
     * get angle between player and center of the table
     */
    fun getAngle(): Float{
        return MathUtils.radDeg*MathUtils.atan2(-direction.x, direction.y)
    }

    /**
     * get place, that selected card sould be put in
     */
    fun getCardspace(): Vector3{
        return Vector3(position).add(Vector3(direction).scl(3.5f))
    }

    override fun equals(other: Any?): Boolean{
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Player

        if (Id != other.Id) return false

        return true
    }

    override fun hashCode(): Int{
        return Id
    }


}

class Hand(val position: Vector3, val MAX_HAND_SIZE: Int = 6, val player: Player){
    val MAX_HAND_WIDTH = 3f
    var size: Int = 0

    fun nextCardPosition(): Vector3{
        val result = Vector3(position)
        //get perpendicular vector in Z=0 plane
        val normal = Vector3(player.direction).crs(0f, 0f, 1f).nor()
        val step = MAX_HAND_WIDTH / MAX_HAND_SIZE
        result.add(normal.scl(step*size - 1.4f))
        result.z += (size + 1)*0.01f
        return result
    }
}