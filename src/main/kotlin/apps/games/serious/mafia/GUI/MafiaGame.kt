package apps.games.serious.mafia.GUI

import apps.games.serious.Card
import apps.games.serious.TableGUI.*
import apps.games.serious.getCardById
import apps.games.serious.getIdByCard
import apps.games.serious.mafia.MafiaLogger
import apps.games.serious.mafia.roles.PlayerRole
import apps.games.serious.mafia.roles.Role
import apps.games.serious.preferans.Bet
import apps.games.serious.preferans.GUI.ScoreOverlay
import apps.games.serious.preferans.PreferansScoreCounter
import apps.games.serious.preferans.Whists
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.Gdx2DPixmap
import com.badlogic.gdx.graphics.g3d.ModelBatch
import entity.Group
import entity.User

/**
 * Created by user on 6/30/16.
 */
class MafiaGame(val group: Group, val logger: MafiaLogger, val maxTextLength: Int) : GameView() {
    lateinit var font: BitmapFont
    lateinit var tableScreen: TableScreen
    lateinit var userOverlay: ButtonOverlay<User>
    lateinit var logOverlay: LogOverlay
    lateinit var mafiaInputOverlay: MafiaInputOverlay
    lateinit var mafiaMessagesOverlay: MafiaMessagesOverlay
    private var role: String = ""
    var loaded = false
    override fun create() {
        batch = ModelBatch()
        font = BitmapFont()
        font.data.scale(2f)
        tableScreen = TableScreen(this, group.users.size)
        initOverlays()
        setScreen(tableScreen)

        val s = "Switch 2D/3D:            C\n" +
                "Move:                         WASD \n" +
                "Strafe:                        Q, E \n" +
                "Zoom camara:            midde mouse button\n" +
                "Toggle camera zoom:        SPACE\n" +
                "Show/hide logs:                  TAB\n" +
                "Show/hide mafia logs:        ESC\n"
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

        logOverlay = LogOverlay(logger)
        tableScreen.addOverlay(logOverlay)

        mafiaInputOverlay = MafiaInputOverlay(maxTextLength)
        mafiaMessagesOverlay = MafiaMessagesOverlay()
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
     * Show overlay with mafia input dialog
     */
    fun showMafiaInputOverlay(){
        mafiaInputOverlay.newMessage()
        tableScreen.addOverlay(mafiaInputOverlay)
        tableScreen.actionManager.addAfterLastComplete(
                OverlayVisibilityAction(mafiaInputOverlay, true))
    }

    /**
     * Show overlay to display mafia messages
     */
    fun showMafiaMessagesOverlay(){
        tableScreen.addOverlay(mafiaMessagesOverlay)
        tableScreen.actionManager.addAfterLastComplete(
                OverlayVisibilityAction(mafiaMessagesOverlay, true))
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
     * Hide mafia input dialog
     */
    fun hideMafiaInputOverlay() {
        tableScreen.removeOverlay(mafiaInputOverlay)
        tableScreen.actionManager.addAfterLastComplete(
                OverlayVisibilityAction(mafiaInputOverlay, false))
    }

    /**
     * register mafia messages to corresponding overlay
     */
    fun registerMafiaMessages(messages: Map<User, String>){
        mafiaMessagesOverlay.registerUserMessages(messages)
    }

    /**
     * Hide mafia messages overlay
     */
    fun hideMafiaMessagesOverlay() {
        tableScreen.removeOverlay(mafiaMessagesOverlay)
        tableScreen.actionManager.addAfterLastComplete(
                OverlayVisibilityAction(mafiaMessagesOverlay, false))
    }

    /**
     * register callback to be executed after last
     */
    fun <T> registerMafiaEOICallback(callback: (String) -> (T)){
        mafiaInputOverlay.registerCallback(callback)
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

    /**
     * Disable buttons for scpecified users
     * @param users - users, whom u can not pick
     */
    fun disableUserPicks(users: Collection<User>) {
        for (user in users){
            userOverlay.disableOption(user)
        }
    }

    /**
     * Mark all User Pick buttons as enabled
     */
    fun resetAllUserPicks() {
        for (user in group.users) {
            userOverlay.resetOption(user)
        }
    }

    /**
     * Set role to be shown in hint
     */
    fun setRole(role: Role){
        this.role = role.name
    }

    /**
     * Display hint for current step
     */
    fun showHint(hint: String) {
        synchronized(tableScreen.hint) {
            tableScreen.hint = "(You are [$role]) $hint"
        }
    }

    /**
     * Give a player with specified ID
     * a cardID taht corresponds to given role
     */
    fun dealPlayer(player: Int, role: Role, index: Int = 0) {
        tableScreen.dealPlayer(player, getCardModelByRole(role, index))
    }

    /**
     * In Mafia each role has it's own card
     * This function Role and transforms it into
     * renderable card object
     */
    private fun getCardModelByRole(role: Role, index: Int): CardGUI {
        val card = role.getCard(index)
        return tableScreen.deck.getCardModel(card.suit, card.pip)
    }

    /**
     * reveal player role
     *
     * @param player - whose card to reveal
     * @param role - role to reveal
     */
    fun revealPlayerRole(player: Int, role: Role, index: Int = 0) {
        val card = getCardModelByRole(role, index)
        tableScreen.revealPlayerCard(player, card)
    }

    /**
     * if role already revealed - animate play
     * of corresponding card (that means player death)
     *
     * @param role - role of killed player
     */
    fun animateRolePlay(role: Role, index: Int = 0){
        val card = getCardModelByRole(role, index)
        val action = tableScreen.animateCardPlay(card)
        if(action != null){
            while (!action.isComplete()){
                Thread.sleep(100)
            }
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
    val gameGUI = MafiaGame(group, MafiaLogger(), 140)
    LwjglApplication(gameGUI, config)
    Thread.sleep(2000)

    //MAFIA INPUT
//    gameGUI.registerMafiaEOICallback { s ->
//        println(s)
//        gameGUI.hideMafiaInputOverlay()
//        gameGUI.showMafiaInputOverlay()
//    }
//    gameGUI.showMafiaInputOverlay()

    //SHOW MESSAGES
    gameGUI.registerMafiaMessages(mutableMapOf(User(Settings.hostAddress, "alice") to "sslfgjsilef", User(Settings.hostAddress, "bob") to "skefhskfhesuef",
            User(Settings.hostAddress, "charlie") to "slkefgjsoiefjolsieujfpoaisejftoiaejfoiasgoairhjg la lairghla aloigh aliua rgladuik ghldaui rg", User(Settings.hostAddress, "Dave") to "skefhskfhesuef",
            User(Settings.hostAddress, "Eve") to "sslfgjsilef"))
    gameGUI.showMafiaMessagesOverlay()


}