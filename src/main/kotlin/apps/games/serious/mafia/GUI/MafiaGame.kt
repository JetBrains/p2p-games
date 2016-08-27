package apps.games.serious.mafia.GUI

import apps.games.serious.Card
import apps.games.serious.TableGUI.*
import apps.games.serious.getCardById
import apps.games.serious.getIdByCard
import apps.games.serious.mafia.roles.PlayerRole
import apps.games.serious.preferans.Bet
import apps.games.serious.preferans.GUI.ScoreOverlay
import apps.games.serious.preferans.PreferansScoreCounter
import apps.games.serious.preferans.Whists
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g3d.ModelBatch
import entity.Group
import entity.User

/**
 * Created by user on 6/30/16.
 */
class MafiaGame(val group: Group) : GameView() {
    lateinit var font: BitmapFont
    lateinit var tableScreen: TableScreen
    lateinit var userOverlay: ButtonOverlay<User>

    var loaded = false
    override fun create() {
        batch = ModelBatch()
        font = BitmapFont()
        font.data.scale(2f)
        tableScreen = TableScreen(this)
        initOverlays()
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
     * Init all overlays, that are used in this game
     */
    private fun initOverlays(){
        val users = group.users.toList().sortedBy { x -> x.name }
        val breaks  = users.slice(4..users.size-1 step 5)
        val names = users.associate { x -> x to x.name }
        userOverlay = ButtonOverlayFactory.create(users, breaks, names)
        userOverlay.create()
    }

    /**
     * Show User selection overlay after all other actions are complete
     */
    fun showUserPickOverlay() {
        tableScreen.addOverlay(userOverlay)
        tableScreen.actionManager.addAfterLastComplete(
                OverlayVisibilityAction(userOverlay, true))
    }

    /**
     * Hide User selection overlay after all other actions are complete
     */
    fun hideUserPickOverlay() {
        tableScreen.removeOverlay(userOverlay)
        tableScreen.actionManager.addAfterLastComplete(
                OverlayVisibilityAction(userOverlay, false))
    }

    /**
     * Add callback listener for buttons
     * corresponding to User Picks
     */
    fun <R> registerUserPickCallback(callback: (User) -> (R), users: Collection<User>) {
        for (user in users) {
            userOverlay.addCallback(user, callback)
        }
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
    val group = Group(mutableSetOf(User(Settings.hostAddress, "sfsefse"), User(Settings.hostAddress, "ASD")))
    val gameGUI = MafiaGame(group)
    LwjglApplication(gameGUI, config)
    Thread.sleep(2000)
    gameGUI.showUserPickOverlay()
    gameGUI.registerUserPickCallback({x -> gameGUI.hideUserPickOverlay()}, group.users)
    println("6 \u2660")
}