package apps.games.serious.preferans.GUI

/**
 * Created by user on 7/13/16.
 */
interface CardSelector {
    fun canSelect(card: CardGUI): Boolean {return false}

    fun select(card: CardGUI) {}
}